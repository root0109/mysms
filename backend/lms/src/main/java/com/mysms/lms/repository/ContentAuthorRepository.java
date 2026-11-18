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
import com.mysms.lms.domain.ContentAuthorEntity;
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
 * Plain-JDBC access to lms.content_author: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ContentAuthorRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("quizId", "r.quiz_id", ValueType.UUID),
            field("questionId", "r.question_id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("exerciseId", "r.exercise_id", ValueType.UUID),
            field("learningProgramId", "r.learning_program_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ContentAuthorEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ContentAuthorEntity::getCreatedAt),
            Map.entry("updateTime", ContentAuthorEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.quiz_id, r.question_id, r.assignment_id, r.exercise_id,
                   r.learning_program_id, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.content_author r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.content_author
            (id, tenant_id, user_id, quiz_id, question_id, assignment_id, exercise_id, learning_program_id,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :quizId, :questionId, :assignmentId, :exerciseId, :learningProgramId,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, quiz_id, question_id, assignment_id, exercise_id, learning_program_id,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.content_author
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.content_author
               SET user_id = :userId,
                   quiz_id = :quizId,
                   question_id = :questionId,
                   assignment_id = :assignmentId,
                   exercise_id = :exerciseId,
                   learning_program_id = :learningProgramId,
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
    private static final String EXISTS_LEARNING_PROGRAM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.learning_program WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.quiz p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.quiz_id IN (SELECT p0.id FROM lms.quiz p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.quiz_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public ContentAuthorRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ContentAuthorEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ContentAuthorRepository::map).optional();
    }

    public Page<ContentAuthorEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ContentAuthorEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ContentAuthorRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ContentAuthorEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ContentAuthorEntity insert(ContentAuthorEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("quizId", e.getQuizId())
                .param("questionId", e.getQuestionId())
                .param("assignmentId", e.getAssignmentId())
                .param("exerciseId", e.getExerciseId())
                .param("learningProgramId", e.getLearningProgramId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ContentAuthorRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ContentAuthorEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("quizId", e.getQuizId())
                .param("questionId", e.getQuestionId())
                .param("assignmentId", e.getAssignmentId())
                .param("exerciseId", e.getExerciseId())
                .param("learningProgramId", e.getLearningProgramId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ContentAuthorEntity e, int expectedVersion) {
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

    /** Whether the referenced lms.learning_program row exists (live). */
    public boolean existsLearningProgramId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LEARNING_PROGRAM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.question row exists (live). */
    public boolean existsQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUESTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz row exists (live). */
    public boolean existsQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUIZ_ID).param("id", id).query(Boolean.class).single());
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

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ContentAuthorEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ContentAuthorEntity map(ResultSet rs, int row) throws SQLException {
        return ContentAuthorEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .quizId(rs.getObject("quiz_id", UUID.class))
                .questionId(rs.getObject("question_id", UUID.class))
                .assignmentId(rs.getObject("assignment_id", UUID.class))
                .exerciseId(rs.getObject("exercise_id", UUID.class))
                .learningProgramId(rs.getObject("learning_program_id", UUID.class))
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
