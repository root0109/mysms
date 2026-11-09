package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * iam.tenant_directory: tenant codes and status, so sign-in can resolve the tenant a user types without reading the
 * platform schema. Global (no row-level security); read before a tenant is bound.
 */
@Repository
public class TenantDirectoryRepository {

    private static final String FIND_ACTIVE = """
            SELECT tenant_id FROM iam.tenant_directory WHERE lower(tenant_code) = lower(:code) AND status = 'active'
            """;
    private static final String FIND_BY_CODE = """
            SELECT tenant_id FROM iam.tenant_directory WHERE lower(tenant_code) = lower(:code)
            """;
    private static final String IS_ACTIVE = """
            SELECT EXISTS (SELECT 1 FROM iam.tenant_directory WHERE tenant_id = :tenant AND status = 'active')
            """;
    private static final String INSERT = """
            INSERT INTO iam.tenant_directory (tenant_id, tenant_code, status) VALUES (:tenant, :code, 'active')
            """;

    /**
     * The directory follows platform.tenant from its events: an entry changes only to a newer tenant row version, so a
     * late or repeated event never restores an older code or status.
     */
    private static final String APPLY = """
            INSERT INTO iam.tenant_directory (tenant_id, tenant_code, status, source_version, synced_at)
            VALUES (:tenant, :code, :status, :version, :now)
            ON CONFLICT (tenant_id) DO UPDATE
               SET tenant_code = EXCLUDED.tenant_code, status = EXCLUDED.status,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE iam.tenant_directory.source_version < EXCLUDED.source_version
            """;
    private static final String FIND_STATE = """
            SELECT tenant_code, status, source_version FROM iam.tenant_directory WHERE tenant_id = :tenant
            """;

    /** One directory entry as stored. */
    public record Entry(String code, String status, int sourceVersion) {
    }

    private final JdbcClient jdbc;

    public TenantDirectoryRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The tenant of an active code (codes compare case-insensitively). */
    public Optional<UUID> findActiveTenant(String code) {
        return jdbc.sql(FIND_ACTIVE).param("code", code).query(UUID.class).optional();
    }

    /** The tenant of a code whatever its status. */
    public Optional<UUID> findTenant(String code) {
        return jdbc.sql(FIND_BY_CODE).param("code", code).query(UUID.class).optional();
    }

    public boolean isActive(UUID tenant) {
        return Boolean.TRUE.equals(jdbc.sql(IS_ACTIVE).param("tenant", tenant).query(Boolean.class).single());
    }

    public void insert(UUID tenant, String code) {
        jdbc.sql(INSERT).param("tenant", tenant).param("code", code).update();
    }

    /** Applies one tenant row version; returns whether the entry changed (false for an older or repeated version). */
    public boolean apply(UUID tenant, String code, String status, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("tenant", tenant).param("code", code).param("status", status)
                .param("version", version).param("now", now).update() > 0;
    }

    public Optional<Entry> find(UUID tenant) {
        return jdbc.sql(FIND_STATE).param("tenant", tenant)
                .query((rs, row) -> new Entry(rs.getString("tenant_code"), rs.getString("status"),
                        rs.getInt("source_version")))
                .optional();
    }
}
