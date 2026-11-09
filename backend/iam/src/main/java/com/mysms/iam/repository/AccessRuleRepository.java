package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The lookups behind the access-management rules (business/*Rules): which permission a row grants, which permissions a
 * role carries and whether a role is a system role. Read inside the caller's tenant transaction.
 */
@Repository
public class AccessRuleRepository {

    private static final String PERMISSION_CODE = """
            SELECT code FROM iam.permission WHERE id = :id AND deleted_at IS NULL
            """;
    private static final String ROLE_PERMISSION_CODES = """
            SELECT pm.code
              FROM iam.role_permission rp
              JOIN iam.permission pm ON pm.id = rp.permission_id AND pm.deleted_at IS NULL
             WHERE rp.tenant_id = :tenantId
               AND rp.role_id = :role AND rp.deleted_at IS NULL
             ORDER BY pm.code
            """;
    private static final String ROLE_IS_SYSTEM = """
            SELECT is_system FROM iam.role WHERE tenant_id = :tenantId AND id = :role AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public AccessRuleRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The code of a live catalogue permission. */
    public Optional<String> permissionCode(UUID permissionId) {
        return jdbc.sql(PERMISSION_CODE).param("id", permissionId).query(String.class).optional();
    }

    /** Every permission a role currently carries. */
    public List<String> rolePermissionCodes(UUID roleId) {
        return jdbc.sql(ROLE_PERMISSION_CODES).param("role", roleId).query(String.class).list();
    }

    /** Whether the live role is a system role (empty when there is no such role). */
    public Optional<Boolean> roleIsSystem(UUID roleId) {
        return jdbc.sql(ROLE_IS_SYSTEM).param("role", roleId).query(Boolean.class).optional();
    }
}
