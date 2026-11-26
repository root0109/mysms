package com.mysms.academics.assessment.repository;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.domain.MarksheetLineEntity;
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
import java.util.Collection;
import java.util.HashMap;
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
 * Plain-JDBC access to assessment.marksheet_line: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class MarksheetLineRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("creditsAttempted", "r.credits_attempted", ValueType.DECIMAL),
            field("creditsEarned", "r.credits_earned", ValueType.DECIMAL),
            field("gradePoints", "r.grade_points", ValueType.DECIMAL),
            field("sgpa", "r.sgpa", ValueType.DECIMAL),
            field("cgpa", "r.cgpa", ValueType.DECIMAL),
            field("marksheetId", "r.marksheet_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("totalMarks", "r.total_marks", ValueType.DECIMAL),
            field("percentage", "r.percentage", ValueType.DECIMAL),
            field("gradeLabel", "r.grade_label", ValueType.STRING),
            field("resultStatus", "r.result_status", ValueType.STRING),
            field("generatedOn", "r.generated_on", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<MarksheetLineEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", MarksheetLineEntity::getCreatedAt),
            Map.entry("updateTime", MarksheetLineEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.credits_attempted, r.credits_earned, r.grade_points, r.sgpa, r.cgpa,
                   r.marksheet_id, r.student_id, r.total_marks, r.percentage, r.grade_label, r.result_status,
                   r.generated_on, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.marksheet_line r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT_GENERATED = """
            INSERT INTO assessment.marksheet_line
                (id, tenant_id, marksheet_id, student_id, total_marks, percentage, grade_label, result_status,
                 generated_on, credits_attempted, credits_earned, grade_points, sgpa, cgpa,
                 created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :marksheetId, :studentId, :totalMarks, :percentage, :gradeLabel, :resultStatus,
                    :generatedOn, :creditsAttempted, :creditsEarned, :gradePoints, :sgpa, :cgpa,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, credits_attempted, credits_earned, grade_points, sgpa, cgpa, marksheet_id,
                      student_id, total_marks, percentage, grade_label, result_status, generated_on, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String EXAM_SESSIONS_OF_MARKSHEETS = """
            SELECT m.id, m.exam_session_id FROM assessment.marksheet m
             WHERE m.tenant_id = :tenantId AND m.id = ANY(:ids)
            """;
    private static final String STUDENT_RESULTS = """
            SELECT r.id, r.tenant_id, r.credits_attempted, r.credits_earned, r.grade_points, r.sgpa, r.cgpa,
                   r.marksheet_id, r.student_id, r.total_marks, r.percentage, r.grade_label, r.result_status,
                   r.generated_on, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM assessment.marksheet_line r
              JOIN assessment.marksheet m ON m.tenant_id = r.tenant_id AND m.tenant_id = :tenantId
                   AND m.id = r.marksheet_id
              JOIN assessment.exam_session s ON s.tenant_id = m.tenant_id AND s.tenant_id = :tenantId
                   AND s.id = m.exam_session_id
             WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL AND m.deleted_at IS NULL
               AND m.status = 'validated' AND r.student_id = :studentId
               AND (NOT :requirePublished OR m.published_at IS NOT NULL)
               AND (NOT :guardians OR m.visible_to_guardians)
               AND (CAST(:academicYearId AS uuid) IS NULL OR s.academic_year_id = :academicYearId)
               AND (CAST(:examSessionId AS uuid) IS NULL OR m.exam_session_id = :examSessionId)
            """;
    /** The student has validated results, and none of them in the given reach (staff outside the student's school). */
    private static final String STUDENT_RESULTS_OUT_OF_REACH = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet_line r
                             JOIN assessment.marksheet m ON m.tenant_id = :tenantId AND m.id = r.marksheet_id
                            WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL AND m.deleted_at IS NULL
                              AND m.status = 'validated' AND r.student_id = :studentId)
               AND NOT EXISTS (SELECT 1 FROM assessment.marksheet_line r
                             JOIN assessment.marksheet m ON m.tenant_id = :tenantId AND m.id = r.marksheet_id
                             JOIN assessment.exam_session s ON s.tenant_id = :tenantId AND s.id = m.exam_session_id
                            WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL AND m.deleted_at IS NULL
                              AND m.status = 'validated' AND r.student_id = :studentId
                              AND (:reachAll OR s.institution_id = ANY(:reachIds)))
            """;
    private static final String SESSION_REACH = "(:reachAll OR s.institution_id = ANY(:reachIds))";
    private static final String CUMULATIVE_GPA = """
            SELECT COALESCE(SUM(r.grade_points), 0) AS grade_points,
                   COALESCE(SUM(r.credits_attempted), 0) AS credits_attempted
              FROM assessment.marksheet_line r
              JOIN assessment.marksheet m ON m.tenant_id = r.tenant_id AND m.tenant_id = :tenantId
                   AND m.id = r.marksheet_id
             WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL AND m.deleted_at IS NULL
               AND r.student_id = :studentId AND m.status = 'validated'
               AND m.exam_session_id <> :examSessionId AND m.generated_on <= :generatedOn
               AND r.grade_points IS NOT NULL AND r.credits_attempted > 0
            """;
    private static final String INSERT = """
            INSERT INTO assessment.marksheet_line
            (id, tenant_id, marksheet_id, student_id, generated_on, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :marksheetId, :studentId, :generatedOn, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, credits_attempted, credits_earned, grade_points, sgpa, cgpa, marksheet_id,
                      student_id, total_marks, percentage, grade_label, result_status, generated_on, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.marksheet_line
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.marksheet_line
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_MARKSHEET_LINE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet_line
                            WHERE tenant_id = :tenantId
                              AND marksheet_id = :marksheetId AND student_id = :studentId AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_MARKSHEET_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXAM_RESULT_MARKSHEET_LINE_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_result WHERE tenant_id = :tenantId
                AND marksheet_line_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM assessment.marksheet p0 JOIN assessment.exam_session p1 ON p1.tenant_id =
                :tenantId AND p1.id =
            p0.exam_session_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.marksheet_id IN (SELECT p0.id FROM assessment.marksheet p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.exam_session_id IN
             (SELECT p1.id FROM assessment.exam_session p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds))))))
            """;

    private final JdbcClient jdbc;

    public MarksheetLineRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<MarksheetLineEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(MarksheetLineRepository::map).optional();
    }

    public Page<MarksheetLineEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<MarksheetLineEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(MarksheetLineRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), MarksheetLineEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public MarksheetLineEntity insert(MarksheetLineEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("marksheetId", e.getMarksheetId())
                .param("studentId", e.getStudentId())
                .param("generatedOn", e.getGeneratedOn())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(MarksheetLineRepository::map).single();
    }

    public MarksheetLineEntity insertGenerated(MarksheetLineEntity e) {
        return jdbc.sql(INSERT_GENERATED).param("id", e.getId()).param("tenantId", e.getTenantId())
                .param("marksheetId", e.getMarksheetId()).param("studentId", e.getStudentId())
                .param("totalMarks", e.getTotalMarks()).param("percentage", e.getPercentage())
                .param("gradeLabel", e.getGradeLabel()).param("resultStatus", e.getResultStatus())
                .param("generatedOn", e.getGeneratedOn()).param("creditsAttempted", e.getCreditsAttempted())
                .param("creditsEarned", e.getCreditsEarned()).param("gradePoints", e.getGradePoints())
                .param("sgpa", e.getSgpa()).param("cgpa", e.getCgpa()).param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy()).param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy()).param("rowVersion", e.getRowVersion())
                .query(MarksheetLineRepository::map).single();
    }

    public record GpaTotals(java.math.BigDecimal gradePoints, java.math.BigDecimal creditsAttempted) { }

    public GpaTotals cumulativeGpaTotals(UUID studentId, UUID examSessionId, LocalDate generatedOn) {
        return jdbc.sql(CUMULATIVE_GPA).param("studentId", studentId).param("examSessionId", examSessionId)
                .param("generatedOn", generatedOn).query((rs, row) -> new GpaTotals(
                        rs.getBigDecimal("grade_points"), rs.getBigDecimal("credits_attempted"))).single();
    }

    /** The exam session of each of the given marksheets. */
    public Map<UUID, UUID> examSessionsOfMarksheets(Collection<UUID> marksheetIds) {
        Map<UUID, UUID> sessions = new HashMap<>();
        jdbc.sql(EXAM_SESSIONS_OF_MARKSHEETS).param("ids", marksheetIds.toArray(UUID[]::new))
                .query((rs, row) -> sessions.put(rs.getObject("id", UUID.class),
                        rs.getObject("exam_session_id", UUID.class))).list();
        return sessions;
    }

    /** The validated marksheet lines of a student (published ones only, or only those shared with guardians). */
    public Page<MarksheetLineEntity> listStudentResults(UUID studentId, UUID academicYearId, UUID examSessionId,
            boolean requirePublished, boolean guardian, PageRequest page, SqlFragment extraScope) {
        SqlFragment where = extraScope.and(page.keysetPredicate());
        String sql = STUDENT_RESULTS + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause()
                + page.limitClause();
        Map<String, Object> params = new HashMap<>();
        params.put("studentId", studentId);
        params.put("academicYearId", academicYearId);
        params.put("examSessionId", examSessionId);
        params.put("requirePublished", requirePublished);
        params.put("guardians", guardian);
        params.putAll(where.params());
        params.putAll(page.limitParams());
        List<MarksheetLineEntity> rows = jdbc.sql(sql).params(params).query(MarksheetLineRepository::map).list();
        return page.toPage(rows, line -> sortValues(page.sort(), line), MarksheetLineEntity::getId);
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(MarksheetLineEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(MarksheetLineEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_marksheet_line_1: marksheet_id, student_id where (deleted_at IS NULL). */
    public boolean takenMarksheetIdStudentId1(MarksheetLineEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_MARKSHEET_LINE_1_TAKEN)
                .param("marksheetId", e.getMarksheetId())
                .param("studentId", e.getStudentId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.marksheet row exists (live). */
    public boolean existsMarksheetId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_MARKSHEET_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live assessment.exam_result rows still reference the row. */
    public boolean referencedByExamResultMarksheetLineId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXAM_RESULT_MARKSHEET_LINE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through marksheet_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Whether the student has validated results, none of them in the reach (so, to that caller, no results). */
    public boolean studentResultsOutOfReach(UUID studentId, DataScope reach) {
        return Boolean.TRUE.equals(jdbc.sql(STUDENT_RESULTS_OUT_OF_REACH).param("studentId", studentId)
                .param("reachAll", reach.all()).param("reachIds", reach.institutionArray())
                .query(Boolean.class).single());
    }

    /** For {@link #listStudentResults}: the results of exam sessions of the reach's institutions only. */
    public static SqlFragment sessionReach(DataScope reach) {
        return new SqlFragment(SESSION_REACH, Map.of("reachAll", reach.all(), "reachIds", reach.institutionArray()));
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, MarksheetLineEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static MarksheetLineEntity map(ResultSet rs, int row) throws SQLException {
        return MarksheetLineEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .creditsAttempted(rs.getBigDecimal("credits_attempted"))
                .creditsEarned(rs.getBigDecimal("credits_earned"))
                .gradePoints(rs.getBigDecimal("grade_points"))
                .sgpa(rs.getBigDecimal("sgpa"))
                .cgpa(rs.getBigDecimal("cgpa"))
                .marksheetId(rs.getObject("marksheet_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .totalMarks(rs.getBigDecimal("total_marks"))
                .percentage(rs.getBigDecimal("percentage"))
                .gradeLabel(rs.getString("grade_label"))
                .resultStatus(rs.getString("result_status"))
                .generatedOn(rs.getObject("generated_on", LocalDate.class))
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
