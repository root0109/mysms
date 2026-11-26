package com.mysms.academics.assessment.repository;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.domain.ExamSessionEntity;
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
 * Plain-JDBC access to assessment.exam_session: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ExamSessionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("academicTermId", "r.academic_term_id", ValueType.UUID),
            field("examTypeId", "r.exam_type_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("gradeLevelId", "r.grade_level_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("batchId", "r.batch_id", ValueType.UUID),
            field("evaluationType", "r.evaluation_type", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE),
            field("endDate", "r.end_date", ValueType.DATE),
            field("venue", "r.venue", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ExamSessionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ExamSessionEntity::getCreatedAt),
            Map.entry("updateTime", ExamSessionEntity::getUpdatedAt),
            Map.entry("code", ExamSessionEntity::getCode),
            Map.entry("name", ExamSessionEntity::getName),
            Map.entry("startDate", ExamSessionEntity::getStartDate));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.academic_term_id, r.exam_type_id,
                   r.code, r.name, r.grade_level_id, r.course_id, r.batch_id, r.evaluation_type, r.start_date,
                   r.end_date, r.venue, r.status, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM assessment.exam_session r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.exam_session
            (id, tenant_id, institution_id, academic_year_id, academic_term_id, exam_type_id, code, name,
             grade_level_id, course_id, batch_id, evaluation_type, start_date, end_date, venue, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :academicTermId, :examTypeId, :code, :name,
                    :gradeLevelId, :courseId, :batchId, COALESCE(:evaluationType, 'marks'::character varying),
                    :startDate, :endDate, :venue, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, academic_term_id, exam_type_id, code, name,
                      grade_level_id, course_id, batch_id, evaluation_type, start_date, end_date, venue, status,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.exam_session
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.exam_session
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   academic_term_id = :academicTermId,
                   exam_type_id = :examTypeId,
                   code = :code,
                   name = :name,
                   grade_level_id = :gradeLevelId,
                   course_id = :courseId,
                   batch_id = :batchId,
                   evaluation_type = :evaluationType,
                   start_date = :startDate,
                   end_date = :endDate,
                   venue = :venue,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_EXAM_SESSION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_session
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_EXAM_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXAM_EXAM_SESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam WHERE tenant_id = :tenantId
                AND exam_session_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_MARKSHEET_EXAM_SESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet WHERE tenant_id = :tenantId
                AND exam_session_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RESULT_TEMPLATE_EXAM_SESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.result_template WHERE tenant_id = :tenantId
                AND exam_session_id = :id AND deleted_at IS NULL)
            """;

    /** assessment.ScheduleExamSession: allowed while status is draft. */
    private static final String TRANSITION_SCHEDULE_EXAM_SESSION = """
            UPDATE assessment.exam_session
               SET status = 'scheduled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.MarkExamSessionHeld: allowed while status is scheduled. */
    private static final String TRANSITION_MARK_EXAM_SESSION_HELD = """
            UPDATE assessment.exam_session
               SET status = 'held',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.CompleteExamSession: allowed while status is held. */
    private static final String TRANSITION_COMPLETE_EXAM_SESSION = """
            UPDATE assessment.exam_session
               SET status = 'done',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.CancelExamSession: allowed while status is draft or scheduled. */
    private static final String TRANSITION_CANCEL_EXAM_SESSION = """
            UPDATE assessment.exam_session
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.ResetExamSession: allowed while status is cancelled. */
    private static final String TRANSITION_RESET_EXAM_SESSION = """
            UPDATE assessment.exam_session
               SET status = 'draft',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public ExamSessionRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ExamSessionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ExamSessionRepository::map).optional();
    }

    public Page<ExamSessionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ExamSessionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ExamSessionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ExamSessionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ExamSessionEntity insert(ExamSessionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("academicTermId", e.getAcademicTermId())
                .param("examTypeId", e.getExamTypeId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("evaluationType", e.getEvaluationType())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("venue", e.getVenue())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ExamSessionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ExamSessionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("academicTermId", e.getAcademicTermId())
                .param("examTypeId", e.getExamTypeId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("evaluationType", e.getEvaluationType())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("venue", e.getVenue())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ExamSessionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_exam_session_1: institution_id, code where (deleted_at IS NULL). */
    public boolean takenInstitutionIdCode1(ExamSessionEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_EXAM_SESSION_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.exam_type row exists (live). */
    public boolean existsExamTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXAM_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live assessment.exam rows still reference the row. */
    public boolean referencedByExamExamSessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXAM_EXAM_SESSION_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live assessment.marksheet rows still reference the row. */
    public boolean referencedByMarksheetExamSessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_MARKSHEET_EXAM_SESSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live assessment.result_template rows still reference the row. */
    public boolean referencedByResultTemplateExamSessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RESULT_TEMPLATE_EXAM_SESSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** assessment.ScheduleExamSession; 0 rows means the row changed (or vanished) since it was read. */
    public int scheduleExamSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_SCHEDULE_EXAM_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.MarkExamSessionHeld; 0 rows means the row changed (or vanished) since it was read. */
    public int markExamSessionHeld(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_MARK_EXAM_SESSION_HELD)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.CompleteExamSession; 0 rows means the row changed (or vanished) since it was read. */
    public int completeExamSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_COMPLETE_EXAM_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.CancelExamSession; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelExamSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_EXAM_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.ResetExamSession; 0 rows means the row changed (or vanished) since it was read. */
    public int resetExamSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESET_EXAM_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ExamSessionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ExamSessionEntity map(ResultSet rs, int row) throws SQLException {
        return ExamSessionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .academicTermId(rs.getObject("academic_term_id", UUID.class))
                .examTypeId(rs.getObject("exam_type_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .gradeLevelId(rs.getObject("grade_level_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .batchId(rs.getObject("batch_id", UUID.class))
                .evaluationType(rs.getString("evaluation_type"))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
                .venue(rs.getString("venue"))
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
