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
import com.mysms.learning.lms.domain.QuizSubmissionEntity;
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
 * Plain-JDBC access to lms.quiz_submission: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class QuizSubmissionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("attemptNumber", "r.attempt_number", ValueType.INTEGER),
            field("quizId", "r.quiz_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("score", "r.score", ValueType.DECIMAL),
            field("scoreOutOf", "r.score_out_of", ValueType.DECIMAL),
            field("percentage", "r.percentage", ValueType.DECIMAL),
            field("passingPercentage", "r.passing_percentage", ValueType.DECIMAL),
            field("violationCount", "r.violation_count", ValueType.INTEGER),
            field("submissionReason", "r.submission_reason", ValueType.STRING),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("startedAt", "r.started_at", ValueType.TIMESTAMP),
            field("submittedAt", "r.submitted_at", ValueType.TIMESTAMP),
            field("timeTakenSeconds", "r.time_taken_seconds", ValueType.INTEGER),
            field("answeredCount", "r.answered_count", ValueType.INTEGER),
            field("correctCount", "r.correct_count", ValueType.INTEGER),
            field("incorrectCount", "r.incorrect_count", ValueType.INTEGER),
            field("isReassigned", "r.is_reassigned", ValueType.BOOLEAN),
            field("reassignedFromId", "r.reassigned_from_id", ValueType.UUID),
            field("teacherComment", "r.teacher_comment", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<QuizSubmissionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", QuizSubmissionEntity::getCreatedAt),
            Map.entry("updateTime", QuizSubmissionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.attempt_number, r.quiz_id, r.user_id, r.course_id, r.score, r.score_out_of,
                   r.percentage, r.passing_percentage, r.violation_count, r.submission_reason, r.content_item_id,
                   r.cohort_id, r.status, r.started_at, r.submitted_at, r.time_taken_seconds, r.answered_count,
                   r.correct_count, r.incorrect_count, r.is_reassigned, r.reassigned_from_id, r.teacher_comment,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.quiz_submission r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.quiz_submission
            (id, tenant_id, quiz_id, user_id, course_id, passing_percentage, violation_count, submission_reason,
             content_item_id, cohort_id, time_taken_seconds, answered_count, correct_count, incorrect_count,
             is_reassigned, reassigned_from_id, teacher_comment, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :quizId, :userId, :courseId, :passingPercentage, COALESCE(:violationCount, 0),
                    :submissionReason, :contentItemId, :cohortId, :timeTakenSeconds, :answeredCount, :correctCount,
                    :incorrectCount, COALESCE(:isReassigned, false), :reassignedFromId, :teacherComment, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, attempt_number, quiz_id, user_id, course_id, score, score_out_of, percentage,
                      passing_percentage, violation_count, submission_reason, content_item_id, cohort_id, status,
                      started_at, submitted_at, time_taken_seconds, answered_count, correct_count, incorrect_count,
                      is_reassigned, reassigned_from_id, teacher_comment, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.quiz_submission
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.quiz_submission
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_QUIZ_SUBMISSION_ATTEMPT_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission
                            WHERE tenant_id = :tenantId
                              AND quiz_id = :quizId AND user_id = :userId AND content_item_id = :contentItemId AND
                                  attempt_number = :attemptNumber AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_REASSIGNED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_GRADEBOOK_ENTRY_QUIZ_SUBMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.gradebook_entry WHERE tenant_id = :tenantId
                AND quiz_submission_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_ANSWER_QUIZ_SUBMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_answer WHERE tenant_id = :tenantId
                AND quiz_submission_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_SUBMISSION_REASSIGNED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId
                AND reassigned_from_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_VIOLATION_QUIZ_SUBMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_violation WHERE tenant_id = :tenantId
                AND quiz_submission_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_REASSIGNMENT_LOG_QUIZ_SUBMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.reassignment_log WHERE tenant_id = :tenantId
                AND quiz_submission_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.quiz p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.quiz_id IN (SELECT p0.id FROM lms.quiz p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private static final String TAUGHT = """
             COALESCE(r.cohort_id, (SELECT COALESCE(ci.cohort_id, ch.cohort_id) FROM lms.content_item ci
                                      LEFT JOIN lms.chapter ch ON ch.id = ci.chapter_id AND ch.tenant_id = :tenantId
                                     WHERE ci.tenant_id = :tenantId AND ci.id = r.content_item_id))
                 IN (""" + TeachingReach.TAUGHT_COHORT_IDS + ")";
    private static final String IS_TAUGHT = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission r WHERE r.tenant_id = :tenantId AND r.id = :id
                              AND r.deleted_at IS NULL AND\s""" + TAUGHT + ")";

    private final JdbcClient jdbc;

    public QuizSubmissionRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuizSubmissionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(QuizSubmissionRepository::map).optional();
    }

    public Page<QuizSubmissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<QuizSubmissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(QuizSubmissionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), QuizSubmissionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public QuizSubmissionEntity insert(QuizSubmissionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("quizId", e.getQuizId())
                .param("userId", e.getUserId())
                .param("courseId", e.getCourseId())
                .param("passingPercentage", e.getPassingPercentage())
                .param("violationCount", e.getViolationCount())
                .param("submissionReason", e.getSubmissionReason())
                .param("contentItemId", e.getContentItemId())
                .param("cohortId", e.getCohortId())
                .param("timeTakenSeconds", e.getTimeTakenSeconds())
                .param("answeredCount", e.getAnsweredCount())
                .param("correctCount", e.getCorrectCount())
                .param("incorrectCount", e.getIncorrectCount())
                .param("isReassigned", e.getIsReassigned())
                .param("reassignedFromId", e.getReassignedFromId())
                .param("teacherComment", e.getTeacherComment())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(QuizSubmissionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(QuizSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(QuizSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_quiz_submission_attempt: quiz_id, user_id, content_item_id, attempt_number where (deleted_at IS NULL). */
    public boolean takenQuizIdUserIdContentItemIdAttemptNumberAttempt(QuizSubmissionEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_QUIZ_SUBMISSION_ATTEMPT_TAKEN)
                .param("quizId", e.getQuizId())
                .param("userId", e.getUserId())
                .param("contentItemId", e.getContentItemId())
                .param("attemptNumber", e.getAttemptNumber())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz row exists (live). */
    public boolean existsQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUIZ_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz_submission row exists (live). */
    public boolean existsReassignedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_REASSIGNED_FROM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.gradebook_entry rows still reference the row. */
    public boolean referencedByGradebookEntryQuizSubmissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GRADEBOOK_ENTRY_QUIZ_SUBMISSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_answer rows still reference the row. */
    public boolean referencedByQuizAnswerQuizSubmissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_ANSWER_QUIZ_SUBMISSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_submission rows still reference the row. */
    public boolean referencedByQuizSubmissionReassignedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_SUBMISSION_REASSIGNED_FROM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_violation rows still reference the row. */
    public boolean referencedByQuizViolationQuizSubmissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_VIOLATION_QUIZ_SUBMISSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.reassignment_log rows still reference the row. */
    public boolean referencedByReassignmentLogQuizSubmissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_REASSIGNMENT_LOG_QUIZ_SUBMISSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through quiz_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** The caller's own attempts and those of the cohorts they teach. */
    public static SqlFragment ownOrTaught(TeachingReach reach) {
        return new SqlFragment("r.user_id = :reachUser OR " + TAUGHT, reach.params());
    }

    /** Whether the attempt belongs to a cohort the caller teaches. */
    public boolean taught(UUID id, TeachingReach reach) {
        return Boolean.TRUE.equals(jdbc.sql(IS_TAUGHT).params(reach.params()).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static final String START_ATTEMPT = """
            UPDATE lms.quiz_submission
               SET status = 'in_progress', started_at = :startedAt, attempt_number = :attemptNumber,
                   updated_at = :startedAt, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;

    /** Marks a fresh submission as the started attempt (workflow columns the generic insert leaves alone). */
    public void startAttempt(UUID id, int attemptNumber, OffsetDateTime startedAt) {
        jdbc.sql(START_ATTEMPT).param("id", id).param("attemptNumber", attemptNumber).param("startedAt", startedAt)
                .update();
    }

    private static final String OPEN_ATTEMPT = SELECT_LIVE + " AND r.quiz_id = :quizId AND r.user_id = :userId"
            + " AND r.content_item_id = :itemId"
            + " AND r.status IN ('in_progress', 'paused') ORDER BY r.created_at DESC LIMIT 1";
    private static final String ATTEMPTS_USED = """
            SELECT count(*) FROM lms.quiz_submission r
             WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL
               AND r.quiz_id = :quizId AND r.user_id = :userId AND r.content_item_id = :itemId
               AND r.status IN ('submitted', 'graded')
            """;

    /** The user's open (in-progress or paused) attempt on the placement, newest first. */
    public Optional<QuizSubmissionEntity> openAttempt(UUID quizId, UUID userId, UUID itemId) {
        return jdbc.sql(OPEN_ATTEMPT).param("quizId", quizId).param("userId", userId).param("itemId", itemId)
                .query(QuizSubmissionRepository::map).optional();
    }

    /** How many attempts on the placement the user has handed in. */
    public int attemptsUsed(UUID quizId, UUID userId, UUID itemId) {
        return jdbc.sql(ATTEMPTS_USED).param("quizId", quizId).param("userId", userId).param("itemId", itemId)
                .query(Integer.class).single();
    }

    private static List<Object> sortValues(SortSpec sort, QuizSubmissionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static QuizSubmissionEntity map(ResultSet rs, int row) throws SQLException {
        return QuizSubmissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .attemptNumber(rs.getObject("attempt_number", Integer.class))
                .quizId(rs.getObject("quiz_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .score(rs.getBigDecimal("score"))
                .scoreOutOf(rs.getBigDecimal("score_out_of"))
                .percentage(rs.getBigDecimal("percentage"))
                .passingPercentage(rs.getBigDecimal("passing_percentage"))
                .violationCount(rs.getObject("violation_count", Integer.class))
                .submissionReason(rs.getString("submission_reason"))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .status(rs.getString("status"))
                .startedAt(rs.getObject("started_at", OffsetDateTime.class))
                .submittedAt(rs.getObject("submitted_at", OffsetDateTime.class))
                .timeTakenSeconds(rs.getObject("time_taken_seconds", Integer.class))
                .answeredCount(rs.getObject("answered_count", Integer.class))
                .correctCount(rs.getObject("correct_count", Integer.class))
                .incorrectCount(rs.getObject("incorrect_count", Integer.class))
                .isReassigned(rs.getObject("is_reassigned", Boolean.class))
                .reassignedFromId(rs.getObject("reassigned_from_id", UUID.class))
                .teacherComment(rs.getString("teacher_comment"))
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
