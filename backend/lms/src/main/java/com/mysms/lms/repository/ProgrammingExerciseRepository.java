package com.mysms.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.ProgrammingExerciseEntity;
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
 * Plain-JDBC access to lms.programming_exercise: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ProgrammingExerciseRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("problemStatement", "r.problem_statement", ValueType.STRING),
            field("language", "r.language", ValueType.STRING),
            field("starterCode", "r.starter_code", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ProgrammingExerciseEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ProgrammingExerciseEntity::getCreatedAt),
            Map.entry("updateTime", ProgrammingExerciseEntity::getUpdatedAt),
            Map.entry("title", ProgrammingExerciseEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.title, r.problem_statement, r.language, r.starter_code, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.programming_exercise r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.programming_exercise
            (id, tenant_id, title, problem_statement, language, starter_code, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :title, :problemStatement, :language, :starterCode, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, title, problem_statement, language, starter_code, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.programming_exercise
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.programming_exercise
               SET title = :title,
                   problem_statement = :problemStatement,
                   language = :language,
                   starter_code = :starterCode,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_CONTENT_AUTHOR_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_author WHERE tenant_id = :tenantId
                AND exercise_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND exercise_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXERCISE_SUBMISSION_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.exercise_submission WHERE tenant_id = :tenantId
                AND exercise_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXERCISE_TEST_CASE_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.exercise_test_case WHERE tenant_id = :tenantId
                AND exercise_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_ACTIVITY_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson_activity WHERE tenant_id = :tenantId
                AND exercise_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TIMETABLE_ENTRY_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_entry WHERE tenant_id = :tenantId
                AND exercise_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public ProgrammingExerciseRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ProgrammingExerciseEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ProgrammingExerciseRepository::map).optional();
    }

    public Page<ProgrammingExerciseEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ProgrammingExerciseEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ProgrammingExerciseRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ProgrammingExerciseEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ProgrammingExerciseEntity insert(ProgrammingExerciseEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("title", e.getTitle())
                .param("problemStatement", e.getProblemStatement())
                .param("language", e.getLanguage())
                .param("starterCode", e.getStarterCode())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ProgrammingExerciseRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ProgrammingExerciseEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("title", e.getTitle())
                .param("problemStatement", e.getProblemStatement())
                .param("language", e.getLanguage())
                .param("starterCode", e.getStarterCode())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ProgrammingExerciseEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live lms.content_author rows still reference the row. */
    public boolean referencedByContentAuthorExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_AUTHOR_EXERCISE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_EXERCISE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.exercise_submission rows still reference the row. */
    public boolean referencedByExerciseSubmissionExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXERCISE_SUBMISSION_EXERCISE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.exercise_test_case rows still reference the row. */
    public boolean referencedByExerciseTestCaseExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXERCISE_TEST_CASE_EXERCISE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.lesson_activity rows still reference the row. */
    public boolean referencedByLessonActivityExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_ACTIVITY_EXERCISE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.timetable_entry rows still reference the row. */
    public boolean referencedByTimetableEntryExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TIMETABLE_ENTRY_EXERCISE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, ProgrammingExerciseEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ProgrammingExerciseEntity map(ResultSet rs, int row) throws SQLException {
        return ProgrammingExerciseEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .title(rs.getString("title"))
                .problemStatement(rs.getString("problem_statement"))
                .language(rs.getString("language"))
                .starterCode(rs.getString("starter_code"))
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
