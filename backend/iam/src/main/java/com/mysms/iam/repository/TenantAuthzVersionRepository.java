package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * iam.tenant_authz_version: the one place the tenant's authorisation version is raised and read, so every write path
 * that changes roles, role permissions, role grants or direct grants raises it the same way.
 */
@Repository
public class TenantAuthzVersionRepository {

    private static final String BUMP = """
            INSERT INTO iam.tenant_authz_version (tenant_id, authz_version, updated_at)
            VALUES (:tenantId, 1, now())
            ON CONFLICT (tenant_id)
            DO UPDATE SET authz_version = iam.tenant_authz_version.authz_version + 1, updated_at = now()
            """;
    private static final String CURRENT = """
            SELECT v.authz_version
              FROM iam.tenant_authz_version v
             WHERE v.tenant_id = :tenantId
            """;

    private final JdbcClient jdbc;

    public TenantAuthzVersionRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Raises the call's tenant by one; called once per unit of work that changed its authorisation data. */
    public void bump() {
        jdbc.sql(BUMP).update();
    }

    /** The call's tenant version; 0 while no change has raised it yet. */
    public long current() {
        return jdbc.sql(CURRENT).query(Long.class).optional().orElse(0L);
    }
}
