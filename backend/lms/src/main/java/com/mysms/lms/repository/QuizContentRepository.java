package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access for the quiz workflows on its question set (SetQuizQuestions, CheckQuizAnswer): the quiz locked
 * for the change, the questions and pools it draws from, and the guarded writes. Every statement filters the caller's
 * tenant; row-level security is the backstop.
 */
@Repository
public class QuizContentRepository {

    /** The quiz as its workflows need it. */
    public record QuizHead(UUID id, UUID institutionId, OffsetDateTime publishedAt, BigDecimal marksPerQuestion,
                           boolean showFeedback, boolean showAnswers, int rowVersion) {
    }

    /** One live question of the quiz. */
    public record Placed(UUID id, UUID questionId, BigDecimal marks, int sortOrder) {
    }

    private static final String HEAD = """
            SELECT id, institution_id, published_at, marks_per_question, show_feedback, show_answers, row_version
              FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String HEAD_FOR_UPDATE = HEAD + " FOR UPDATE";
    private static final String PLACED = """
            SELECT id, question_id, marks, sort_order FROM lms.quiz_question
             WHERE tenant_id = :tenantId AND quiz_id = :quizId AND deleted_at IS NULL ORDER BY sort_order, id
            """;
    private static final String QUESTION_MARKS = """
            SELECT id, marks FROM lms.question
             WHERE tenant_id = :tenantId AND id IN (:ids) AND deleted_at IS NULL AND is_retired = false
            """;
    private static final String POOL_QUESTIONS = """
            SELECT i.question_id FROM lms.question_pool_item i
              JOIN lms.question q ON q.id = i.question_id AND q.tenant_id = :tenantId AND q.deleted_at IS NULL
                   AND q.is_retired = false
             WHERE i.tenant_id = :tenantId AND i.question_pool_id = :poolId AND i.deleted_at IS NULL
             ORDER BY i.created_at, i.id
            """;
    private static final String POOL_EXISTS = """
            SELECT EXISTS (SELECT 1 FROM lms.question_pool WHERE tenant_id = :tenantId AND id = :id
                              AND deleted_at IS NULL)
            """;
    private static final String INSERT_PLACED = """
            INSERT INTO lms.quiz_question (id, tenant_id, quiz_id, question_id, marks, sort_order, created_at,
                   created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :quizId, :questionId, :marks, :sortOrder, :now, :actor, :now, :actor, 1)
            """;
    private static final String REMOVE_PLACED = """
            UPDATE lms.quiz_question SET deleted_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String INSERT_HISTORY = """
            INSERT INTO lms.quiz_question_history (id, tenant_id, quiz_id, question_id, marks, sort_order,
                   superseded_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :quizId, :questionId, :marks, :sortOrder, :now, :now, :actor, :now, :actor, 1)
            """;
    private static final String HAS_ATTEMPTS = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId AND quiz_id = :quizId
                              AND deleted_at IS NULL)
            """;
    private static final String SET_TOTAL = """
            UPDATE lms.quiz SET total_marks = :total, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String GRADED_PLACEMENT = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId AND quiz_id = :quizId
                              AND is_graded = true AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public QuizContentRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuizHead> head(UUID id) {
        return jdbc.sql(HEAD).param("id", id).query(QuizContentRepository::head).optional();
    }

    public Optional<QuizHead> lock(UUID id) {
        return jdbc.sql(HEAD_FOR_UPDATE).param("id", id).query(QuizContentRepository::head).optional();
    }

    public List<Placed> placed(UUID quizId) {
        return jdbc.sql(PLACED).param("quizId", quizId).query((rs, n) -> new Placed(rs.getObject("id", UUID.class),
                rs.getObject("question_id", UUID.class), rs.getBigDecimal("marks"), rs.getInt("sort_order"))).list();
    }

    /** The marks of the given live, unretired questions (a null value: the question sets none). */
    public Map<UUID, Optional<BigDecimal>> questionMarks(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql(QUESTION_MARKS).param("ids", ids).query((rs, n) -> Map.entry(rs.getObject("id", UUID.class),
                Optional.ofNullable(rs.getBigDecimal("marks")))).list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean poolExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(POOL_EXISTS).param("id", id).query(Boolean.class).single());
    }

    public List<UUID> poolQuestions(UUID poolId) {
        return jdbc.sql(POOL_QUESTIONS).param("poolId", poolId).query(UUID.class).list();
    }

    public void place(UUID id, UUID quizId, UUID questionId, BigDecimal marks, int sortOrder, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(INSERT_PLACED).param("id", id).param("quizId", quizId).param("questionId", questionId)
                .param("marks", marks).param("sortOrder", sortOrder).param("now", now).param("actor", actor).update();
    }

    public void remove(UUID id, OffsetDateTime now, UUID actor) {
        jdbc.sql(REMOVE_PLACED).param("id", id).param("now", now).param("actor", actor).update();
    }

    public void keepHistory(UUID id, UUID quizId, Placed placed, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_HISTORY).param("id", id).param("quizId", quizId).param("questionId", placed.questionId())
                .param("marks", placed.marks()).param("sortOrder", placed.sortOrder()).param("now", now)
                .param("actor", actor).update();
    }

    public boolean hasAttempts(UUID quizId) {
        return Boolean.TRUE.equals(jdbc.sql(HAS_ATTEMPTS).param("quizId", quizId).query(Boolean.class).single());
    }

    public int setTotal(UUID id, int expected, BigDecimal total, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_TOTAL).param("id", id).param("expected", expected).param("total", total)
                .param("now", now).param("actor", actor).update();
    }

    public boolean gradedPlacement(UUID quizId) {
        return Boolean.TRUE.equals(jdbc.sql(GRADED_PLACEMENT).param("quizId", quizId).query(Boolean.class).single());
    }

    private static QuizHead head(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new QuizHead(rs.getObject("id", UUID.class), rs.getObject("institution_id", UUID.class),
                rs.getObject("published_at", OffsetDateTime.class), rs.getBigDecimal("marks_per_question"),
                rs.getBoolean("show_feedback"), rs.getBoolean("show_answers"), rs.getInt("row_version"));
    }
}
