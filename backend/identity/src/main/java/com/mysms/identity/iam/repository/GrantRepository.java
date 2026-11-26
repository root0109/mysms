package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * A user's role grants and effective permissions (iam.user_role, iam.role, iam.role_permission, iam.user_permission,
 * iam.permission). A grant counts while it is active, inside its validity dates and its role is active. Effective
 * permissions = permissions of every counted role + direct grants + permissions that apply to every role, minus
 * direct denies (a deny never removes a non-modifiable self-service permission).
 */
@Repository
public class GrantRepository {

    /** Starts with a space: text blocks drop trailing blanks, so the statements that append this end without one. */
    private static final String COUNTED_GRANT = " " + """
            ur.user_id = :user AND ur.deleted_at IS NULL AND ur.is_active
            AND (ur.valid_from IS NULL OR ur.valid_from <= :today) AND (ur.valid_to IS NULL OR ur.valid_to >= :today)
            AND rl.deleted_at IS NULL AND rl.is_active
            """;
    private static final String ACTIVE_GRANTS = """
            SELECT rl.code, ur.scope_type, ur.scope_id, ur.is_primary
              FROM iam.user_role ur
              JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
             WHERE ur.tenant_id = :tenantId
               AND""" + COUNTED_GRANT + """
             ORDER BY rl.code, ur.scope_type, ur.scope_id
            """;
    private static final String NOT_DENIED = """
            AND (NOT pm.is_modifiable OR NOT EXISTS (
                    SELECT 1 FROM iam.user_permission dn
                     WHERE dn.tenant_id = :tenantId
                       AND dn.permission_id = pm.id AND dn.user_id = :user AND dn.deleted_at IS NULL
                       AND dn.effect = 'deny'))
            """;
    /** Permissions carried by tenant-scope role grants or granted directly (no unit): they reach every institution. */
    private static final String TENANT_WIDE = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL
               AND (EXISTS (SELECT 1
                              FROM iam.role_permission rp
                              JOIN iam.user_role ur ON ur.tenant_id = :tenantId AND ur.role_id = rp.role_id
                              JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
                             WHERE rp.tenant_id = :tenantId
                               AND rp.permission_id = pm.id AND rp.deleted_at IS NULL AND ur.scope_type = 'tenant'
                               AND""" + COUNTED_GRANT + """
                           )
                    OR EXISTS (SELECT 1 FROM iam.user_permission ug
                                WHERE ug.tenant_id = :tenantId
                                  AND ug.permission_id = pm.id AND ug.user_id = :user AND ug.deleted_at IS NULL
                                  AND ug.effect = 'grant'))
            """ + NOT_DENIED + """
             ORDER BY pm.code
            """;
    private static final String EFFECTIVE = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL
               AND (pm.applies_to_all_roles
                    OR EXISTS (SELECT 1
                                 FROM iam.role_permission rp
                                 JOIN iam.user_role ur ON ur.tenant_id = :tenantId AND ur.role_id = rp.role_id
                                 JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
                                WHERE rp.tenant_id = :tenantId
                                  AND rp.permission_id = pm.id AND rp.deleted_at IS NULL AND""" + COUNTED_GRANT + """
                              )
                    OR EXISTS (SELECT 1 FROM iam.user_permission ug
                                WHERE ug.tenant_id = :tenantId
                                  AND ug.permission_id = pm.id AND ug.user_id = :user AND ug.deleted_at IS NULL
                                  AND ug.effect = 'grant'))
            """ + NOT_DENIED + """
             ORDER BY pm.code
            """;
    /**
     * Whether the user holds a permission for one scope: through a direct grant or a tenant-wide role grant (both
     * cover every scope), or a role granted on exactly that unit. Units are not expanded across modules (a grant on an
     * institution does not cover its departments, which another schema owns).
     */
    private static final String PERMITTED_AT = """
            SELECT EXISTS (
                SELECT 1 FROM iam.permission pm
                 WHERE pm.code = :code AND pm.deleted_at IS NULL
                   AND (EXISTS (SELECT 1 FROM iam.user_permission ug
                                 WHERE ug.tenant_id = :tenantId
                                   AND ug.permission_id = pm.id AND ug.user_id = :user AND ug.deleted_at IS NULL
                                   AND ug.effect = 'grant')
                        OR EXISTS (SELECT 1
                                     FROM iam.role_permission rp
                                     JOIN iam.user_role ur ON ur.tenant_id = :tenantId AND ur.role_id = rp.role_id
                                     JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
                                    WHERE rp.tenant_id = :tenantId
                                      AND rp.permission_id = pm.id AND rp.deleted_at IS NULL AND""" + COUNTED_GRANT + """
                                      AND (ur.scope_type = 'tenant'
                                           OR (ur.scope_type = :scopeType AND ur.scope_id = CAST(:scopeId AS uuid)))))
            """ + NOT_DENIED + ")";
    /** The units a role grant gives the user this permission on (tenant-wide grants are reported by permittedAt). */
    private static final String GRANT_SCOPES = """
            SELECT DISTINCT ur.scope_type, ur.scope_id
              FROM iam.permission pm
              JOIN iam.role_permission rp ON rp.tenant_id = :tenantId AND rp.permission_id = pm.id AND rp.deleted_at IS NULL
              JOIN iam.user_role ur ON ur.tenant_id = :tenantId AND ur.role_id = rp.role_id
              JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
             WHERE pm.code = :code AND pm.deleted_at IS NULL AND ur.scope_type <> 'tenant' AND""" + COUNTED_GRANT
            + NOT_DENIED + """
             ORDER BY ur.scope_type, ur.scope_id
            """;
    /**
     * Every permission the user holds now or will hold through a grant that has not ended: active and future-dated
     * role grants and direct grants (an administrator taking over an account must outrank its future access too).
     */
    private static final String HELD_OR_SCHEDULED = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL
               AND (pm.applies_to_all_roles
                    OR EXISTS (SELECT 1
                                 FROM iam.role_permission rp
                                 JOIN iam.user_role ur ON ur.tenant_id = :tenantId AND ur.role_id = rp.role_id
                                 JOIN iam.role rl ON rl.tenant_id = :tenantId AND rl.id = ur.role_id
                                WHERE rp.tenant_id = :tenantId
                                  AND rp.permission_id = pm.id AND rp.deleted_at IS NULL AND ur.user_id = :user
                                  AND ur.deleted_at IS NULL AND ur.is_active
                                  AND (ur.valid_to IS NULL OR ur.valid_to >= :today)
                                  AND rl.deleted_at IS NULL AND rl.is_active)
                    OR EXISTS (SELECT 1 FROM iam.user_permission ug
                                WHERE ug.tenant_id = :tenantId
                                  AND ug.permission_id = pm.id AND ug.user_id = :user AND ug.deleted_at IS NULL
                                  AND ug.effect = 'grant'))
             ORDER BY pm.code
            """;
    private static final String CATALOGUE = "SELECT code FROM iam.permission WHERE deleted_at IS NULL ORDER BY code";
    /** The codes granted to the user directly (the token's {@code dg}): they reach every scope. */
    private static final String DIRECT_GRANTS = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL
               AND EXISTS (SELECT 1 FROM iam.user_permission ug
                            WHERE ug.tenant_id = :tenantId
                              AND ug.permission_id = pm.id AND ug.user_id = :user AND ug.deleted_at IS NULL
                              AND ug.effect = 'grant')
             ORDER BY pm.code
            """;
    /**
     * The codes denied to the user directly (the token's {@code dd}); a deny never removes a non-modifiable
     * (self-service) code, so those are left out.
     */
    private static final String DENIES = """
            SELECT pm.code
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL AND pm.is_modifiable
               AND EXISTS (SELECT 1 FROM iam.user_permission dn
                            WHERE dn.tenant_id = :tenantId
                              AND dn.permission_id = pm.id AND dn.user_id = :user AND dn.deleted_at IS NULL
                              AND dn.effect = 'deny')
             ORDER BY pm.code
            """;

