package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.core.id.UuidV7;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The few role writes the installation bootstrap needs (iam.role, iam.role_permission, iam.user_role). */
@Repository
public class RoleSeedRepository {

    private static final String FIND_BY_CODE = """
            SELECT id FROM iam.role WHERE tenant_id = :tenantId AND code = :code AND deleted_at IS NULL
            """;
    private static final String INSERT_ROLE = """
            INSERT INTO iam.role (id, tenant_id, code, display_name, description, is_system, is_active, created_at,
                                  created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :code, :name, :description, true, true, :now, :actor, :now, :actor, 1)
            """;
    /** Permissions of the catalogue the role does not have yet. */
    private static final String MISSING_PERMISSIONS = """
            SELECT pm.id
              FROM iam.permission pm
             WHERE pm.deleted_at IS NULL
               AND NOT EXISTS (SELECT 1 FROM iam.role_permission rp
                                WHERE rp.tenant_id = :tenantId
                                  AND rp.role_id = :role AND rp.permission_id = pm.id AND rp.deleted_at IS NULL)
             ORDER BY pm.code
            """;
    /** One statement for the whole set: application-generated ids paired with the permission ids. */
    private static final String GRANT = """
            INSERT INTO iam.role_permission (id, tenant_id, role_id, permission_id, created_at, created_by, updated_at,
                                             updated_by, row_version)
            SELECT grant_row.id, :tenant, :role, grant_row.permission_id, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:permissions AS uuid[])) AS grant_row (id, permission_id)
            """;
    private static final String ASSIGN = """
            INSERT INTO iam.user_role (id, tenant_id, user_id, role_id, scope_type, is_active, created_at, created_by,
                                       updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :user, :role, 'tenant', true, :now, :actor, :now, :actor, 1)
            """;

    private final JdbcClient jdbc;

    public RoleSeedRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<UUID> findByCode(String code) {
        return jdbc.sql(FIND_BY_CODE).param("code", code).query(UUID.class).optional();
    }

    public void insertSystemRole(UUID id, UUID tenant, String code, String name, String description,
                                 OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_ROLE).param("id", id).param("tenant", tenant).param("code", code).param("name", name)
                .param("description", description).param("now", now).param("actor", actor).update();
    }

    /** Grants the role every permission of the catalogue it lacks; returns how many were added. */
    public int grantEveryPermission(UUID tenant, UUID role, OffsetDateTime now, UUID actor, Clock clock) {
        List<UUID> permissions = jdbc.sql(MISSING_PERMISSIONS).param("role", role).query(UUID.class).list();
        if (permissions.isEmpty()) {
            return 0;
        }
        UUID[] ids = new UUID[permissions.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = UuidV7.next(clock);
        }
        return jdbc.sql(GRANT).param("tenant", tenant).param("role", role).param("now", now).param("actor", actor)
                .param("ids", ids).param("permissions", permissions.toArray(UUID[]::new)).update();
    }

    public void assignTenantWide(UUID id, UUID tenant, UUID user, UUID role, OffsetDateTime now, UUID actor) {
        jdbc.sql(ASSIGN).param("id", id).param("tenant", tenant).param("user", user).param("role", role)
                .param("now", now).param("actor", actor).update();
    }
}
