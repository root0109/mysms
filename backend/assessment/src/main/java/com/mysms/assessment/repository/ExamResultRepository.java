package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.ExamResultEntity;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.id.UuidV7;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
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
 * Plain-JDBC access to assessment.exam_result (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class ExamResultRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("maxMarks", "r.max_marks", ValueType.DECIMAL),
            field("gradeLabel", "r.grade_label", ValueType.STRING),
            field("gradePoint", "r.grade_point", ValueType.DECIMAL),
            field("credits", "r.credits", ValueType.DECIMAL),
            field("resultStatus", "r.result_status", ValueType.STRING),
            field("marksheetLineId", "r.marksheet_line_id", ValueType.UUID),
            field("evaluatedByUserId", "r.evaluated_by_user_id", ValueType.UUID),
            field("evaluatedAt", "r.evaluated_at", ValueType.TIMESTAMP),
            field("examId", "r.exam_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("roomId", "r.room_id", ValueType.UUID),
            field("attendanceStatus", "r.attendance_status", ValueType.STRING),
            field("marks", "r.marks", ValueType.DECIMAL),
            field("note", "r.note", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ExamResultEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ExamResultEntity::getCreatedAt),
            Map.entry("updateTime", ExamResultEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.max_marks, r.grade_label, r.grade_point, r.credits, r.result_status,
                   r.marksheet_line_id, r.evaluated_by_user_id, r.evaluated_at, r.exam_id, r.student_id, r.room_id,
                   r.attendance_status, r.marks, r.note, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM assessment.exam_result r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.exam_result
            (id, tenant_id, exam_id, student_id, room_id, attendance_status, max_marks, credits, note, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :examId, :studentId, :roomId,
                    COALESCE(:attendanceStatus, 'expected'::character varying), :maxMarks, :credits, :note, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, max_marks, grade_label, grade_point, credits, result_status, marksheet_line_id,
                      evaluated_by_user_id, evaluated_at, exam_id, student_id, room_id, attendance_status, marks, note,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String LIST_FOR_EXAM = SELECT_LIVE + " AND r.exam_id = :examId ORDER BY r.student_id, r.id";
    private static final String LIST_FOR_SESSION_STUDENT = SELECT_LIVE + " AND r.student_id = :studentId"
            + " AND r.exam_id IN (SELECT e.id FROM assessment.exam e WHERE e.tenant_id = :tenantId"
            + " AND e.exam_session_id = :sessionId AND e.deleted_at IS NULL) ORDER BY r.exam_id, r.id";
    private static final String COUNT_FOR_EXAM = """
            SELECT count(*) FROM assessment.exam_result WHERE tenant_id = :tenantId AND exam_id = :examId
              AND deleted_at IS NULL
            """;
    private static final String ALL_MARKS_ENTERED = """
            SELECT count(*) > 0 AND NOT EXISTS (
                SELECT 1 FROM assessment.exam_result WHERE tenant_id = :tenantId AND exam_id = :examId
                  AND deleted_at IS NULL
                  AND (attendance_status = 'expected' OR (attendance_status = 'present' AND marks IS NULL))
            ) FROM assessment.exam_result WHERE tenant_id = :tenantId AND exam_id = :examId AND deleted_at IS NULL
            """;
    private static final String ASSIGN_ROOM = """
            UPDATE assessment.exam_result SET room_id = :roomId, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SET_CREDITS = """
            UPDATE assessment.exam_result SET credits = :credits, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String RECORD_MARKS = """
            UPDATE assessment.exam_result
               SET attendance_status = :attendanceStatus, marks = :marks, max_marks = :maxMarks,
                   result_status = :resultStatus, note = :note, evaluated_by_user_id = :actor,
                   evaluated_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
               AND marksheet_line_id IS NULL
            """;
    private static final String SET_GENERATED_VALUES = """
            UPDATE assessment.exam_result SET grade_label = :gradeLabel, grade_point = :gradePoint,
                   credits = :credits, marksheet_line_id = :lineId, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String INSERT_CHANGE_LOG = """
            INSERT INTO assessment.result_change_log
                (id, tenant_id, exam_result_id, changed_by_user_id, changed_at, old_marks, new_marks,
                 old_result_status, new_result_status, old_grade_label, new_grade_label, reason)
            VALUES (:id, :tenantId, :resultId, :actor, :now, :oldMarks, :newMarks,
                    :oldStatus, :newStatus, :oldGrade, :newGrade, :reason)
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.exam_result
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.exam_result
               SET room_id = :roomId,
                   attendance_status = :attendanceStatus,
                   note = :note,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_EXAM_RESULT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_result
                            WHERE tenant_id = :tenantId
                              AND exam_id = :examId AND student_id = :studentId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_EXAM_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_MARKSHEET_LINE_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.marksheet_line WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RESULT_CHANGE_LOG_EXAM_RESULT_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.result_change_log WHERE tenant_id = :tenantId
                AND exam_result_id = :id)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM assessment.exam p0 JOIN assessment.exam_session p1 ON p1.tenant_id = :tenantId
                AND p1.id =
            p0.exam_session_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.exam_id IN (SELECT p0.id FROM assessment.exam p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.exam_session_id IN (SELECT p1.id
             FROM assessment.exam_session p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds))))))
            """;

    private final JdbcClient jdbc;

    public ExamResultRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ExamResultEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ExamResultRepository::map).optional();
    }

    public List<ExamResultEntity> listForExam(UUID examId) {
        return jdbc.sql(LIST_FOR_EXAM).param("examId", examId).query(ExamResultRepository::map).list();
    }

    public List<ExamResultEntity> listForSessionAndStudent(UUID sessionId, UUID studentId) {
        return jdbc.sql(LIST_FOR_SESSION_STUDENT).param("sessionId", sessionId).param("studentId", studentId)
                .query(ExamResultRepository::map).list();
    }

    public long countForExam(UUID examId) {
        return jdbc.sql(COUNT_FOR_EXAM).param("examId", examId).query(Long.class).single();
    }

    public boolean allMarksEntered(UUID examId) {
        return Boolean.TRUE.equals(jdbc.sql(ALL_MARKS_ENTERED).param("examId", examId)
                .query(Boolean.class).single());
    }

    public int assignRoom(UUID id, int expected, UUID roomId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(ASSIGN_ROOM).param("id", id).param("expected", expected).param("roomId", roomId)
                .param("now", now).param("actor", actor).update();
    }

    public int setCredits(UUID id, int expected, BigDecimal credits, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_CREDITS).param("id", id).param("expected", expected).param("credits", credits)
                .param("now", now).param("actor", actor).update();
    }

    public int recordMarks(ExamResultEntity current, int expected, String attendanceStatus, BigDecimal marks,
            BigDecimal maxMarks, String resultStatus, String note, UUID actor, OffsetDateTime now, String reason) {
        int updated = jdbc.sql(RECORD_MARKS).param("id", current.getId()).param("expected", expected)
                .param("attendanceStatus", attendanceStatus).param("marks", marks).param("maxMarks", maxMarks)
                .param("resultStatus", resultStatus).param("note", note).param("actor", actor)
                .param("now", now).update();
        if (updated > 0) {
            jdbc.sql(INSERT_CHANGE_LOG).param("id", UuidV7.next()).param("resultId", current.getId())
                    .param("actor", actor).param("now", now).param("oldMarks", current.getMarks())
                    .param("newMarks", marks).param("oldStatus", current.getResultStatus())
                    .param("newStatus", resultStatus).param("oldGrade", current.getGradeLabel())
                    .param("newGrade", null).param("reason", reason).update();
        }
        return updated;
    }

    public int setGeneratedValues(ExamResultEntity current, int expected, String gradeLabel, BigDecimal gradePoint,
            BigDecimal credits, UUID lineId, UUID actor, OffsetDateTime now) {
        return jdbc.sql(SET_GENERATED_VALUES).param("id", current.getId()).param("expected", expected)
                .param("gradeLabel", gradeLabel).param("gradePoint", gradePoint).param("credits", credits)
                .param("lineId", lineId).param("actor", actor).param("now", now).update();
    }

    public Page<ExamResultEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ExamResultEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ExamResultRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ExamResultEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ExamResultEntity insert(ExamResultEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("examId", e.getExamId())
                .param("studentId", e.getStudentId())
                .param("roomId", e.getRoomId())
                .param("attendanceStatus", e.getAttendanceStatus())
                .param("maxMarks", e.getMaxMarks())
                .param("credits", e.getCredits())
                .param("note", e.getNote())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ExamResultRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ExamResultEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("roomId", e.getRoomId())
                .param("attendanceStatus", e.getAttendanceStatus())
                .param("note", e.getNote())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ExamResultEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_exam_result_1: exam_id, student_id where (deleted_at IS NULL). */
    public boolean takenExamIdStudentId1(ExamResultEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_EXAM_RESULT_1_TAKEN)
                .param("examId", e.getExamId())
                .param("studentId", e.getStudentId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.exam row exists (live). */
    public boolean existsExamId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXAM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced assessment.marksheet_line row exists (live). */
    public boolean existsMarksheetLineId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_MARKSHEET_LINE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live assessment.result_change_log rows still reference the row. */
    public boolean referencedByResultChangeLogExamResultId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RESULT_CHANGE_LOG_EXAM_RESULT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through exam_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, ExamResultEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ExamResultEntity map(ResultSet rs, int row) throws SQLException {
        return ExamResultEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .maxMarks(rs.getBigDecimal("max_marks"))
                .gradeLabel(rs.getString("grade_label"))
                .gradePoint(rs.getBigDecimal("grade_point"))
                .credits(rs.getBigDecimal("credits"))
                .resultStatus(rs.getString("result_status"))
                .marksheetLineId(rs.getObject("marksheet_line_id", UUID.class))
                .evaluatedByUserId(rs.getObject("evaluated_by_user_id", UUID.class))
                .evaluatedAt(rs.getObject("evaluated_at", OffsetDateTime.class))
                .examId(rs.getObject("exam_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .roomId(rs.getObject("room_id", UUID.class))
                .attendanceStatus(rs.getString("attendance_status"))
                .marks(rs.getBigDecimal("marks"))
                .note(rs.getString("note"))
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