    private final JdbcClient jdbc;

    public GrantRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public List<RoleGrantRow> activeGrants(UUID user, LocalDate today) {
        return jdbc.sql(ACTIVE_GRANTS).param("user", user).param("today", today)
                .query((rs, row) -> new RoleGrantRow(rs.getString("code"), rs.getString("scope_type"),
                        Optional.ofNullable(rs.getObject("scope_id", UUID.class)), rs.getBoolean("is_primary")))
                .list();
    }

    public List<String> effectivePermissions(UUID user, LocalDate today) {
        return jdbc.sql(EFFECTIVE).param("user", user).param("today", today).query(String.class).list();
    }

    public List<String> heldOrScheduledPermissions(UUID user, LocalDate today) {
        return jdbc.sql(HELD_OR_SCHEDULED).param("user", user).param("today", today).query(String.class).list();
    }

    public List<String> tenantWidePermissions(UUID user, LocalDate today) {
        return jdbc.sql(TENANT_WIDE).param("user", user).param("today", today).query(String.class).list();
    }

    /** {@code scopeId} is null for the tenant scope. */
    public boolean permittedAt(UUID user, String code, String scopeType, UUID scopeId, LocalDate today) {
        return Boolean.TRUE.equals(jdbc.sql(PERMITTED_AT).param("user", user).param("code", code)
                .param("scopeType", scopeType).param("scopeId", scopeId == null ? null : scopeId.toString())
                .param("today", today).query(Boolean.class).single());
    }

    /** The units (other than the whole tenant) on which a role grant gives the user the permission. */
    public List<RoleGrantRow> grantScopes(UUID user, String code, LocalDate today) {
        return jdbc.sql(GRANT_SCOPES).param("user", user).param("code", code).param("today", today)
                .query((rs, row) -> new RoleGrantRow(code, rs.getString("scope_type"),
                        Optional.ofNullable(rs.getObject("scope_id", UUID.class)), false))
                .list();
    }

    public List<String> directGrants(UUID user) {
        return jdbc.sql(DIRECT_GRANTS).param("user", user).query(String.class).list();
    }

    public List<String> denies(UUID user) {
        return jdbc.sql(DENIES).param("user", user).query(String.class).list();
    }

    /** Every permission code that exists (global catalogue). */
    public List<String> catalogue() {
        return jdbc.sql(CATALOGUE).query(String.class).list();
    }
}
