package com.mysms.learning.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.RubricAssessmentEntity;
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
 * Plain-JDBC access to lms.rubric_assessment: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RubricAssessmentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("rubricVersion", "r.rubric_version", ValueType.INTEGER),
            field("pointsAwarded", "r.points_awarded", ValueType.DECIMAL),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("groupId", "r.group_id", ValueType.UUID),
            field("rubricCellId", "r.rubric_cell_id", ValueType.UUID),
            field("comment", "r.comment", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RubricAssessmentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RubricAssessmentEntity::getCreatedAt),
            Map.entry("updateTime", RubricAssessmentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.rubric_version, r.points_awarded, r.content_item_id, r.user_id, r.group_id,
                   r.rubric_cell_id, r.comment, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.rubric_assessment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.rubric_assessment
            (id, tenant_id, content_item_id, user_id, group_id, rubric_cell_id, comment, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :contentItemId, :userId, :groupId, :rubricCellId, :comment, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, rubric_version, points_awarded, content_item_id, user_id, group_id, rubric_cell_id,
                      comment, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.rubric_assessment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.rubric_assessment
               SET content_item_id = :contentItemId,
                   user_id = :userId,
                   group_id = :groupId,
                   rubric_cell_id = :rubricCellId,
                   comment = :comment,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_RUBRIC_CELL_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_cell WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.rubric_cell p0 JOIN lms.rubric p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.rubric_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.rubric_cell_id IN (SELECT p0.id FROM lms.rubric_cell p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.rubric_id IN (SELECT
             p1.id FROM lms.rubric p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public RubricAssessmentRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RubricAssessmentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RubricAssessmentRepository::map).optional();
    }

    public Page<RubricAssessmentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RubricAssessmentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RubricAssessmentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RubricAssessmentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RubricAssessmentEntity insert(RubricAssessmentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("contentItemId", e.getContentItemId())
                .param("userId", e.getUserId())
                .param("groupId", e.getGroupId())
                .param("rubricCellId", e.getRubricCellId())
                .param("comment", e.getComment())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RubricAssessmentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RubricAssessmentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("contentItemId", e.getContentItemId())
                .param("userId", e.getUserId())
                .param("groupId", e.getGroupId())
                .param("rubricCellId", e.getRubricCellId())
                .param("comment", e.getComment())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RubricAssessmentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.assignment_group row exists (live). */
    public boolean existsGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_GROUP_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.rubric_cell row exists (live). */
    public boolean existsRubricCellId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RUBRIC_CELL_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through rubric_cell_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, RubricAssessmentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RubricAssessmentEntity map(ResultSet rs, int row) throws SQLException {
        return RubricAssessmentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .rubricVersion(rs.getObject("rubric_version", Integer.class))
                .pointsAwarded(rs.getBigDecimal("points_awarded"))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .groupId(rs.getObject("group_id", UUID.class))
                .rubricCellId(rs.getObject("rubric_cell_id", UUID.class))
                .comment(rs.getString("comment"))
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
