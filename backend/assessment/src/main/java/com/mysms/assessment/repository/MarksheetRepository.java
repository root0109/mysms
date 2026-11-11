package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.MarksheetEntity;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to assessment.marksheet (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class MarksheetRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("publishedByUserId", "r.published_by_user_id", ValueType.UUID),
            field("visibleToGuardians", "r.visible_to_guardians", ValueType.BOOLEAN),
            field("examSessionId", "r.exam_session_id", ValueType.UUID),
            field("resultTemplateId", "r.result_template_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("generatedOn", "r.generated_on", ValueType.DATE),
            field("generatedByUserId", "r.generated_by_user_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("totalPassed", "r.total_passed", ValueType.INTEGER),
            field("totalFailed", "r.total_failed", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<MarksheetEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", MarksheetEntity::getCreatedAt),
            Map.entry("updateTime", MarksheetEntity::getUpdatedAt),
            Map.entry("name", MarksheetEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.published_at, r.published_by_user_id, r.visible_to_guardians,
                   r.exam_session_id, r.result_template_id, r.name, r.generated_on, r.generated_by_user_id, r.status,
                   r.total_passed, r.total_failed, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM assessment.marksheet r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String HAS_COMPLETE_RESULTS = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet_line ml
                            WHERE ml.tenant_id = :tenantId AND ml.marksheet_id = m.id AND ml.deleted_at IS NULL)
               AND NOT EXISTS (
                    SELECT 1 FROM assessment.exam_result er
                    JOIN assessment.exam e ON e.tenant_id = er.tenant_id AND e.tenant_id = :tenantId
                                        AND e.id = er.exam_id
                    WHERE er.tenant_id = :tenantId AND e.exam_session_id = m.exam_session_id
                      AND er.deleted_at IS NULL
                      AND (er.result_status IS NULL OR er.result_status = 'pending' OR er.marksheet_line_id IS NULL
                           OR NOT EXISTS (SELECT 1 FROM assessment.marksheet_line ml
                                          WHERE ml.tenant_id = :tenantId AND ml.id = er.marksheet_line_id
                                            AND ml.marksheet_id = m.id AND ml.student_id = er.student_id
                                            AND ml.deleted_at IS NULL)))
               AND NOT EXISTS (SELECT 1 FROM assessment.exam e WHERE e.tenant_id = :tenantId
                    AND e.exam_session_id = m.exam_session_id AND e.deleted_at IS NULL
                    AND e.status NOT IN ('result_updated', 'done'))
              FROM assessment.marksheet m WHERE m.tenant_id = :tenantId AND m.id = :id
                AND m.deleted_at IS NULL
            """;
    private static final String INSERT = """
            INSERT INTO assessment.marksheet
            (id, tenant_id, visible_to_guardians, exam_session_id, result_template_id, name, generated_on,
             generated_by_user_id, total_passed, total_failed, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, COALESCE(:visibleToGuardians, false), :examSessionId, :resultTemplateId, :name,
                    :generatedOn, :generatedByUserId, :totalPassed, :totalFailed, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, published_at, published_by_user_id, visible_to_guardians, exam_session_id,
                      result_template_id, name, generated_on, generated_by_user_id, status, total_passed, total_failed,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.marksheet
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.marksheet
               SET visible_to_guardians = :visibleToGuardians,
                   exam_session_id = :examSessionId,
                   result_template_id = :resultTemplateId,
                   name = :name,
                   generated_on = :generatedOn,
                   generated_by_user_id = :generatedByUserId,
                   total_passed = :totalPassed,
                   total_failed = :totalFailed,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_EXAM_SESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_session WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_RESULT_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.result_template WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_MARKSHEET_LINE_MARKSHEET_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet_line WHERE tenant_id = :tenantId
                AND marksheet_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.exam_session p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** assessment.CancelMarksheet: allowed while status is draft or validated. */
    private static final String TRANSITION_CANCEL_MARKSHEET = """
            UPDATE assessment.marksheet
               SET status = 'cancelled',
                   published_at = NULL,
                   published_by_user_id = NULL,
                   visible_to_guardians = false,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.ResetMarksheet: allowed while status is cancelled. */
    private static final String TRANSITION_RESET_MARKSHEET = """
            UPDATE assessment.marksheet
               SET status = 'draft',
                   published_at = NULL,
                   published_by_user_id = NULL,
                   visible_to_guardians = false,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String TRANSITION_VALIDATE_MARKSHEET = """
            UPDATE assessment.marksheet SET status = 'validated', updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND status = 'draft'
               AND deleted_at IS NULL
            """;
    private static final String TRANSITION_PUBLISH_MARKSHEET = """
            UPDATE assessment.marksheet SET published_at = :now, published_by_user_id = :actor,
                   visible_to_guardians = :visibleToGuardians, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND status = 'validated'
               AND published_at IS NULL AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.exam_session_id IN (SELECT p0.id FROM assessment.exam_session p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public MarksheetRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<MarksheetEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(MarksheetRepository::map).optional();
    }

    public boolean hasCompleteResults(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(HAS_COMPLETE_RESULTS).param("id", id).query(Boolean.class).single());
    }

    public Page<MarksheetEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<MarksheetEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(MarksheetRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), MarksheetEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public MarksheetEntity insert(MarksheetEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("visibleToGuardians", e.getVisibleToGuardians())
                .param("examSessionId", e.getExamSessionId())
                .param("resultTemplateId", e.getResultTemplateId())
                .param("name", e.getName())
                .param("generatedOn", e.getGeneratedOn())
                .param("generatedByUserId", e.getGeneratedByUserId())
                .param("totalPassed", e.getTotalPassed())
                .param("totalFailed", e.getTotalFailed())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(MarksheetRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(MarksheetEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("visibleToGuardians", e.getVisibleToGuardians())
                .param("examSessionId", e.getExamSessionId())
                .param("resultTemplateId", e.getResultTemplateId())
                .param("name", e.getName())
                .param("generatedOn", e.getGeneratedOn())
                .param("generatedByUserId", e.getGeneratedByUserId())
                .param("totalPassed", e.getTotalPassed())
                .param("totalFailed", e.getTotalFailed())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(MarksheetEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced assessment.exam_session row exists (live). */
    public boolean existsExamSessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXAM_SESSION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced assessment.result_template row exists (live). */
    public boolean existsResultTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RESULT_TEMPLATE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live assessment.marksheet_line rows still reference the row. */
    public boolean referencedByMarksheetLineMarksheetId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_MARKSHEET_LINE_MARKSHEET_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through exam_session_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** assessment.CancelMarksheet; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelMarksheet(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_MARKSHEET)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.ResetMarksheet; 0 rows means the row changed (or vanished) since it was read. */
    public int resetMarksheet(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESET_MARKSHEET)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    public int validateMarksheet(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_VALIDATE_MARKSHEET).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    public int publishMarksheet(UUID id, int expected, OffsetDateTime now, UUID actor, boolean visibleToGuardians) {
        return jdbc.sql(TRANSITION_PUBLISH_MARKSHEET).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).param("visibleToGuardians", visibleToGuardians).update();
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, MarksheetEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static MarksheetEntity map(ResultSet rs, int row) throws SQLException {
        return MarksheetEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .publishedByUserId(rs.getObject("published_by_user_id", UUID.class))
                .visibleToGuardians(rs.getObject("visible_to_guardians", Boolean.class))
                .examSessionId(rs.getObject("exam_session_id", UUID.class))
                .resultTemplateId(rs.getObject("result_template_id", UUID.class))
                .name(rs.getString("name"))
                .generatedOn(rs.getObject("generated_on", LocalDate.class))
                .generatedByUserId(rs.getObject("generated_by_user_id", UUID.class))
                .status(rs.getString("status"))
                .totalPassed(rs.getObject("total_passed", Integer.class))
                .totalFailed(rs.getObject("total_failed", Integer.class))
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
