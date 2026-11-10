package com.mysms.iam.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.RolePermissionEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to iam.role_permission (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class RolePermissionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("roleId", "r.role_id", ValueType.UUID),
            field("permissionId", "r.permission_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RolePermissionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RolePermissionEntity::getCreatedAt),
            Map.entry("updateTime", RolePermissionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.role_id, r.permission_id, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM iam.role_permission r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO iam.role_permission
            (id, tenant_id, role_id, permission_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :roleId, :permissionId, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, role_id, permission_id, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE iam.role_permission
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE iam.role_permission
               SET permission_id = :permissionId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ROLE_PERMISSION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM iam.role_permission
                            WHERE tenant_id = :tenantId
                              AND role_id = :roleId AND permission_id = :permissionId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_PERMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.permission WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_ROLE_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.role WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public RolePermissionRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RolePermissionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RolePermissionRepository::map).optional();
    }

    public Page<RolePermissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RolePermissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RolePermissionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RolePermissionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RolePermissionEntity insert(RolePermissionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("roleId", e.getRoleId())
                .param("permissionId", e.getPermissionId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RolePermissionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RolePermissionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("permissionId", e.getPermissionId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RolePermissionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_role_permission_1: role_id, permission_id where (deleted_at IS NULL). */
    public boolean takenRoleIdPermissionId1(RolePermissionEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ROLE_PERMISSION_1_TAKEN)
                .param("roleId", e.getRoleId())
                .param("permissionId", e.getPermissionId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced iam.permission row exists (live). */
    public boolean existsPermissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PERMISSION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced iam.role row exists (live). */
    public boolean existsRoleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ROLE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.role_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, RolePermissionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RolePermissionEntity map(ResultSet rs, int row) throws SQLException {
        return RolePermissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .roleId(rs.getObject("role_id", UUID.class))
                .permissionId(rs.getObject("permission_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
