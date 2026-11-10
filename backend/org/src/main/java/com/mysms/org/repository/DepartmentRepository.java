package com.mysms.org.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.org.config.OrgDatabaseConfig;
import com.mysms.org.domain.DepartmentEntity;
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
 * Plain-JDBC access to org.department (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class DepartmentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("parentDepartmentId", "r.parent_department_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<DepartmentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", DepartmentEntity::getCreatedAt),
            Map.entry("updateTime", DepartmentEntity::getUpdatedAt),
            Map.entry("code", DepartmentEntity::getCode),
            Map.entry("name", DepartmentEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.parent_department_id, r.code, r.name, r.is_active,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM org.department r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO org.department
            (id, tenant_id, institution_id, parent_department_id, code, name, is_active, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :parentDepartmentId, :code, :name, COALESCE(:isActive, true),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, parent_department_id, code, name, is_active, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE org.department
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE org.department
               SET institution_id = :institutionId,
                   parent_department_id = :parentDepartmentId,
                   code = :code,
                   name = :name,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_DEPARTMENT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM org.department
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_INSTITUTION_ID = """
            SELECT EXISTS (SELECT 1 FROM org.institution WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PARENT_DEPARTMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM org.department WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_DEPARTMENT_PARENT_DEPARTMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM org.department WHERE tenant_id = :tenantId
                AND parent_department_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public DepartmentRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<DepartmentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(DepartmentRepository::map).optional();
    }

    public Page<DepartmentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<DepartmentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(DepartmentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), DepartmentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public DepartmentEntity insert(DepartmentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("parentDepartmentId", e.getParentDepartmentId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(DepartmentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(DepartmentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("parentDepartmentId", e.getParentDepartmentId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(DepartmentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_department_1: institution_id, code where (deleted_at IS NULL). */
    public boolean takenInstitutionIdCode1(DepartmentEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_DEPARTMENT_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced org.institution row exists (live). */
    public boolean existsInstitutionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_INSTITUTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced org.department row exists (live). */
    public boolean existsParentDepartmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARENT_DEPARTMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live org.department rows still reference the row. */
    public boolean referencedByDepartmentParentDepartmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_DEPARTMENT_PARENT_DEPARTMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, DepartmentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static DepartmentEntity map(ResultSet rs, int row) throws SQLException {
        return DepartmentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .parentDepartmentId(rs.getObject("parent_department_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .isActive(rs.getObject("is_active", Boolean.class))
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
