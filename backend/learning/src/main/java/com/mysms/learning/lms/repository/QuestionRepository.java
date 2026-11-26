package com.mysms.learning.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.QuestionEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
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
 * Plain-JDBC access to lms.question: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class QuestionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("version", "r.version", ValueType.INTEGER),
            field("supersededById", "r.superseded_by_id", ValueType.UUID),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("questionCode", "r.question_code", ValueType.STRING),
            field("questionText", "r.question_text", ValueType.STRING),
            field("questionType", "r.question_type", ValueType.STRING),
            field("allowsMultiple", "r.allows_multiple", ValueType.BOOLEAN),
            field("marks", "r.marks", ValueType.DECIMAL),
            field("title", "r.title", ValueType.STRING),
            field("shuffleOptions", "r.shuffle_options", ValueType.BOOLEAN),
            field("optionLabelStyle", "r.option_label_style", ValueType.STRING),
            field("maxSelections", "r.max_selections", ValueType.INTEGER),
            field("difficulty", "r.difficulty", ValueType.STRING),
            field("generalFeedback", "r.general_feedback", ValueType.STRING),
            field("correctFeedback", "r.correct_feedback", ValueType.STRING),
            field("incorrectFeedback", "r.incorrect_feedback", ValueType.STRING),
            field("modelAnswer", "r.model_answer", ValueType.STRING),
            field("trueFalseAnswer", "r.true_false_answer", ValueType.BOOLEAN),
            field("numericAnswer", "r.numeric_answer", ValueType.STRING),
            field("numericTolerance", "r.numeric_tolerance", ValueType.STRING),
            field("numericIsRange", "r.numeric_is_range", ValueType.BOOLEAN),
            field("caseSensitive", "r.case_sensitive", ValueType.BOOLEAN),
            field("allowSuperscript", "r.allow_superscript", ValueType.BOOLEAN),
            field("allowSubscript", "r.allow_subscript", ValueType.BOOLEAN),
            field("isRetired", "r.is_retired", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<QuestionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", QuestionEntity::getCreatedAt),
            Map.entry("updateTime", QuestionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.version, r.superseded_by_id, r.published_at, r.question_code, r.question_text,
                   r.question_type, r.allows_multiple, r.marks, r.title, r.shuffle_options, r.option_label_style,
                   r.max_selections, r.difficulty, r.general_feedback, r.correct_feedback, r.incorrect_feedback,
                   r.model_answer, r.true_false_answer, r.numeric_answer, r.numeric_tolerance, r.numeric_is_range,
                   r.case_sensitive, r.allow_superscript, r.allow_subscript, r.is_retired, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.question r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String FIND_BY_IDS = SELECT_LIVE + " AND r.id = ANY(:ids)";
    private static final String INSERT = """
            INSERT INTO lms.question
            (id, tenant_id, question_code, question_text, question_type, allows_multiple, marks, title,
             shuffle_options, option_label_style, max_selections, difficulty, general_feedback, correct_feedback,
             incorrect_feedback, model_answer, true_false_answer, numeric_answer, numeric_tolerance,
             numeric_is_range, case_sensitive, allow_superscript, allow_subscript, is_retired, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :questionCode, :questionText,
                    COALESCE(:questionType, 'choices'::character varying), COALESCE(:allowsMultiple, false), :marks,
                    :title, COALESCE(:shuffleOptions, false), :optionLabelStyle, :maxSelections, :difficulty,
                    :generalFeedback, :correctFeedback, :incorrectFeedback, :modelAnswer, :trueFalseAnswer,
                    :numericAnswer, :numericTolerance, COALESCE(:numericIsRange, false),
                    COALESCE(:caseSensitive, false), COALESCE(:allowSuperscript, false),
                    COALESCE(:allowSubscript, false), COALESCE(:isRetired, false), :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, version, superseded_by_id, published_at, question_code, question_text,
                      question_type, allows_multiple, marks, title, shuffle_options, option_label_style, max_selections,
                      difficulty, general_feedback, correct_feedback, incorrect_feedback, model_answer,
                      true_false_answer, numeric_answer, numeric_tolerance, numeric_is_range, case_sensitive,
                      allow_superscript, allow_subscript, is_retired, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.question
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.question
               SET question_code = :questionCode,
                   question_text = :questionText,
                   question_type = :questionType,
                   allows_multiple = :allowsMultiple,
                   marks = :marks,
                   title = :title,
                   shuffle_options = :shuffleOptions,
                   option_label_style = :optionLabelStyle,
                   max_selections = :maxSelections,
                   difficulty = :difficulty,
                   general_feedback = :generalFeedback,
                   correct_feedback = :correctFeedback,
                   incorrect_feedback = :incorrectFeedback,
                   model_answer = :modelAnswer,
                   true_false_answer = :trueFalseAnswer,
                   numeric_answer = :numericAnswer,
                   numeric_tolerance = :numericTolerance,
                   numeric_is_range = :numericIsRange,
                   case_sensitive = :caseSensitive,
                   allow_superscript = :allowSuperscript,
                   allow_subscript = :allowSubscript,
                   is_retired = :isRetired,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_AUTHOR_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_author WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question WHERE tenant_id = :tenantId
                AND superseded_by_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_ACCEPTED_ANSWER_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_accepted_answer WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_BLANK_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_blank WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_HOTSPOT_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_hotspot WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_MATCH_PAIR_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_match_pair WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_OPTION_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_option WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_PATTERN_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_pattern WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_POOL_ITEM_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_pool_item WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_ANSWER_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_answer WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_QUESTION_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_question WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_QUESTION_HISTORY_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_question_history WHERE tenant_id = :tenantId
                AND question_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public QuestionRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuestionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(QuestionRepository::map).optional();
    }

    /** The live rows among the ids in one statement, in no particular order; an id with no live row is absent. */
    public List<QuestionEntity> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(FIND_BY_IDS).param("ids", ids.stream().distinct().toArray(UUID[]::new))
                .query(QuestionRepository::map).list();
    }

    public Page<QuestionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<QuestionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(QuestionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), QuestionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public QuestionEntity insert(QuestionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("questionCode", e.getQuestionCode())
                .param("questionText", e.getQuestionText())
                .param("questionType", e.getQuestionType())
                .param("allowsMultiple", e.getAllowsMultiple())
                .param("marks", e.getMarks())
                .param("title", e.getTitle())
                .param("shuffleOptions", e.getShuffleOptions())
                .param("optionLabelStyle", e.getOptionLabelStyle())
                .param("maxSelections", e.getMaxSelections())
                .param("difficulty", e.getDifficulty())
                .param("generalFeedback", e.getGeneralFeedback())
                .param("correctFeedback", e.getCorrectFeedback())
                .param("incorrectFeedback", e.getIncorrectFeedback())
                .param("modelAnswer", e.getModelAnswer())
                .param("trueFalseAnswer", e.getTrueFalseAnswer())
                .param("numericAnswer", e.getNumericAnswer())
                .param("numericTolerance", e.getNumericTolerance())
                .param("numericIsRange", e.getNumericIsRange())
                .param("caseSensitive", e.getCaseSensitive())
                .param("allowSuperscript", e.getAllowSuperscript())
                .param("allowSubscript", e.getAllowSubscript())
                .param("isRetired", e.getIsRetired())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(QuestionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(QuestionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("questionCode", e.getQuestionCode())
                .param("questionText", e.getQuestionText())
                .param("questionType", e.getQuestionType())
                .param("allowsMultiple", e.getAllowsMultiple())
                .param("marks", e.getMarks())
                .param("title", e.getTitle())
                .param("shuffleOptions", e.getShuffleOptions())
                .param("optionLabelStyle", e.getOptionLabelStyle())
                .param("maxSelections", e.getMaxSelections())
                .param("difficulty", e.getDifficulty())
                .param("generalFeedback", e.getGeneralFeedback())
                .param("correctFeedback", e.getCorrectFeedback())
                .param("incorrectFeedback", e.getIncorrectFeedback())
                .param("modelAnswer", e.getModelAnswer())
                .param("trueFalseAnswer", e.getTrueFalseAnswer())
                .param("numericAnswer", e.getNumericAnswer())
                .param("numericTolerance", e.getNumericTolerance())
                .param("numericIsRange", e.getNumericIsRange())
                .param("caseSensitive", e.getCaseSensitive())
                .param("allowSuperscript", e.getAllowSuperscript())
                .param("allowSubscript", e.getAllowSubscript())
                .param("isRetired", e.getIsRetired())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(QuestionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.question row exists (live). */
    public boolean existsSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SUPERSEDED_BY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.content_author rows still reference the row. */
    public boolean referencedByContentAuthorQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_AUTHOR_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question rows still reference the row. */
    public boolean referencedByQuestionSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_SUPERSEDED_BY_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_accepted_answer rows still reference the row. */
    public boolean referencedByQuestionAcceptedAnswerQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_ACCEPTED_ANSWER_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_blank rows still reference the row. */
    public boolean referencedByQuestionBlankQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_BLANK_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_hotspot rows still reference the row. */
    public boolean referencedByQuestionHotspotQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_HOTSPOT_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_match_pair rows still reference the row. */
    public boolean referencedByQuestionMatchPairQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_MATCH_PAIR_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_option rows still reference the row. */
    public boolean referencedByQuestionOptionQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_OPTION_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_pattern rows still reference the row. */
    public boolean referencedByQuestionPatternQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_PATTERN_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.question_pool_item rows still reference the row. */
    public boolean referencedByQuestionPoolItemQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_POOL_ITEM_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_answer rows still reference the row. */
    public boolean referencedByQuizAnswerQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_ANSWER_QUESTION_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.quiz_question rows still reference the row. */
    public boolean referencedByQuizQuestionQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_QUESTION_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_question_history rows still reference the row. */
    public boolean referencedByQuizQuestionHistoryQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_QUESTION_HISTORY_QUESTION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, QuestionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static QuestionEntity map(ResultSet rs, int row) throws SQLException {
        return QuestionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .version(rs.getObject("version", Integer.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .questionCode(rs.getString("question_code"))
                .questionText(rs.getString("question_text"))
                .questionType(rs.getString("question_type"))
                .allowsMultiple(rs.getObject("allows_multiple", Boolean.class))
                .marks(rs.getBigDecimal("marks"))
                .title(rs.getString("title"))
                .shuffleOptions(rs.getObject("shuffle_options", Boolean.class))
                .optionLabelStyle(rs.getString("option_label_style"))
                .maxSelections(rs.getObject("max_selections", Integer.class))
                .difficulty(rs.getString("difficulty"))
                .generalFeedback(rs.getString("general_feedback"))
                .correctFeedback(rs.getString("correct_feedback"))
                .incorrectFeedback(rs.getString("incorrect_feedback"))
                .modelAnswer(rs.getString("model_answer"))
                .trueFalseAnswer(rs.getObject("true_false_answer", Boolean.class))
                .numericAnswer(rs.getString("numeric_answer"))
                .numericTolerance(rs.getString("numeric_tolerance"))
                .numericIsRange(rs.getObject("numeric_is_range", Boolean.class))
                .caseSensitive(rs.getObject("case_sensitive", Boolean.class))
                .allowSuperscript(rs.getObject("allow_superscript", Boolean.class))
                .allowSubscript(rs.getObject("allow_subscript", Boolean.class))
                .isRetired(rs.getObject("is_retired", Boolean.class))
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
