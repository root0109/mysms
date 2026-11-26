package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.model.QuizAnswerKey;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access for the quiz attempt workflows (save, pause, resume, submit, mark): the attempt with the quiz
 * settings it runs under, its answers, the answer keys of its questions, and the guarded writes of each step. Every
 * statement filters the caller's tenant ({@code tenant_id = :tenantId}); row-level security is the backstop. The
 * attempt row is read {@code FOR UPDATE} by every step that changes it, so the steps of one attempt run one after the
 * other.
 */
@Repository
public class QuizAttemptRepository {

    /** One attempt with the settings of its quiz. */
    public record Attempt(UUID id, UUID quizId, UUID userId, UUID contentItemId, UUID cohortId, int attemptNumber,
                          String status, OffsetDateTime startedAt, OffsetDateTime resumedAt, Integer timeTakenSeconds,
                          Long lastClientSequence, int violationCount, int rowVersion, UUID institutionId,
                          Integer durationMinutes, boolean allowPause, Integer maxViolations,
                          boolean negativeMarking, BigDecimal negativeMarks, int passingPercentage,
                          String attemptScoring, boolean showAnswers, OffsetDateTime dueAt,
                          OffsetDateTime closesAt, Integer extraMinutes) {
    }

    /** One stored answer of an attempt. */
    public record StoredAnswer(UUID id, UUID questionId, String answer, BigDecimal marksPossible, Integer sortOrder,
                               Boolean correct, BigDecimal marksAwarded, int rowVersion) {
    }

    /** One question of a quiz with the marks and position it has there. */
    public record QuizQuestionMarks(UUID questionId, BigDecimal marks, int sortOrder) {
    }

