package com.mysms.academics.assessment.repository;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.domain.AssignmentTypeEntity;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to assessment.assignment_type: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class AssignmentTypeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("gradingBasis", "r.grading_basis", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AssignmentTypeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AssignmentTypeEntity::getCreatedAt),
            Map.entry("updateTime", AssignmentTypeEntity::getUpdatedAt),
            Map.entry("sortOrder", AssignmentTypeEntity::getSortOrder),
            Map.entry("code", AssignmentTypeEntity::getCode),
            Map.entry("name", AssignmentTypeEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.sort_order, r.is_active, r.code, r.name, r.grading_basis, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.assignment_type r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.assignment_type
            (id, tenant_id, sort_order, is_active, code, name, grading_basis, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, COALESCE(:sortOrder, 0), COALESCE(:isActive, true), :code, :name,
                    COALESCE(:gradingBasis, 'submission'::character varying), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, sort_order, is_active, code, name, grading_basis, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.assignment_type
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.assignment_type
               SET sort_order = :sortOrder,
                   is_active = :isActive,
                   code = :code,
                   name = :name,
                   grading_basis = :gradingBasis,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ASSIGNMENT_TYPE_SCOPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.assignment_type
                            WHERE tenant_id = :tenantId
                              AND code = :code AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_ASSIGNMENT_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.assignment WHERE tenant_id = :tenantId
                AND assignment_type_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public AssignmentTypeRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AssignmentTypeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AssignmentTypeRepository::map).optional();
    }

    public Page<AssignmentTypeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AssignmentTypeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AssignmentTypeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AssignmentTypeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AssignmentTypeEntity insert(AssignmentTypeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("gradingBasis", e.getGradingBasis())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssignmentTypeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AssignmentTypeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("gradingBasis", e.getGradingBasis())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AssignmentTypeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_assignment_type_scope_code: code where (deleted_at IS NULL). */
    public boolean takenCodeCode(AssignmentTypeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ASSIGNMENT_TYPE_SCOPE_CODE_TAKEN).param("code", e.getCode())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live assessment.assignment rows still reference the row. */
    public boolean referencedByAssignmentAssignmentTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_ASSIGNMENT_TYPE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, AssignmentTypeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AssignmentTypeEntity map(ResultSet rs, int row) throws SQLException {
        return AssignmentTypeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .isActive(rs.getObject("is_active", Boolean.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .gradingBasis(rs.getString("grading_basis"))
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
