package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.LessonPlanApprovalEntity;
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
 * Plain-JDBC access to academics.lesson_plan_approval (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class LessonPlanApprovalRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("lessonPlanId", "r.lesson_plan_id", ValueType.UUID),
            field("decision", "r.decision", ValueType.STRING),
            field("comments", "r.comments", ValueType.STRING),
            field("decidedByUserId", "r.decided_by_user_id", ValueType.UUID),
            field("decidedAt", "r.decided_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LessonPlanApprovalEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LessonPlanApprovalEntity::getCreatedAt),
            Map.entry("updateTime", LessonPlanApprovalEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.lesson_plan_id, r.decision, r.comments, r.decided_by_user_id, r.decided_at,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM academics.lesson_plan_approval r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.lesson_plan_approval
            (id, tenant_id, lesson_plan_id, decision, comments, decided_by_user_id, decided_at, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :lessonPlanId, COALESCE(:decision, 'pending'::character varying), :comments,
                    :decidedByUserId, :decidedAt, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, lesson_plan_id, decision, comments, decided_by_user_id, decided_at, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.lesson_plan_approval
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.lesson_plan_approval
               SET decision = :decision,
                   comments = :comments,
                   decided_by_user_id = :decidedByUserId,
                   decided_at = :decidedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_LESSON_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.lesson_plan WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public LessonPlanApprovalRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LessonPlanApprovalEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LessonPlanApprovalRepository::map).optional();
    }

    public Page<LessonPlanApprovalEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LessonPlanApprovalEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LessonPlanApprovalRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LessonPlanApprovalEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LessonPlanApprovalEntity insert(LessonPlanApprovalEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("lessonPlanId", e.getLessonPlanId())
                .param("decision", e.getDecision())
                .param("comments", e.getComments())
                .param("decidedByUserId", e.getDecidedByUserId())
                .param("decidedAt", e.getDecidedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LessonPlanApprovalRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LessonPlanApprovalEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("decision", e.getDecision())
                .param("comments", e.getComments())
                .param("decidedByUserId", e.getDecidedByUserId())
                .param("decidedAt", e.getDecidedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LessonPlanApprovalEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced academics.lesson_plan row exists (live). */
    public boolean existsLessonPlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LESSON_PLAN_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.lesson_plan_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, LessonPlanApprovalEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LessonPlanApprovalEntity map(ResultSet rs, int row) throws SQLException {
        return LessonPlanApprovalEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .lessonPlanId(rs.getObject("lesson_plan_id", UUID.class))
                .decision(rs.getString("decision"))
                .comments(rs.getString("comments"))
                .decidedByUserId(rs.getObject("decided_by_user_id", UUID.class))
                .decidedAt(rs.getObject("decided_at", OffsetDateTime.class))
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