    private static final String ATTEMPT_SELECT = """
            SELECT s.id, s.quiz_id, s.user_id, s.content_item_id, s.cohort_id, s.attempt_number, s.status,
                   s.started_at, s.resumed_at, s.time_taken_seconds, s.last_client_sequence, s.violation_count,
                   s.row_version, q.institution_id, q.duration_minutes, q.allow_pause, q.max_violations,
                   q.negative_marking_enabled, q.negative_marks, q.passing_percentage, q.attempt_scoring,
                   q.show_answers, COALESCE(ov.due_at, ci.due_at) AS due_at,
                   CASE WHEN ci.late_submission_allowed
                        THEN COALESCE(ov.due_at, ci.due_at) + make_interval(days => ci.late_submission_days)
                        ELSE COALESCE(ov.due_at, ci.due_at) END AS closes_at,
                   ov.extra_time_minutes
              FROM lms.quiz_submission s
              JOIN lms.quiz q ON q.id = s.quiz_id AND q.tenant_id = :tenantId
              LEFT JOIN lms.content_item ci ON ci.id = s.content_item_id AND ci.tenant_id = :tenantId
              LEFT JOIN LATERAL (SELECT o.due_at, o.extra_time_minutes FROM lms.item_override o
                                  WHERE o.tenant_id = :tenantId AND o.content_item_id = s.content_item_id
                                    AND o.user_id = s.user_id AND o.deleted_at IS NULL
                                  ORDER BY o.created_at DESC LIMIT 1) ov ON TRUE
             WHERE s.tenant_id = :tenantId AND s.id = :id AND s.deleted_at IS NULL
            """;
    private static final String ATTEMPT_FOR_UPDATE = ATTEMPT_SELECT + " FOR UPDATE OF s";
    private static final String QUIZ_QUESTIONS = """
            SELECT question_id, marks, sort_order FROM lms.quiz_question
             WHERE tenant_id = :tenantId AND quiz_id = :quizId AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String ANSWERS = """
            SELECT id, question_id, answer, marks_possible, sort_order, is_correct, marks_awarded, row_version
              FROM lms.quiz_answer
             WHERE tenant_id = :tenantId AND quiz_submission_id = :submissionId AND deleted_at IS NULL
             ORDER BY sort_order NULLS LAST, id
            """;
    private static final String QUESTIONS = """
            SELECT id, question_type, question_text, allows_multiple, marks, case_sensitive, true_false_answer,
                   numeric_answer, numeric_tolerance, numeric_is_range, general_feedback, correct_feedback,
                   incorrect_feedback
              FROM lms.question
             WHERE tenant_id = :tenantId AND id IN (:ids) AND deleted_at IS NULL
            """;
    private static final String OPTIONS = """
            SELECT question_id, id, is_correct, explanation FROM lms.question_option
             WHERE tenant_id = :tenantId AND question_id IN (:ids) AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String ACCEPTED = """
            SELECT question_id, answer_text FROM lms.question_accepted_answer
             WHERE tenant_id = :tenantId AND question_id IN (:ids) AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String BLANKS = """
            SELECT b.question_id, b.id, a.answer_text
              FROM lms.question_blank b
              LEFT JOIN lms.question_blank_answer a ON a.question_blank_id = b.id AND a.tenant_id = :tenantId
                   AND a.deleted_at IS NULL
             WHERE b.tenant_id = :tenantId AND b.question_id IN (:ids) AND b.deleted_at IS NULL
             ORDER BY b.sort_order, b.id, a.id
            """;
    private static final String MATCH_PAIRS = """
            SELECT question_id, id FROM lms.question_match_pair
             WHERE tenant_id = :tenantId AND question_id IN (:ids) AND deleted_at IS NULL
            """;
    private static final String REGIONS = """
            SELECT h.question_id, r.shape, r.coordinates, r.is_correct
              FROM lms.question_hotspot h
              JOIN lms.question_hotspot_region r ON r.question_hotspot_id = h.id AND r.tenant_id = :tenantId
                   AND r.deleted_at IS NULL
             WHERE h.tenant_id = :tenantId AND h.question_id IN (:ids) AND h.deleted_at IS NULL
            """;
    private static final String PATTERNS = """
            SELECT question_id, pattern FROM lms.question_pattern
             WHERE tenant_id = :tenantId AND question_id IN (:ids) AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String INSERT_ANSWER = """
            INSERT INTO lms.quiz_answer (id, tenant_id, quiz_submission_id, question_id, question_text, question_type,
                   answer, marks_possible, sort_order, time_spent_ms, created_at, created_by, updated_at, updated_by,
                   row_version)
            VALUES (:id, :tenantId, :submissionId, :questionId, :questionText, :questionType, :answer, :marksPossible,
                    :sortOrder, :timeSpentMs, :now, :actor, :now, :actor, 1)
            """;
    private static final String SAVE_ANSWER = """
            UPDATE lms.quiz_answer
               SET answer = :answer, is_correct = NULL, marks_awarded = NULL, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String MARK_ANSWER = """
            UPDATE lms.quiz_answer
               SET is_correct = :correct, marks_awarded = :marks, marks_possible = :marksPossible,
                   feedback = :feedback, graded_by = :gradedBy, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String RECORD_SAVE = """
            UPDATE lms.quiz_submission
               SET last_client_sequence = :sequence, answered_count = :answered, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String PAUSE = """
            UPDATE lms.quiz_submission
               SET status = 'paused', time_taken_seconds = :timeTaken, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String RESUME = """
            UPDATE lms.quiz_submission
               SET status = 'in_progress', resumed_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SUBMIT = """
            UPDATE lms.quiz_submission
               SET status = :status, submitted_at = :now, time_taken_seconds = :timeTaken,
                   submission_reason = :reason, violation_count = :violations, passing_percentage = :passing,
                   score = :score, score_out_of = :outOf, percentage = :percentage, answered_count = :answered,
                   correct_count = :correct, incorrect_count = :incorrect, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String RECORD_MARKS = """
            UPDATE lms.quiz_submission
               SET status = :status, score = :score, score_out_of = :outOf, percentage = :percentage,
                   correct_count = :correct, incorrect_count = :incorrect, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String INSERT_VIOLATION = """
            INSERT INTO lms.quiz_violation (id, tenant_id, quiz_submission_id, event_type, severity, occurred_at,
                   created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :submissionId, :eventType, :severity, :occurredAt, :now, :actor, :now, :actor, 1)
            """;
    private static final String SCORED_ATTEMPTS = """
            SELECT percentage FROM lms.quiz_submission
             WHERE tenant_id = :tenantId AND quiz_id = :quizId AND user_id = :userId
               AND content_item_id = :contentItemId AND status = 'graded' AND percentage IS NOT NULL
               AND deleted_at IS NULL
             ORDER BY attempt_number
            """;

    private final JdbcClient jdbc;

    public QuizAttemptRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<Attempt> find(UUID id) {
        return jdbc.sql(ATTEMPT_SELECT).param("id", id).query(QuizAttemptRepository::attempt).optional();
    }

    /** The attempt, locked until the transaction ends. */
    public Optional<Attempt> lock(UUID id) {
        return jdbc.sql(ATTEMPT_FOR_UPDATE).param("id", id).query(QuizAttemptRepository::attempt).optional();
    }

    public List<QuizQuestionMarks> quizQuestions(UUID quizId) {
        return jdbc.sql(QUIZ_QUESTIONS).param("quizId", quizId).query((rs, n) -> new QuizQuestionMarks(
                rs.getObject("question_id", UUID.class), rs.getBigDecimal("marks"), rs.getInt("sort_order"))).list();
    }

    public List<StoredAnswer> answers(UUID submissionId) {
        return jdbc.sql(ANSWERS).param("submissionId", submissionId).query((rs, n) -> new StoredAnswer(
                rs.getObject("id", UUID.class), rs.getObject("question_id", UUID.class), rs.getString("answer"),
                rs.getBigDecimal("marks_possible"), (Integer) rs.getObject("sort_order"),
                (Boolean) rs.getObject("is_correct"), rs.getBigDecimal("marks_awarded"), rs.getInt("row_version")))
                .list();
    }

    /**
     * The answer keys of the given questions, each carrying the marks it has in the attempt (questions the bank no
     * longer holds are left out).
     */
    public Map<UUID, QuizAnswerKey> keys(Map<UUID, BigDecimal> marksByQuestion) {
        if (marksByQuestion.isEmpty()) {
            return Map.of();
        }
        Collection<UUID> ids = marksByQuestion.keySet();
        Map<UUID, List<QuizAnswerKey.Option>> options = grouped(OPTIONS, ids, rs -> new QuizAnswerKey.Option(
                rs.getObject("id", UUID.class), rs.getBoolean("is_correct"), rs.getString("explanation")));
        Map<UUID, List<String>> accepted = grouped(ACCEPTED, ids, rs -> rs.getString("answer_text"));
        Map<UUID, List<UUID>> pairs = grouped(MATCH_PAIRS, ids, rs -> rs.getObject("id", UUID.class));
        Map<UUID, List<QuizAnswerKey.Region>> regions = grouped(REGIONS, ids, rs -> new QuizAnswerKey.Region(
                rs.getString("shape"), rs.getString("coordinates"), rs.getBoolean("is_correct")));
        Map<UUID, List<String>> patterns = grouped(PATTERNS, ids, rs -> rs.getString("pattern"));
        Map<UUID, Map<UUID, List<String>>> blankTexts = new LinkedHashMap<>();
        jdbc.sql(BLANKS).param("ids", ids).query(rs -> {
            List<String> texts = blankTexts.computeIfAbsent(rs.getObject("question_id", UUID.class),
                    k -> new LinkedHashMap<>()).computeIfAbsent(rs.getObject("id", UUID.class), k -> new ArrayList<>());
            String text = rs.getString("answer_text");
            if (text != null) {
                texts.add(text);
            }
        });
        Map<UUID, QuizAnswerKey> keys = new LinkedHashMap<>();
        jdbc.sql(QUESTIONS).param("ids", ids).query(rs -> {
            UUID id = rs.getObject("id", UUID.class);
            List<QuizAnswerKey.Blank> blanks = blankTexts.getOrDefault(id, Map.of()).entrySet().stream()
                    .map(e -> new QuizAnswerKey.Blank(e.getKey(), e.getValue())).toList();
            keys.put(id, new QuizAnswerKey(id, rs.getString("question_type"), rs.getString("question_text"),
                    rs.getBoolean("allows_multiple"), marksByQuestion.get(id), rs.getBoolean("case_sensitive"),
                    (Boolean) rs.getObject("true_false_answer"), rs.getString("numeric_answer"),
                    rs.getString("numeric_tolerance"), rs.getBoolean("numeric_is_range"),
                    rs.getString("general_feedback"), rs.getString("correct_feedback"),
                    rs.getString("incorrect_feedback"), options.getOrDefault(id, List.of()),
                    accepted.getOrDefault(id, List.of()), blanks, Set.copyOf(pairs.getOrDefault(id, List.of())),
                    regions.getOrDefault(id, List.of()), patterns.getOrDefault(id, List.of())));
        });
        return keys;
    }

    public void insertAnswer(UUID id, UUID submissionId, QuizAnswerKey key, String answer, Integer sortOrder,
            Long timeSpentMs, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_ANSWER).param("id", id).param("submissionId", submissionId).param("questionId", key.questionId())
                .param("questionText", key.questionText()).param("questionType", key.questionType())
                .param("answer", answer).param("marksPossible", key.marks()).param("sortOrder", sortOrder)
                .param("timeSpentMs", timeSpentMs).param("now", now).param("actor", actor).update();
    }

    public void saveAnswer(UUID id, String answer, OffsetDateTime now, UUID actor) {
        jdbc.sql(SAVE_ANSWER).param("id", id).param("answer", answer).param("now", now).param("actor", actor).update();
    }

    public void markAnswer(UUID id, Boolean correct, BigDecimal marks, BigDecimal marksPossible, String feedback,
            UUID gradedBy, OffsetDateTime now, UUID actor) {
        jdbc.sql(MARK_ANSWER).param("id", id).param("correct", correct).param("marks", marks)
                .param("marksPossible", marksPossible).param("feedback", feedback).param("gradedBy", gradedBy)
                .param("now", now).param("actor", actor).update();
    }

    public int recordSave(UUID id, int expected, long sequence, int answered, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RECORD_SAVE).param("id", id).param("expected", expected).param("sequence", sequence)
                .param("answered", answered).param("now", now).param("actor", actor).update();
    }

    public int pause(UUID id, int expected, int timeTaken, OffsetDateTime now, UUID actor) {
        return jdbc.sql(PAUSE).param("id", id).param("expected", expected).param("timeTaken", timeTaken)
                .param("now", now).param("actor", actor).update();
    }

    public int resume(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RESUME).param("id", id).param("expected", expected).param("now", now).param("actor", actor)
                .update();
    }

    /** The submitted attempt's outcome. */
    public record Outcome(String status, int timeTaken, String reason, int violations, BigDecimal passing,
                          BigDecimal score, BigDecimal outOf, BigDecimal percentage, int answered, int correct,
                          int incorrect) {
    }

    public int submit(UUID id, int expected, Outcome outcome, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SUBMIT).param("id", id).param("expected", expected).param("status", outcome.status())
                .param("timeTaken", outcome.timeTaken()).param("reason", outcome.reason())
                .param("violations", outcome.violations()).param("passing", outcome.passing())
                .param("score", outcome.score()).param("outOf", outcome.outOf())
                .param("percentage", outcome.percentage()).param("answered", outcome.answered())
                .param("correct", outcome.correct()).param("incorrect", outcome.incorrect()).param("now", now)
                .param("actor", actor).update();
    }

    public int recordMarks(UUID id, int expected, Outcome outcome, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RECORD_MARKS).param("id", id).param("expected", expected).param("status", outcome.status())
                .param("score", outcome.score()).param("outOf", outcome.outOf())
                .param("percentage", outcome.percentage()).param("correct", outcome.correct())
                .param("incorrect", outcome.incorrect()).param("now", now).param("actor", actor).update();
    }

    public void insertViolation(UUID id, UUID submissionId, String eventType, String severity, OffsetDateTime at,
            OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_VIOLATION).param("id", id).param("submissionId", submissionId).param("eventType", eventType)
                .param("severity", severity).param("occurredAt", at).param("now", now).param("actor", actor).update();
    }

    /** The percentages of the student's marked attempts at this placement, oldest first. */
    public List<BigDecimal> scoredAttempts(UUID quizId, UUID userId, UUID contentItemId) {
        return jdbc.sql(SCORED_ATTEMPTS).param("quizId", quizId).param("userId", userId)
                .param("contentItemId", contentItemId).query(BigDecimal.class).list();
    }

    private <T> Map<UUID, List<T>> grouped(String sql, Collection<UUID> ids, Row<T> row) {
        Map<UUID, List<T>> grouped = new LinkedHashMap<>();
        jdbc.sql(sql).param("ids", ids).query(rs -> {
            grouped.computeIfAbsent(rs.getObject("question_id", UUID.class), k -> new ArrayList<>()).add(row.map(rs));
        });
        return grouped;
    }

    @FunctionalInterface
    private interface Row<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private static Attempt attempt(ResultSet rs, int n) throws SQLException {
        return new Attempt(rs.getObject("id", UUID.class), rs.getObject("quiz_id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getObject("content_item_id", UUID.class),
                rs.getObject("cohort_id", UUID.class), rs.getInt("attempt_number"), rs.getString("status"),
                rs.getObject("started_at", OffsetDateTime.class), rs.getObject("resumed_at", OffsetDateTime.class),
                (Integer) rs.getObject("time_taken_seconds"), (Long) rs.getObject("last_client_sequence"),
                rs.getInt("violation_count"), rs.getInt("row_version"), rs.getObject("institution_id", UUID.class),
                (Integer) rs.getObject("duration_minutes"), rs.getBoolean("allow_pause"),
                (Integer) rs.getObject("max_violations"), rs.getBoolean("negative_marking_enabled"),
                rs.getBigDecimal("negative_marks"), rs.getInt("passing_percentage"), rs.getString("attempt_scoring"),
                rs.getBoolean("show_answers"), rs.getObject("due_at", OffsetDateTime.class),
                rs.getObject("closes_at", OffsetDateTime.class), (Integer) rs.getObject("extra_time_minutes"));
    }

    static Map<UUID, BigDecimal> marks(List<QuizQuestionMarks> questions) {
        return questions.stream().collect(Collectors.toMap(QuizQuestionMarks::questionId, QuizQuestionMarks::marks,
                (a, b) -> a, LinkedHashMap::new));
    }
}
