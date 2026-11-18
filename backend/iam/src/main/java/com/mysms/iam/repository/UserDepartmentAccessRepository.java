package com.mysms.iam.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.UserDepartmentAccessEntity;
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
 * Plain-JDBC access to iam.user_department_access: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class UserDepartmentAccessRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("departmentId", "r.department_id", ValueType.UUID),
            field("isPrimary", "r.is_primary", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<UserDepartmentAccessEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", UserDepartmentAccessEntity::getCreatedAt),
            Map.entry("updateTime", UserDepartmentAccessEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.department_id, r.is_primary, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM iam.user_department_access r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO iam.user_department_access
            (id, tenant_id, user_id, department_id, is_primary, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :userId, :departmentId, COALESCE(:isPrimary, false), :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, department_id, is_primary, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE iam.user_department_access
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE iam.user_department_access
               SET department_id = :departmentId,
                   is_primary = :isPrimary,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_USER_DEPARTMENT_ACCESS_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM iam.user_department_access
                            WHERE tenant_id = :tenantId
                              AND user_id = :userId AND department_id = :departmentId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.user_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public UserDepartmentAccessRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<UserDepartmentAccessEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(UserDepartmentAccessRepository::map).optional();
    }

    public Page<UserDepartmentAccessEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<UserDepartmentAccessEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(UserDepartmentAccessRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), UserDepartmentAccessEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public UserDepartmentAccessEntity insert(UserDepartmentAccessEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("departmentId", e.getDepartmentId())
                .param("isPrimary", e.getIsPrimary())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(UserDepartmentAccessRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(UserDepartmentAccessEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("departmentId", e.getDepartmentId())
                .param("isPrimary", e.getIsPrimary())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(UserDepartmentAccessEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_user_department_access_1: user_id, department_id where (deleted_at IS NULL). */
    public boolean takenUserIdDepartmentId1(UserDepartmentAccessEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_USER_DEPARTMENT_ACCESS_1_TAKEN)
                .param("userId", e.getUserId())
                .param("departmentId", e.getDepartmentId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced iam.user_account row exists (live). */
    public boolean existsUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_USER_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.user_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, UserDepartmentAccessEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static UserDepartmentAccessEntity map(ResultSet rs, int row) throws SQLException {
        return UserDepartmentAccessEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .departmentId(rs.getObject("department_id", UUID.class))
                .isPrimary(rs.getObject("is_primary", Boolean.class))
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
