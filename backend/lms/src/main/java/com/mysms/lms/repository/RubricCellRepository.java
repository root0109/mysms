package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.RubricCellEntity;
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
 * Plain-JDBC access to lms.rubric_cell (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class RubricCellRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("rubricId", "r.rubric_id", ValueType.UUID),
            field("rubricCriterionId", "r.rubric_criterion_id", ValueType.UUID),
            field("rubricLevelId", "r.rubric_level_id", ValueType.UUID),
            field("description", "r.description", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RubricCellEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RubricCellEntity::getCreatedAt),
            Map.entry("updateTime", RubricCellEntity::getUpdatedAt),
            Map.entry("sortOrder", RubricCellEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.rubric_id, r.rubric_criterion_id, r.rubric_level_id, r.description,
                   r.sort_order, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.rubric_cell r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.rubric_cell
            (id, tenant_id, rubric_id, rubric_criterion_id, rubric_level_id, description, sort_order, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :rubricId, :rubricCriterionId, :rubricLevelId, :description,
                    COALESCE(:sortOrder, 0), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, rubric_id, rubric_criterion_id, rubric_level_id, description, sort_order,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.rubric_cell
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.rubric_cell
               SET rubric_criterion_id = :rubricCriterionId,
                   rubric_level_id = :rubricLevelId,
                   description = :description,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_RUBRIC_CELL_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_cell
                            WHERE tenant_id = :tenantId
                              AND rubric_criterion_id = :rubricCriterionId AND rubric_level_id = :rubricLevelId AND id
                                  <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_RUBRIC_CRITERION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_criterion WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_RUBRIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_RUBRIC_LEVEL_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_level WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_ASSESSMENT_RUBRIC_CELL_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_assessment WHERE tenant_id = :tenantId
                AND rubric_cell_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.rubric p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.rubric_id IN (SELECT p0.id FROM lms.rubric p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public RubricCellRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RubricCellEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RubricCellRepository::map).optional();
    }

    public Page<RubricCellEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RubricCellEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RubricCellRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RubricCellEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RubricCellEntity insert(RubricCellEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("rubricId", e.getRubricId())
                .param("rubricCriterionId", e.getRubricCriterionId())
                .param("rubricLevelId", e.getRubricLevelId())
                .param("description", e.getDescription())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RubricCellRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RubricCellEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("rubricCriterionId", e.getRubricCriterionId())
                .param("rubricLevelId", e.getRubricLevelId())
                .param("description", e.getDescription())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RubricCellEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_rubric_cell_1: rubric_criterion_id, rubric_level_id where (deleted_at IS NULL). */
    public boolean takenRubricCriterionIdRubricLevelId1(RubricCellEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_RUBRIC_CELL_1_TAKEN)
                .param("rubricCriterionId", e.getRubricCriterionId())
                .param("rubricLevelId", e.getRubricLevelId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.rubric_criterion row exists (live). */
    public boolean existsRubricCriterionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RUBRIC_CRITERION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.rubric row exists (live). */
    public boolean existsRubricId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RUBRIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.rubric_level row exists (live). */
    public boolean existsRubricLevelId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RUBRIC_LEVEL_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.rubric_assessment rows still reference the row. */
    public boolean referencedByRubricAssessmentRubricCellId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_ASSESSMENT_RUBRIC_CELL_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through rubric_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.rubric_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, RubricCellEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RubricCellEntity map(ResultSet rs, int row) throws SQLException {
        return RubricCellEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .rubricId(rs.getObject("rubric_id", UUID.class))
                .rubricCriterionId(rs.getObject("rubric_criterion_id", UUID.class))
                .rubricLevelId(rs.getObject("rubric_level_id", UUID.class))
                .description(rs.getString("description"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
