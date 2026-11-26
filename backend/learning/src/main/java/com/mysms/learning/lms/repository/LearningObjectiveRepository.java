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
import com.mysms.learning.lms.domain.LearningObjectiveEntity;
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
 * Plain-JDBC access to lms.learning_objective: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LearningObjectiveRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("chapterId", "r.chapter_id", ValueType.UUID),
            field("objective", "r.objective", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("copiedFromId", "r.copied_from_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LearningObjectiveEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LearningObjectiveEntity::getCreatedAt),
            Map.entry("updateTime", LearningObjectiveEntity::getUpdatedAt),
            Map.entry("sortOrder", LearningObjectiveEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.course_id, r.chapter_id, r.objective, r.sort_order, r.copied_from_id,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.learning_objective r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.learning_objective
            (id, tenant_id, course_id, chapter_id, objective, sort_order, copied_from_id, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :chapterId, :objective, COALESCE(:sortOrder, 0), :copiedFromId,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, course_id, chapter_id, objective, sort_order, copied_from_id, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.learning_objective
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.learning_objective
               SET course_id = :courseId,
                   chapter_id = :chapterId,
                   objective = :objective,
                   sort_order = :sortOrder,
                   copied_from_id = :copiedFromId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.learning_objective WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LEARNING_OBJECTIVE_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.learning_objective WHERE tenant_id = :tenantId
                AND copied_from_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_ANSWER_LEARNING_OBJECTIVE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_answer WHERE tenant_id = :tenantId
                AND learning_objective_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.course_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public LearningObjectiveRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LearningObjectiveEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LearningObjectiveRepository::map).optional();
    }

    public Page<LearningObjectiveEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LearningObjectiveEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LearningObjectiveRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LearningObjectiveEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LearningObjectiveEntity insert(LearningObjectiveEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("courseId", e.getCourseId())
                .param("chapterId", e.getChapterId())
                .param("objective", e.getObjective())
                .param("sortOrder", e.getSortOrder())
                .param("copiedFromId", e.getCopiedFromId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LearningObjectiveRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LearningObjectiveEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("courseId", e.getCourseId())
                .param("chapterId", e.getChapterId())
                .param("objective", e.getObjective())
                .param("sortOrder", e.getSortOrder())
                .param("copiedFromId", e.getCopiedFromId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LearningObjectiveEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.chapter row exists (live). */
    public boolean existsChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CHAPTER_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.learning_objective row exists (live). */
    public boolean existsCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COPIED_FROM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.learning_objective rows still reference the row. */
    public boolean referencedByLearningObjectiveCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEARNING_OBJECTIVE_COPIED_FROM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_answer rows still reference the row. */
    public boolean referencedByQuizAnswerLearningObjectiveId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_ANSWER_LEARNING_OBJECTIVE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through course_id (empty: a tenant-level row, or none). */
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

    private static final String OF_COURSE = SELECT_LIVE + " AND r.course_id = :courseId ORDER BY r.sort_order";

    /** The course's live objectives, in order. */
    public List<LearningObjectiveEntity> ofCourse(UUID courseId) {
        return jdbc.sql(OF_COURSE).param("courseId", courseId).query(LearningObjectiveRepository::map).list();
    }

    private static List<Object> sortValues(SortSpec sort, LearningObjectiveEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LearningObjectiveEntity map(ResultSet rs, int row) throws SQLException {
        return LearningObjectiveEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .chapterId(rs.getObject("chapter_id", UUID.class))
                .objective(rs.getString("objective"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .copiedFromId(rs.getObject("copied_from_id", UUID.class))
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
