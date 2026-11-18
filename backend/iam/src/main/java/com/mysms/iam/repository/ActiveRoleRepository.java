package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * A user's counted role grants with their role ids (the persona switcher offers them, iam.SwitchRole picks one) and
 * the permissions of one role alone. A grant counts as in {@link GrantRepository}: active, inside its dates, its role
 * live and active. Working in one role, a user has that role's permissions and the self-service ones every role
 * has, minus direct denies; direct grants and the other roles do not apply.
 */
@Repository
public class ActiveRoleRepository {

    /** Starts with a space: text blocks drop trailing blanks, so the statements that append this end without one. */
    private static final String COUNTED = " " + """
            ur.user_id = :user AND ur.deleted_at IS NULL AND ur.is_active
            AND (ur.valid_from IS NULL OR ur.valid_from <= :today) AND (ur.valid_to IS NULL OR ur.valid_to >= :today)
            AND rl.deleted_at IS NULL AND rl.is_active
            """;
    private static final String GRANTS = """
            SELECT rl.id, rl.code, ur.scope_type, ur.scope_id
              FROM iam.user_role ur
              JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
             WHERE ur.tenant_id = :tenantId
               AND""" + COUNTED + """
             ORDER BY rl.code, ur.scope_type, ur.scope_id
            """;
    private static final String NOT_DENIED = """
            AND (NOT pm.is_modifiable OR NOT EXISTS (
                    SELECT 1 FROM iam.user_permission dn
                     WHERE dn.tenant_id = :tenantId
                       AND dn.permission_id = pm.id AND dn.user_id = :user AND dn.deleted_at IS NULL
                       AND dn.effect = 'deny'))
            """;
    private static final String ROLE_PERMISSIONS = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL
               AND (pm.applies_to_all_roles
                    OR EXISTS (SELECT 1 FROM iam.role_permission rp
                                WHERE rp.tenant_id = :tenantId
                                  AND rp.role_id = :role AND rp.permission_id = pm.id AND rp.deleted_at IS NULL))
            """ + NOT_DENIED + """
             ORDER BY pm.code
            """;

    private final JdbcClient jdbc;

    public ActiveRoleRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Every counted grant of the user, with its role id. */
    public List<Grant> grants(UUID user, LocalDate today) {
        return jdbc.sql(GRANTS).param("user", user).param("today", today)
                .query((rs, row) -> new Grant(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("scope_type"), Optional.ofNullable(rs.getObject("scope_id", UUID.class))))
                .list();
    }

    /** The role's own permissions plus every role's self-service ones, minus the user's direct denies. */
    public List<String> rolePermissions(UUID user, UUID role) {
        return jdbc.sql(ROLE_PERMISSIONS).param("user", user).param("role", role).query(String.class).list();
    }

    /**
     * @param roleId    the role
     * @param roleCode  its code
     * @param scopeType tenant, institution, department, class_section, course, lms_course or lms_cohort
     * @param scopeId   the unit; empty for a tenant-wide grant
     */
    public record Grant(UUID roleId, String roleCode, String scopeType, Optional<UUID> scopeId) {

        public Grant {
            scopeId = scopeId == null ? Optional.empty() : scopeId;
        }

        /** The same grant as the sign-in claims list it. */
        public RoleGrantRow row() {
            return new RoleGrantRow(roleCode, scopeType, scopeId, false);
        }
    }
}
