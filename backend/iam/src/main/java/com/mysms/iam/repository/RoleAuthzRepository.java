package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The permissions of whole roles, read in one statement for a batch of roles: what every service's permissions filter
 * caches. A role that is deleted, inactive or another tenant's yields no row; a live one yields its
 * iam.role_permission codes and the codes every role holds implicitly (iam.permission.applies_to_all_roles).
 */
@Repository
public class RoleAuthzRepository {

    private static final String CODES_OF_ROLES = """
            SELECT rl.id AS role_id, pm.code
              FROM iam.role rl
              JOIN iam.permission pm ON pm.deleted_at IS NULL
               AND EXISTS (SELECT 1
                             FROM iam.role_permission rp
                            WHERE rp.tenant_id = :tenantId
                              AND rp.role_id = rl.id AND rp.permission_id = pm.id AND rp.deleted_at IS NULL)
             WHERE rl.tenant_id = :tenantId
               AND rl.deleted_at IS NULL
               AND rl.is_active
               AND rl.id = ANY(:roleIds)
             ORDER BY rl.id, pm.code
            """;
    /** The codes every role holds implicitly (self-service): one list for the whole platform. */
    private static final String COMMON_CODES = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL AND pm.applies_to_all_roles
             ORDER BY pm.code
            """;

    private final JdbcClient jdbc;

    public RoleAuthzRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public List<String> commonCodes() {
        return jdbc.sql(COMMON_CODES).query(String.class).list();
    }

    /** One row per (role, own permission code) of the call's tenant, ordered by role and then code. */
    public List<RolePermissionCodeRow> codesOfRoles(UUID[] roleIds) {
        return jdbc.sql(CODES_OF_ROLES).param("roleIds", roleIds)
                .query((rs, row) -> new RolePermissionCodeRow(rs.getObject("role_id", UUID.class),
                        rs.getString("code")))
                .list();
    }
}
