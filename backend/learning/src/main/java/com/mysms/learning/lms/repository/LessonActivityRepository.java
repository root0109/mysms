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
import com.mysms.learning.lms.domain.LessonActivityEntity;
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
 * Plain-JDBC access to lms.lesson_activity: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LessonActivityRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("lessonId", "r.lesson_id", ValueType.UUID),
            field("quizId", "r.quiz_id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("exerciseId", "r.exercise_id", ValueType.UUID),
            field("teacherOnly", "r.teacher_only", ValueType.BOOLEAN),
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

    private static final Map<String, Function<LessonActivityEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LessonActivityEntity::getCreatedAt),
            Map.entry("updateTime", LessonActivityEntity::getUpdatedAt),
            Map.entry("sortOrder", LessonActivityEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.lesson_id, r.quiz_id, r.assignment_id, r.exercise_id, r.teacher_only,
                   r.sort_order, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.lesson_activity r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** A lesson's activities in their order; students see those that are not teacher-only. */
    private static final String OF_LESSON = SELECT_LIVE
            + " AND r.lesson_id = :lessonId AND (:teacherOnlyToo OR NOT r.teacher_only) ORDER BY r.sort_order, r.id";
    private static final String INSERT = """
            INSERT INTO lms.lesson_activity
            (id, tenant_id, lesson_id, quiz_id, assignment_id, exercise_id, teacher_only, sort_order, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :lessonId, :quizId, :assignmentId, :exerciseId, COALESCE(:teacherOnly, false),
                    COALESCE(:sortOrder, 0), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, lesson_id, quiz_id, assignment_id, exercise_id, teacher_only, sort_order,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.lesson_activity
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.lesson_activity
               SET quiz_id = :quizId,
                   assignment_id = :assignmentId,
                   exercise_id = :exerciseId,
                   teacher_only = :teacherOnly,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.programming_exercise WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LESSON_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p2.institution_id FROM lms.lesson p0 JOIN lms.chapter p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.chapter_id JOIN lms.course p2
            ON p2.tenant_id = :tenantId AND p2.id = p1.course_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.lesson_id IN (SELECT p0.id FROM lms.lesson p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.chapter_id IN (SELECT p1.id FROM
             lms.chapter p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.course_id IN (SELECT p2.id FROM lms.course p2 WHERE p2.tenant_id = :tenantId
                 AND (p2.institution_id =
             ANY(:scopeIds) OR p2.institution_id IS NULL)))))))
            """;

    private final JdbcClient jdbc;

    public LessonActivityRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LessonActivityEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LessonActivityRepository::map).optional();
    }

    public Page<LessonActivityEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LessonActivityEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LessonActivityRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LessonActivityEntity::getId);
    }

    /** The lesson's activities in order, teacher-only ones included only when asked. */
    public List<LessonActivityEntity> ofLesson(UUID lessonId, boolean teacherOnlyToo) {
        return jdbc.sql(OF_LESSON).param("lessonId", lessonId).param("teacherOnlyToo", teacherOnlyToo)
                .query(LessonActivityRepository::map).list();
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LessonActivityEntity insert(LessonActivityEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("lessonId", e.getLessonId())
                .param("quizId", e.getQuizId())
                .param("assignmentId", e.getAssignmentId())
                .param("exerciseId", e.getExerciseId())
                .param("teacherOnly", e.getTeacherOnly())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LessonActivityRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LessonActivityEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("quizId", e.getQuizId())
                .param("assignmentId", e.getAssignmentId())
                .param("exerciseId", e.getExerciseId())
                .param("teacherOnly", e.getTeacherOnly())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LessonActivityEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.assignment row exists (live). */
    public boolean existsAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.programming_exercise row exists (live). */
    public boolean existsExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXERCISE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.lesson row exists (live). */
    public boolean existsLessonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LESSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz row exists (live). */
    public boolean existsQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUIZ_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through lesson_id (empty: a tenant-level row, or none). */
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.lesson_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, LessonActivityEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LessonActivityEntity map(ResultSet rs, int row) throws SQLException {
        return LessonActivityEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .lessonId(rs.getObject("lesson_id", UUID.class))
                .quizId(rs.getObject("quiz_id", UUID.class))
                .assignmentId(rs.getObject("assignment_id", UUID.class))
                .exerciseId(rs.getObject("exercise_id", UUID.class))
                .teacherOnly(rs.getObject("teacher_only", Boolean.class))
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
