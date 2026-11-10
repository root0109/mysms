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
import com.mysms.lms.domain.QuizAnswerEntity;
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
 * Plain-JDBC access to lms.quiz_answer (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class QuizAnswerRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("quizSubmissionId", "r.quiz_submission_id", ValueType.UUID),
            field("questionId", "r.question_id", ValueType.UUID),
            field("questionText", "r.question_text", ValueType.STRING),
            field("questionType", "r.question_type", ValueType.STRING),
            field("answer", "r.answer", ValueType.STRING),
            field("isCorrect", "r.is_correct", ValueType.BOOLEAN),
            field("marksAwarded", "r.marks_awarded", ValueType.DECIMAL),
            field("marksPossible", "r.marks_possible", ValueType.DECIMAL),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("feedback", "r.feedback", ValueType.STRING),
            field("gradedBy", "r.graded_by", ValueType.UUID),
            field("learningObjectiveId", "r.learning_objective_id", ValueType.UUID),
            field("timeSpentMs", "r.time_spent_ms", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<QuizAnswerEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", QuizAnswerEntity::getCreatedAt),
            Map.entry("updateTime", QuizAnswerEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.quiz_submission_id, r.question_id, r.question_text, r.question_type, r.answer,
                   r.is_correct, r.marks_awarded, r.marks_possible, r.sort_order, r.feedback, r.graded_by,
                   r.learning_objective_id, r.time_spent_ms, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM lms.quiz_answer r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.quiz_answer
            (id, tenant_id, quiz_submission_id, question_id, question_text, question_type, answer, is_correct,
             marks_awarded, marks_possible, sort_order, feedback, graded_by, learning_objective_id, time_spent_ms,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :quizSubmissionId, :questionId, :questionText, :questionType, :answer,
                    :isCorrect, :marksAwarded, :marksPossible, :sortOrder, :feedback, :gradedBy,
                    :learningObjectiveId, :timeSpentMs, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, quiz_submission_id, question_id, question_text, question_type, answer, is_correct,
                      marks_awarded, marks_possible, sort_order, feedback, graded_by, learning_objective_id,
                      time_spent_ms, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.quiz_answer
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.quiz_answer
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_LEARNING_OBJECTIVE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.learning_objective WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUIZ_SUBMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.quiz_submission p0 JOIN lms.quiz p1 ON p1.tenant_id = :tenantId AND p1.id
                = p0.quiz_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.quiz_submission_id IN (SELECT p0.id FROM lms.quiz_submission p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.quiz_id IN
             (SELECT p1.id FROM lms.quiz p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public QuizAnswerRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuizAnswerEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(QuizAnswerRepository::map).optional();
    }

    public Page<QuizAnswerEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<QuizAnswerEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(QuizAnswerRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), QuizAnswerEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public QuizAnswerEntity insert(QuizAnswerEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("quizSubmissionId", e.getQuizSubmissionId())
                .param("questionId", e.getQuestionId())
                .param("questionText", e.getQuestionText())
                .param("questionType", e.getQuestionType())
                .param("answer", e.getAnswer())
                .param("isCorrect", e.getIsCorrect())
                .param("marksAwarded", e.getMarksAwarded())
                .param("marksPossible", e.getMarksPossible())
                .param("sortOrder", e.getSortOrder())
                .param("feedback", e.getFeedback())
                .param("gradedBy", e.getGradedBy())
                .param("learningObjectiveId", e.getLearningObjectiveId())
                .param("timeSpentMs", e.getTimeSpentMs())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(QuizAnswerRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(QuizAnswerEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(QuizAnswerEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.learning_objective row exists (live). */
    public boolean existsLearningObjectiveId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LEARNING_OBJECTIVE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced lms.question row exists (live). */
    public boolean existsQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUESTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz_submission row exists (live). */
    public boolean existsQuizSubmissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUIZ_SUBMISSION_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through quiz_submission_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.quiz_submission_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, QuizAnswerEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static QuizAnswerEntity map(ResultSet rs, int row) throws SQLException {
        return QuizAnswerEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .quizSubmissionId(rs.getObject("quiz_submission_id", UUID.class))
                .questionId(rs.getObject("question_id", UUID.class))
                .questionText(rs.getString("question_text"))
                .questionType(rs.getString("question_type"))
                .answer(rs.getString("answer"))
                .isCorrect(rs.getObject("is_correct", Boolean.class))
                .marksAwarded(rs.getBigDecimal("marks_awarded"))
                .marksPossible(rs.getBigDecimal("marks_possible"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .feedback(rs.getString("feedback"))
                .gradedBy(rs.getObject("graded_by", UUID.class))
                .learningObjectiveId(rs.getObject("learning_objective_id", UUID.class))
                .timeSpentMs(rs.getObject("time_spent_ms", Long.class))
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
