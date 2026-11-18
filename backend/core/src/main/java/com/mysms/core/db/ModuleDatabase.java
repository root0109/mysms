package com.mysms.core.db;

import com.mysms.core.context.CallContextHolder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Everything one module needs to reach its own schema, and nothing that could reach another's: pools, transaction
 * manager, {@link JdbcClient}, tenant binding and Flyway history, from `mysms.<module>.datasource.*`.
 */
public final class ModuleDatabase implements AutoCloseable {

    private static final Pattern MODULE = Pattern.compile("^[a-z]+$");
    private static final Pattern ROLE = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");
    private static final String BIND_TENANT = "SELECT set_config('app.tenant_id', :tenant, true)";
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDatabase.class);
    /** How long a read waits for a replica connection before the primary serves it (RoutingDataSource). */
    private static final long REPLICA_CONNECTION_TIMEOUT_MS = 2_000;

    private final String module;
    private final HikariDataSource primaryPool;
    private final HikariDataSource replicaPool;
    private final RoutingDataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final JdbcClient jdbc;
    private final JdbcClient tenantJdbc;
    private final boolean tenantScoped;
    private final TransactionDefinition writeDefinition;
    private final TransactionDefinition readDefinition;
    private final ModuleDatabaseSettings settings;
    private HikariDataSource relayPool;
    private final Object lock = new Object();

    private ModuleDatabase(String module, HikariDataSource primaryPool, HikariDataSource replicaPool,
                           boolean tenantScoped, ModuleDatabaseSettings settings) {
        this.module = module;
        this.settings = settings;
        this.primaryPool = primaryPool;
        this.replicaPool = replicaPool;
        this.dataSource = new RoutingDataSource(primaryPool, replicaPool);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        // statements slower than the module's limit are named in the log (their text, never their values)
        this.jdbc = !settings.slowStatement().isNegative() && !settings.slowStatement().isZero()
                ? JdbcClient.create(new SlowStatementJdbcTemplate(dataSource, module, settings.slowStatement()))
                : JdbcClient.create(dataSource);
        this.tenantJdbc = TenantJdbcClient.wrap(jdbc);
        this.tenantScoped = tenantScoped;
        this.writeDefinition = definition(false, settings.transactionTimeout());
        this.readDefinition = definition(true, settings.transactionTimeout());
    }

    private static TransactionDefinition definition(boolean readOnly, Duration timeout) {
        DefaultTransactionDefinition definition =
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setReadOnly(readOnly);
        definition.setTimeout(Math.toIntExact(timeout.toSeconds()));
        return definition;
    }

    /** Binds `mysms.<module>.datasource.*` and opens the module's database (migrating it unless disabled). */
    public static ModuleDatabase open(String module, Environment environment, boolean tenantScoped) {
        ModuleDatabaseSettings settings = Binder.get(environment)
                .bind("mysms." + module + ".datasource", Bindable.of(ModuleDatabaseSettings.class))
                .orElseThrow(() -> new IllegalStateException("mysms." + module + ".datasource is not configured"));
        return open(module, settings, tenantScoped);
    }

    public static ModuleDatabase open(String module, ModuleDatabaseSettings settings, boolean tenantScoped) {
        if (!MODULE.matcher(module).matches()) {
            throw new IllegalArgumentException("invalid module name " + module);
        }
        HikariDataSource primary = pool(module, DatabaseRole.PRIMARY, settings.url(), settings.username(),
                settings.password(), settings.maximumPoolSize());
        HikariDataSource replica = null;
        try {
            if (settings.migrate()) {
                migrate(module, primary, settings);
            }
            if (settings.hasReplica()) {
                replica = pool(module, DatabaseRole.REPLICA, settings.replicaUrl(), settings.replicaUsername(),
                        settings.replicaPassword(), settings.maximumPoolSize());
            }
            return new ModuleDatabase(module, primary, replica, tenantScoped, settings);
        } catch (RuntimeException e) {
            if (replica != null) {
                replica.close();
            }
            primary.close();
            throw e;
        }
    }

    /**
     * One connection pool, named {@code <module>} or {@code <module>-replica}; the name is also the sessions'
     * {@code application_name}, so each shows in {@code pg_stat_activity} as what it is.
     */
    private static HikariDataSource pool(String module, DatabaseRole role, String url, String username,
                                         String password, int size) {
        String name = role == DatabaseRole.PRIMARY ? module : module + "-replica";
        HikariConfig config = new HikariConfig();
        config.setPoolName(name);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(size);
        config.addDataSourceProperty("ApplicationName", name);
        if (role == DatabaseRole.REPLICA) {
            // a replica refuses writes itself; marking its connections read-only says so to the driver as well
            config.setReadOnly(true);
            // the module starts without its replica, and a read waits briefly before the primary serves it
            config.setInitializationFailTimeout(-1);
            config.setConnectionTimeout(REPLICA_CONNECTION_TIMEOUT_MS);
        }
        return new HikariDataSource(config);
    }

    /**
     * Applies the module's own migrations (classpath:db/migration/&lt;module&gt;) to its own schema, with its own
     * history table in that schema. Never cleans. With a separate migration role, Flyway connects as the schema owner
     * and then grants the application role exactly USAGE on the schema, DML on its tables and use of its sequences.
     */
    static MigrateResult migrate(String module, DataSource applicationDataSource, ModuleDatabaseSettings settings) {
        FluentConfiguration flyway = Flyway.configure()
                .schemas(module)
                .defaultSchema(module)
                .createSchemas(true)
                .locations("classpath:db/migration/" + module)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .failOnMissingLocations(true);
        if (settings.separateMigrationRole()) {
            if (!ROLE.matcher(settings.username()).matches()) {
                throw new IllegalArgumentException("invalid application role name for module " + module);
            }
            flyway.dataSource(settings.url(), settings.migrateUsername(), settings.migratePassword())
                    .callbacks(new ApplicationRoleGrants(module, settings.username(), relayRole(module, settings)));
        } else {
            LOG.warn("module {} migrates as its application role: development only, use a separate migrate-username",
                    module);
            flyway.dataSource(applicationDataSource);
        }
        return flyway.load().migrate();
    }

    private static String relayRole(String module, ModuleDatabaseSettings settings) {
        if (settings.relayUsername() == null) {
            return null;
        }
        if (!ROLE.matcher(settings.relayUsername()).matches()) {
            throw new IllegalArgumentException("invalid relay role name for module " + module);
        }
        if (settings.relayUsername().equals(settings.username())
                || settings.relayUsername().equals(settings.migrateUsername())) {
            throw new IllegalArgumentException("module " + module + ": the relay role must be a role of its own "
                    + "(mysms_relay), not the application or migration role");
        }
        return settings.relayUsername();
    }

    /**
     * Connections for the outbox relay, which reads every tenant's pending events: as the relay role (BYPASSRLS) when
     * one is configured, otherwise empty. A small pool of its own, never used for business queries.
     */
    public Optional<DataSource> relayDataSource() {
        synchronized (lock) {
            if (settings.relayUsername() == null) {
                return Optional.empty();
            }
            if (relayPool == null) {
                HikariConfig config = new HikariConfig();
                config.setPoolName(module + "-relay");
                config.setJdbcUrl(settings.url());
                config.setUsername(settings.relayUsername());
                config.setPassword(settings.relayPassword());
                config.setMaximumPoolSize(2);
                relayPool = new HikariDataSource(config);
            }
            return Optional.of(relayPool);
        }
    }

    public String module() {
        return module;
    }

    /**
     * The module's connections: the primary's, and for a read-only transaction opened through
     * {@link AppTransactionManager} the replica's when the module has one.
     */
    public DataSource dataSource() {
        return dataSource;
    }

    /** Whether read-only transactions of this module read from a replica. */
    public boolean hasReplica() {
        return dataSource.hasReplica();
    }

    public PlatformTransactionManager transactionManager() {
        return transactionManager;
    }

    /** The module's client; it binds the call's tenant to every statement that filters on {@code :tenantId}. */
    public JdbcClient jdbc() {
        return tenantJdbc;
    }

    /**
     * Connections as the schema owner (the migrate role), for the DDL the application role may never run: partition
     * maintenance. Without a separate migrate role (local development) this is the application pool.
     */
    public DataSource ownerDataSource() {
        if (!settings.separateMigrationRole()) {
            return primaryPool;
        }
        return new DriverManagerDataSource(settings.url(), settings.migrateUsername(), settings.migratePassword());
    }

    /** A read-write transaction of this module: READ COMMITTED, bounded by the module's transaction timeout. */
    public TransactionDefinition writeDefinition() {
        return writeDefinition;
    }

    /** A read-only transaction of this module: the database rejects writes inside it. */
    public TransactionDefinition readDefinition() {
        return readDefinition;
    }

    /**
     * Binds the call's tenant to the transaction just opened (the transaction-local {@code app.tenant_id} row-level
     * security reads), so every statement sees that tenant's rows only. Never called for global tables.
     *
     * @throws IllegalStateException when no tenant is in the call context (a programming error: the edge sets it)
     */
    public void bindTenant() {
        if (!tenantScoped) {
            return;
        }
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow(
                () -> new IllegalStateException("tenant-scoped transaction without a tenant in the call context"));
        jdbc.sql(BIND_TENANT).param("tenant", tenant.toString()).query().singleValue();
    }

    /** True when the copy of the database answers; used by the module's health indicator. */
    public boolean isReachable(DatabaseRole role) {
        try {
            Integer one = JdbcClient.create(dataSource.target(role)).sql("SELECT 1").query(Integer.class).single();
            return one == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (relayPool != null) {
                relayPool.close();
            }
            if (replicaPool != null) {
                replicaPool.close();
            }
            primaryPool.close();
        }
    }
}
