package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.ExamEntity;
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
 * Plain-JDBC access to assessment.exam: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ExamRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("examSessionId", "r.exam_session_id", ValueType.UUID),
            field("subjectId", "r.subject_id", ValueType.UUID),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("startAt", "r.start_at", ValueType.TIMESTAMP),
            field("endAt", "r.end_at", ValueType.TIMESTAMP),
            field("totalMarks", "r.total_marks", ValueType.DECIMAL),
            field("passMarks", "r.pass_marks", ValueType.DECIMAL),
            field("status", "r.status", ValueType.STRING),
            field("resultsEntered", "r.results_entered", ValueType.BOOLEAN),
            field("notes", "r.notes", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ExamEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ExamEntity::getCreatedAt),
            Map.entry("updateTime", ExamEntity::getUpdatedAt),
            Map.entry("code", ExamEntity::getCode),
            Map.entry("name", ExamEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.exam_session_id, r.subject_id, r.class_section_id, r.code, r.name, r.start_at,
                   r.end_at, r.total_marks, r.pass_marks, r.status, r.results_entered, r.notes, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.exam r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** The row locked until the transaction ends, so two roster or room writes of one exam run one after the other. */
    private static final String FIND_BY_ID_FOR_UPDATE = FIND_BY_ID + " FOR UPDATE";
    private static final String LIST_FOR_SESSION = SELECT_LIVE + " AND r.exam_session_id = :sessionId ORDER BY r.id";
    private static final String INSERT = """
            INSERT INTO assessment.exam
            (id, tenant_id, exam_session_id, subject_id, class_section_id, code, name, start_at, end_at, total_marks,
             pass_marks, results_entered, notes, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :examSessionId, :subjectId, :classSectionId, :code, :name, :startAt, :endAt,
                    :totalMarks, :passMarks, COALESCE(:resultsEntered, false), :notes, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, exam_session_id, subject_id, class_section_id, code, name, start_at, end_at,
                      total_marks, pass_marks, status, results_entered, notes, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.exam
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.exam
               SET exam_session_id = :examSessionId,
                   subject_id = :subjectId,
                   class_section_id = :classSectionId,
                   code = :code,
                   name = :name,
                   start_at = :startAt,
                   end_at = :endAt,
                   total_marks = :totalMarks,
                   pass_marks = :passMarks,
                   results_entered = :resultsEntered,
                   notes = :notes,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_EXAM_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam
                            WHERE tenant_id = :tenantId
                              AND exam_session_id = :examSessionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_EXAM_SESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_session WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXAM_INVIGILATOR_EXAM_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_invigilator WHERE tenant_id = :tenantId
                AND exam_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EXAM_RESULT_EXAM_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_result WHERE tenant_id = :tenantId
                AND exam_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.exam_session p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** assessment.ScheduleExam: allowed while status is draft. */
    private static final String TRANSITION_SCHEDULE_EXAM = """
            UPDATE assessment.exam
               SET status = 'scheduled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.CompleteExam: allowed while status is held or result_updated. */
    private static final String TRANSITION_COMPLETE_EXAM = """
            UPDATE assessment.exam
               SET status = 'done',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.CancelExam: allowed while status is draft or scheduled. */
    private static final String TRANSITION_CANCEL_EXAM = """
            UPDATE assessment.exam
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.ResetExam: allowed while status is cancelled. */
    private static final String TRANSITION_RESET_EXAM = """
            UPDATE assessment.exam
               SET status = 'draft',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String TRANSITION_MARK_HELD = """
            UPDATE assessment.exam SET status = 'held', results_entered = false, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String TRANSITION_RESULTS_UPDATED = """
            UPDATE assessment.exam SET status = 'result_updated', results_entered = true, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REOPEN_RESULTS = """
            UPDATE assessment.exam SET status = 'held', results_entered = false, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.exam_session_id IN (SELECT p0.id FROM assessment.exam_session p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public ExamRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ExamEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ExamRepository::map).optional();
    }

    /** The live row, locked until the transaction ends. */
    public Optional<ExamEntity> findByIdForUpdate(UUID id) {
        return jdbc.sql(FIND_BY_ID_FOR_UPDATE).param("id", id).query(ExamRepository::map).optional();
    }

    public List<ExamEntity> listForSession(UUID sessionId) {
        return jdbc.sql(LIST_FOR_SESSION).param("sessionId", sessionId).query(ExamRepository::map).list();
    }

    public Page<ExamEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ExamEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ExamRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ExamEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ExamEntity insert(ExamEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("examSessionId", e.getExamSessionId())
                .param("subjectId", e.getSubjectId())
                .param("classSectionId", e.getClassSectionId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("totalMarks", e.getTotalMarks())
                .param("passMarks", e.getPassMarks())
                .param("resultsEntered", e.getResultsEntered())
                .param("notes", e.getNotes())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ExamRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ExamEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("examSessionId", e.getExamSessionId())
                .param("subjectId", e.getSubjectId())
                .param("classSectionId", e.getClassSectionId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("totalMarks", e.getTotalMarks())
                .param("passMarks", e.getPassMarks())
                .param("resultsEntered", e.getResultsEntered())
                .param("notes", e.getNotes())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ExamEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_exam_1: exam_session_id, code where (deleted_at IS NULL). */
    public boolean takenExamSessionIdCode1(ExamEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_EXAM_1_TAKEN)
                .param("examSessionId", e.getExamSessionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.exam_session row exists (live). */
    public boolean existsExamSessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXAM_SESSION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live assessment.exam_invigilator rows still reference the row. */
    public boolean referencedByExamInvigilatorExamId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXAM_INVIGILATOR_EXAM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live assessment.exam_result rows still reference the row. */
    public boolean referencedByExamResultExamId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EXAM_RESULT_EXAM_ID).param("id", id).query(Boolean.class)
                .single());
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

    /** assessment.ScheduleExam; 0 rows means the row changed (or vanished) since it was read. */
    public int scheduleExam(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_SCHEDULE_EXAM)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.CompleteExam; 0 rows means the row changed (or vanished) since it was read. */
    public int completeExam(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_COMPLETE_EXAM)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.CancelExam; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelExam(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_EXAM)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.ResetExam; 0 rows means the row changed (or vanished) since it was read. */
    public int resetExam(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESET_EXAM)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    public int markExamHeld(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_MARK_HELD).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    public int markExamResultsUpdated(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESULTS_UPDATED).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    public int reopenResults(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(REOPEN_RESULTS).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ExamEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ExamEntity map(ResultSet rs, int row) throws SQLException {
        return ExamEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .examSessionId(rs.getObject("exam_session_id", UUID.class))
                .subjectId(rs.getObject("subject_id", UUID.class))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .startAt(rs.getObject("start_at", OffsetDateTime.class))
                .endAt(rs.getObject("end_at", OffsetDateTime.class))
                .totalMarks(rs.getBigDecimal("total_marks"))
                .passMarks(rs.getBigDecimal("pass_marks"))
                .status(rs.getString("status"))
                .resultsEntered(rs.getObject("results_entered", Boolean.class))
                .notes(rs.getString("notes"))
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
