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
import com.mysms.lms.domain.QuizEntity;
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
 * Plain-JDBC access to lms.quiz: live rows only, keyset paging, and the row_version guard on every update and delete.
 * Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class QuizRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("version", "r.version", ValueType.INTEGER),
            field("supersededById", "r.superseded_by_id", ValueType.UUID),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING),
            field("courseId", "r.course_id", ValueType.UUID),
            field("lessonId", "r.lesson_id", ValueType.UUID),
            field("maxAttempts", "r.max_attempts", ValueType.INTEGER),
            field("passingPercentage", "r.passing_percentage", ValueType.INTEGER),
            field("totalMarks", "r.total_marks", ValueType.DECIMAL),
            field("showAnswers", "r.show_answers", ValueType.BOOLEAN),
            field("showSubmissionHistory", "r.show_submission_history", ValueType.BOOLEAN),
            field("shuffleQuestions", "r.shuffle_questions", ValueType.BOOLEAN),
            field("limitQuestionsTo", "r.limit_questions_to", ValueType.INTEGER),
            field("durationMinutes", "r.duration_minutes", ValueType.INTEGER),
            field("proctoringEnabled", "r.proctoring_enabled", ValueType.BOOLEAN),
            field("maxViolations", "r.max_violations", ValueType.INTEGER),
            field("negativeMarkingEnabled", "r.negative_marking_enabled", ValueType.BOOLEAN),
            field("negativeMarks", "r.negative_marks", ValueType.DECIMAL),
            field("schedulingEnabled", "r.scheduling_enabled", ValueType.BOOLEAN),
            field("scheduleStart", "r.schedule_start", ValueType.TIMESTAMP),
            field("scheduleEnd", "r.schedule_end", ValueType.TIMESTAMP),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("description", "r.description", ValueType.STRING),
            field("isAutoGraded", "r.is_auto_graded", ValueType.BOOLEAN),
            field("isAdaptive", "r.is_adaptive", ValueType.BOOLEAN),
            field("equalQuestionWeighting", "r.equal_question_weighting", ValueType.BOOLEAN),
            field("marksPerQuestion", "r.marks_per_question", ValueType.DECIMAL),
            field("allowPause", "r.allow_pause", ValueType.BOOLEAN),
            field("attemptScoring", "r.attempt_scoring", ValueType.STRING),
            field("dropLowestAttempt", "r.drop_lowest_attempt", ValueType.BOOLEAN),
            field("drawsFromPool", "r.draws_from_pool", ValueType.BOOLEAN),
            field("showFeedback", "r.show_feedback", ValueType.BOOLEAN),
            field("isInRepository", "r.is_in_repository", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<QuizEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", QuizEntity::getCreatedAt),
            Map.entry("updateTime", QuizEntity::getUpdatedAt),
            Map.entry("title", QuizEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.version, r.superseded_by_id, r.published_at, r.title, r.course_id,
                   r.lesson_id, r.max_attempts, r.passing_percentage, r.total_marks, r.show_answers,
                   r.show_submission_history, r.shuffle_questions, r.limit_questions_to, r.duration_minutes,
                   r.proctoring_enabled, r.max_violations, r.negative_marking_enabled, r.negative_marks,
                   r.scheduling_enabled, r.schedule_start, r.schedule_end, r.institution_id, r.description,
                   r.is_auto_graded, r.is_adaptive, r.equal_question_weighting, r.marks_per_question, r.allow_pause,
                   r.attempt_scoring, r.drop_lowest_attempt, r.draws_from_pool, r.show_feedback, r.is_in_repository,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.quiz r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.quiz
            (id, tenant_id, title, course_id, lesson_id, max_attempts, passing_percentage, total_marks, show_answers,
             show_submission_history, shuffle_questions, limit_questions_to, duration_minutes, proctoring_enabled,
             max_violations, negative_marking_enabled, negative_marks, scheduling_enabled, schedule_start,
             schedule_end, institution_id, description, is_auto_graded, is_adaptive, equal_question_weighting,
             marks_per_question, allow_pause, attempt_scoring, drop_lowest_attempt, draws_from_pool, show_feedback,
             is_in_repository, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :title, :courseId, :lessonId, :maxAttempts, :passingPercentage, :totalMarks,
                    COALESCE(:showAnswers, true), COALESCE(:showSubmissionHistory, false),
                    COALESCE(:shuffleQuestions, false), :limitQuestionsTo, :durationMinutes,
                    COALESCE(:proctoringEnabled, false), :maxViolations, COALESCE(:negativeMarkingEnabled, false),
                    :negativeMarks, COALESCE(:schedulingEnabled, false), :scheduleStart, :scheduleEnd,
                    :institutionId, :description, COALESCE(:isAutoGraded, true), COALESCE(:isAdaptive, false),
                    COALESCE(:equalQuestionWeighting, false), :marksPerQuestion, COALESCE(:allowPause, false),
                    COALESCE(:attemptScoring, 'highest'::character varying), COALESCE(:dropLowestAttempt, false),
                    COALESCE(:drawsFromPool, false), COALESCE(:showFeedback, true), COALESCE(:isInRepository, false),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, version, superseded_by_id, published_at, title, course_id, lesson_id, max_attempts,
                      passing_percentage, total_marks, show_answers, show_submission_history, shuffle_questions,
                      limit_questions_to, duration_minutes, proctoring_enabled, max_violations,
                      negative_marking_enabled, negative_marks, scheduling_enabled, schedule_start, schedule_end,
                      institution_id, description, is_auto_graded, is_adaptive, equal_question_weighting,
                      marks_per_question, allow_pause, attempt_scoring, drop_lowest_attempt, draws_from_pool,
                      show_feedback, is_in_repository, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.quiz
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.quiz
               SET title = :title,
                   course_id = :courseId,
                   lesson_id = :lessonId,
                   max_attempts = :maxAttempts,
                   passing_percentage = :passingPercentage,
                   total_marks = :totalMarks,
                   show_answers = :showAnswers,
                   show_submission_history = :showSubmissionHistory,
                   shuffle_questions = :shuffleQuestions,
                   limit_questions_to = :limitQuestionsTo,
                   duration_minutes = :durationMinutes,
                   proctoring_enabled = :proctoringEnabled,
                   max_violations = :maxViolations,
                   negative_marking_enabled = :negativeMarkingEnabled,
                   negative_marks = :negativeMarks,
                   scheduling_enabled = :schedulingEnabled,
                   schedule_start = :scheduleStart,
                   schedule_end = :scheduleEnd,
                   institution_id = :institutionId,
                   description = :description,
                   is_auto_graded = :isAutoGraded,
                   is_adaptive = :isAdaptive,
                   equal_question_weighting = :equalQuestionWeighting,
                   marks_per_question = :marksPerQuestion,
                   allow_pause = :allowPause,
                   attempt_scoring = :attemptScoring,
                   drop_lowest_attempt = :dropLowestAttempt,
                   draws_from_pool = :drawsFromPool,
                   show_feedback = :showFeedback,
                   is_in_repository = :isInRepository,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LESSON_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_AUTHOR_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_author WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_ACTIVITY_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson_activity WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId
                AND superseded_by_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_QUESTION_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_question WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_QUESTION_HISTORY_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_question_history WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_SUBMISSION_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TIMETABLE_ENTRY_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_entry WHERE tenant_id = :tenantId
                AND quiz_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public QuizRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuizEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(QuizRepository::map).optional();
    }

    public Page<QuizEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<QuizEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(QuizRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), QuizEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public QuizEntity insert(QuizEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("title", e.getTitle())
                .param("courseId", e.getCourseId())
                .param("lessonId", e.getLessonId())
                .param("maxAttempts", e.getMaxAttempts())
                .param("passingPercentage", e.getPassingPercentage())
                .param("totalMarks", e.getTotalMarks())
                .param("showAnswers", e.getShowAnswers())
                .param("showSubmissionHistory", e.getShowSubmissionHistory())
                .param("shuffleQuestions", e.getShuffleQuestions())
                .param("limitQuestionsTo", e.getLimitQuestionsTo())
                .param("durationMinutes", e.getDurationMinutes())
                .param("proctoringEnabled", e.getProctoringEnabled())
                .param("maxViolations", e.getMaxViolations())
                .param("negativeMarkingEnabled", e.getNegativeMarkingEnabled())
                .param("negativeMarks", e.getNegativeMarks())
                .param("schedulingEnabled", e.getSchedulingEnabled())
                .param("scheduleStart", e.getScheduleStart())
                .param("scheduleEnd", e.getScheduleEnd())
                .param("institutionId", e.getInstitutionId())
                .param("description", e.getDescription())
                .param("isAutoGraded", e.getIsAutoGraded())
                .param("isAdaptive", e.getIsAdaptive())
                .param("equalQuestionWeighting", e.getEqualQuestionWeighting())
                .param("marksPerQuestion", e.getMarksPerQuestion())
                .param("allowPause", e.getAllowPause())
                .param("attemptScoring", e.getAttemptScoring())
                .param("dropLowestAttempt", e.getDropLowestAttempt())
                .param("drawsFromPool", e.getDrawsFromPool())
                .param("showFeedback", e.getShowFeedback())
                .param("isInRepository", e.getIsInRepository())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(QuizRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(QuizEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("title", e.getTitle())
                .param("courseId", e.getCourseId())
                .param("lessonId", e.getLessonId())
                .param("maxAttempts", e.getMaxAttempts())
                .param("passingPercentage", e.getPassingPercentage())
                .param("totalMarks", e.getTotalMarks())
                .param("showAnswers", e.getShowAnswers())
                .param("showSubmissionHistory", e.getShowSubmissionHistory())
                .param("shuffleQuestions", e.getShuffleQuestions())
                .param("limitQuestionsTo", e.getLimitQuestionsTo())
                .param("durationMinutes", e.getDurationMinutes())
                .param("proctoringEnabled", e.getProctoringEnabled())
                .param("maxViolations", e.getMaxViolations())
                .param("negativeMarkingEnabled", e.getNegativeMarkingEnabled())
                .param("negativeMarks", e.getNegativeMarks())
                .param("schedulingEnabled", e.getSchedulingEnabled())
                .param("scheduleStart", e.getScheduleStart())
                .param("scheduleEnd", e.getScheduleEnd())
                .param("institutionId", e.getInstitutionId())
                .param("description", e.getDescription())
                .param("isAutoGraded", e.getIsAutoGraded())
                .param("isAdaptive", e.getIsAdaptive())
                .param("equalQuestionWeighting", e.getEqualQuestionWeighting())
                .param("marksPerQuestion", e.getMarksPerQuestion())
                .param("allowPause", e.getAllowPause())
                .param("attemptScoring", e.getAttemptScoring())
                .param("dropLowestAttempt", e.getDropLowestAttempt())
                .param("drawsFromPool", e.getDrawsFromPool())
                .param("showFeedback", e.getShowFeedback())
                .param("isInRepository", e.getIsInRepository())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(QuizEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.lesson row exists (live). */
    public boolean existsLessonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LESSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz row exists (live). */
    public boolean existsSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SUPERSEDED_BY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.content_author rows still reference the row. */
    public boolean referencedByContentAuthorQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_AUTHOR_QUIZ_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_QUIZ_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.lesson_activity rows still reference the row. */
    public boolean referencedByLessonActivityQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_ACTIVITY_QUIZ_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.quiz rows still reference the row. */
    public boolean referencedByQuizSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_SUPERSEDED_BY_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.quiz_question rows still reference the row. */
    public boolean referencedByQuizQuestionQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_QUESTION_QUIZ_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.quiz_question_history rows still reference the row. */
    public boolean referencedByQuizQuestionHistoryQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_QUESTION_HISTORY_QUIZ_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_submission rows still reference the row. */
    public boolean referencedByQuizSubmissionQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_SUBMISSION_QUIZ_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.timetable_entry rows still reference the row. */
    public boolean referencedByTimetableEntryQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TIMETABLE_ENTRY_QUIZ_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, QuizEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static QuizEntity map(ResultSet rs, int row) throws SQLException {
        return QuizEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .version(rs.getObject("version", Integer.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .title(rs.getString("title"))
                .courseId(rs.getObject("course_id", UUID.class))
                .lessonId(rs.getObject("lesson_id", UUID.class))
                .maxAttempts(rs.getObject("max_attempts", Integer.class))
                .passingPercentage(rs.getObject("passing_percentage", Integer.class))
                .totalMarks(rs.getBigDecimal("total_marks"))
                .showAnswers(rs.getObject("show_answers", Boolean.class))
                .showSubmissionHistory(rs.getObject("show_submission_history", Boolean.class))
                .shuffleQuestions(rs.getObject("shuffle_questions", Boolean.class))
                .limitQuestionsTo(rs.getObject("limit_questions_to", Integer.class))
                .durationMinutes(rs.getObject("duration_minutes", Integer.class))
                .proctoringEnabled(rs.getObject("proctoring_enabled", Boolean.class))
                .maxViolations(rs.getObject("max_violations", Integer.class))
                .negativeMarkingEnabled(rs.getObject("negative_marking_enabled", Boolean.class))
                .negativeMarks(rs.getBigDecimal("negative_marks"))
                .schedulingEnabled(rs.getObject("scheduling_enabled", Boolean.class))
                .scheduleStart(rs.getObject("schedule_start", OffsetDateTime.class))
                .scheduleEnd(rs.getObject("schedule_end", OffsetDateTime.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .description(rs.getString("description"))
                .isAutoGraded(rs.getObject("is_auto_graded", Boolean.class))
                .isAdaptive(rs.getObject("is_adaptive", Boolean.class))
                .equalQuestionWeighting(rs.getObject("equal_question_weighting", Boolean.class))
                .marksPerQuestion(rs.getBigDecimal("marks_per_question"))
                .allowPause(rs.getObject("allow_pause", Boolean.class))
                .attemptScoring(rs.getString("attempt_scoring"))
                .dropLowestAttempt(rs.getObject("drop_lowest_attempt", Boolean.class))
                .drawsFromPool(rs.getObject("draws_from_pool", Boolean.class))
                .showFeedback(rs.getObject("show_feedback", Boolean.class))
                .isInRepository(rs.getObject("is_in_repository", Boolean.class))
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
