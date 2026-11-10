package com.mysms.core.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * A module's data source that hands out connections of the primary or of the replica, by the role the opening
 * transaction asked for ({@link DatabaseRoleHolder}); the {@code RoutingDataSource} of the owner's {@code common}
 * module, with its targets fixed when the module starts. A module without a replica routes both roles to the
 * primary, so code never needs to know whether a replica exists.
 *
 * <p>The replica is an optimisation, never a dependency: when it cannot give a connection (down, restarting, its pool
 * exhausted past its short timeout) the read is served by the primary instead, so a replica outage costs load on the
 * primary, not availability.
 */
public final class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingDataSource.class);

    private final DataSource primary;
    private final DataSource replica;

    public RoutingDataSource(DataSource primary, DataSource replica) {
        this.primary = primary;
        this.replica = replica == null ? primary : replica;
        Map<Object, Object> targets = new HashMap<>();
        targets.put(DatabaseRole.PRIMARY, primary);
        targets.put(DatabaseRole.REPLICA, this.replica);
        setTargetDataSources(targets);
        setDefaultTargetDataSource(primary);
        // an unknown role is a programming error, never a reason to fall back silently
        setLenientFallback(false);
        afterPropertiesSet();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!hasReplica() || determineCurrentLookupKey() != DatabaseRole.REPLICA) {
            return super.getConnection();
        }
        try {
            return replica.getConnection();
        } catch (SQLException unavailable) {
            LOG.warn("the replica gave no connection ({}); the read runs on the primary", unavailable.getSQLState());
            return primary.getConnection();
        }
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return DatabaseRoleHolder.current().orElse(DatabaseRole.PRIMARY);
    }

    /** The connection pool of a role. */
    public DataSource target(DatabaseRole role) {
        return role == DatabaseRole.REPLICA ? replica : primary;
    }

    /** Whether reads have a replica of their own. */
    public boolean hasReplica() {
        return replica != primary;
    }
}
