package com.mysms.learning.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.ExerciseSubmissionEntity;
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
 * Plain-JDBC access to lms.exercise_submission: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ExerciseSubmissionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("exerciseId", "r.exercise_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("isFullCode", "r.is_full_code", ValueType.BOOLEAN),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ExerciseSubmissionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ExerciseSubmissionEntity::getCreatedAt),
            Map.entry("updateTime", ExerciseSubmissionEntity::getUpdatedAt),
            Map.entry("code", ExerciseSubmissionEntity::getCode));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.exercise_id, r.user_id, r.code, r.is_full_code, r.status, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.exercise_submission r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.exercise_submission
            (id, tenant_id, exercise_id, user_id, code, is_full_code, status, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :exerciseId, :userId, :code, COALESCE(:isFullCode, false), :status, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, exercise_id, user_id, code, is_full_code, status, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.exercise_submission
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.exercise_submission
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.programming_exercise WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXERCISE_TEST_RESULT_EXERCISE_SUBMISSION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.exercise_test_result WHERE tenant_id = :tenantId
                AND exercise_submission_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public ExerciseSubmissionRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ExerciseSubmissionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ExerciseSubmissionRepository::map).optional();
    }

    public Page<ExerciseSubmissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ExerciseSubmissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ExerciseSubmissionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ExerciseSubmissionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ExerciseSubmissionEntity insert(ExerciseSubmissionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("exerciseId", e.getExerciseId())
                .param("userId", e.getUserId())
                .param("code", e.getCode())
                .param("isFullCode", e.getIsFullCode())
                .param("status", e.getStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ExerciseSubmissionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ExerciseSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ExerciseSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.programming_exercise row exists (live). */
    public boolean existsExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXERCISE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.exercise_test_result rows still reference the row. */
    public boolean referencedByExerciseTestResultExerciseSubmissionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXERCISE_TEST_RESULT_EXERCISE_SUBMISSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, ExerciseSubmissionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ExerciseSubmissionEntity map(ResultSet rs, int row) throws SQLException {
        return ExerciseSubmissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .exerciseId(rs.getObject("exercise_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .code(rs.getString("code"))
                .isFullCode(rs.getObject("is_full_code", Boolean.class))
                .status(rs.getString("status"))
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
