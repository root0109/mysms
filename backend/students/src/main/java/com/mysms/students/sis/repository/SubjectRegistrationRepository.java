package com.mysms.students.sis.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.domain.SubjectRegistrationEntity;
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
 * Plain-JDBC access to sis.subject_registration: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class SubjectRegistrationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("academicTermId", "r.academic_term_id", ValueType.UUID),
            field("programEnrollmentId", "r.program_enrollment_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("batchId", "r.batch_id", ValueType.UUID),
            field("referenceNo", "r.reference_no", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("minUnitLoad", "r.min_unit_load", ValueType.DECIMAL),
            field("maxUnitLoad", "r.max_unit_load", ValueType.DECIMAL),
            field("readByStudent", "r.read_by_student", ValueType.BOOLEAN),
            field("readByGuardian", "r.read_by_guardian", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<SubjectRegistrationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", SubjectRegistrationEntity::getCreatedAt),
            Map.entry("updateTime", SubjectRegistrationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.academic_year_id, r.academic_term_id, r.program_enrollment_id, r.student_id,
                   r.course_id, r.batch_id, r.reference_no, r.status, r.min_unit_load, r.max_unit_load,
                   r.read_by_student, r.read_by_guardian, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM sis.subject_registration r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String LOCK_BY_ID = FIND_BY_ID + " FOR UPDATE";
    private static final String INSERT = """
            INSERT INTO sis.subject_registration
            (id, tenant_id, academic_year_id, academic_term_id, program_enrollment_id, student_id, course_id,
             batch_id, reference_no, min_unit_load, max_unit_load, read_by_student, read_by_guardian, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :academicYearId, :academicTermId, :programEnrollmentId, :studentId, :courseId,
                    :batchId, :referenceNo, :minUnitLoad, :maxUnitLoad, COALESCE(:readByStudent, false),
                    COALESCE(:readByGuardian, false), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, academic_year_id, academic_term_id, program_enrollment_id, student_id, course_id,
                      batch_id, reference_no, status, min_unit_load, max_unit_load, read_by_student, read_by_guardian,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE sis.subject_registration
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE sis.subject_registration
               SET student_id = :studentId,
                   course_id = :courseId,
                   batch_id = :batchId,
                   reference_no = :referenceNo,
                   min_unit_load = :minUnitLoad,
                   max_unit_load = :maxUnitLoad,
                   read_by_student = :readByStudent,
                   read_by_guardian = :readByGuardian,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_SUBJECT_REGISTRATION_SLOT_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM sis.subject_registration
                            WHERE tenant_id = :tenantId
                              AND program_enrollment_id IS NOT DISTINCT FROM :programEnrollmentId AND academic_year_id
                                  IS NOT DISTINCT FROM :academicYearId AND academic_term_id IS NOT DISTINCT FROM
                                  :academicTermId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_PROGRAM_ENROLLMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.program_enrollment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_STUDENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_SUBJECT_REGISTRATION_LINE_SUBJECT_REGISTRATION_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.subject_registration_line WHERE tenant_id = :tenantId
                AND subject_registration_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM sis.student p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** sis.SubmitSubjectRegistration: allowed while status is draft. */
    private static final String TRANSITION_SUBMIT_SUBJECT_REGISTRATION = """
            UPDATE sis.subject_registration
               SET status = 'submitted',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** sis.RejectSubjectRegistration: allowed while status is submitted. */
    private static final String TRANSITION_REJECT_SUBJECT_REGISTRATION = """
            UPDATE sis.subject_registration
               SET status = 'rejected',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** sis.ResetSubjectRegistration: allowed while status is rejected. */
    private static final String TRANSITION_RESET_SUBJECT_REGISTRATION = """
            UPDATE sis.subject_registration
               SET status = 'draft',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** sis.LoadCourseSubjects: the lines of a draft changed, so the registration takes a new version (its ETag). */
    private static final String LINES_CHANGED = """
            UPDATE sis.subject_registration
               SET updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft' AND deleted_at IS NULL
            """;

    /** sis.ApproveSubjectRegistration: allowed while status is submitted. */
    private static final String TRANSITION_APPROVE_SUBJECT_REGISTRATION = """
            UPDATE sis.subject_registration
               SET status = 'approved',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.student_id IN (SELECT p0.id FROM sis.student p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public SubjectRegistrationRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SubjectRegistrationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SubjectRegistrationRepository::map).optional();
    }

    public Page<SubjectRegistrationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SubjectRegistrationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SubjectRegistrationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SubjectRegistrationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public SubjectRegistrationEntity insert(SubjectRegistrationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("academicYearId", e.getAcademicYearId())
                .param("academicTermId", e.getAcademicTermId())
                .param("programEnrollmentId", e.getProgramEnrollmentId())
                .param("studentId", e.getStudentId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("referenceNo", e.getReferenceNo())
                .param("minUnitLoad", e.getMinUnitLoad())
                .param("maxUnitLoad", e.getMaxUnitLoad())
                .param("readByStudent", e.getReadByStudent())
                .param("readByGuardian", e.getReadByGuardian())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SubjectRegistrationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SubjectRegistrationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("studentId", e.getStudentId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("referenceNo", e.getReferenceNo())
                .param("minUnitLoad", e.getMinUnitLoad())
                .param("maxUnitLoad", e.getMaxUnitLoad())
                .param("readByStudent", e.getReadByStudent())
                .param("readByGuardian", e.getReadByGuardian())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SubjectRegistrationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /**
     * uq_subject_registration_slot: program_enrollment_id, academic_year_id, academic_term_id where (deleted_at IS
     * NULL).
     */
    public boolean takenProgramEnrollmentIdAcademicYearIdAcademicTermIdSlot(SubjectRegistrationEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_SUBJECT_REGISTRATION_SLOT_TAKEN)
                .param("programEnrollmentId", e.getProgramEnrollmentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("academicTermId", e.getAcademicTermId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced sis.program_enrollment row exists (live). */
    public boolean existsProgramEnrollmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PROGRAM_ENROLLMENT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced sis.student row exists (live). */
    public boolean existsStudentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STUDENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live sis.subject_registration_line rows still reference the row. */
    public boolean referencedBySubjectRegistrationLineSubjectRegistrationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_SUBJECT_REGISTRATION_LINE_SUBJECT_REGISTRATION_ID)
                .param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through student_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** sis.SubmitSubjectRegistration; 0 rows means the row changed (or vanished) since it was read. */
    public int submitSubjectRegistration(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_SUBMIT_SUBJECT_REGISTRATION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** sis.RejectSubjectRegistration; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectSubjectRegistration(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REJECT_SUBJECT_REGISTRATION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** sis.ResetSubjectRegistration; 0 rows means the row changed (or vanished) since it was read. */
    public int resetSubjectRegistration(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESET_SUBJECT_REGISTRATION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** The live row, locked until the transaction ends, so no step changes it meanwhile. */
    public Optional<SubjectRegistrationEntity> lockById(UUID id) {
        return jdbc.sql(LOCK_BY_ID).param("id", id).query(SubjectRegistrationRepository::map).optional();
    }

    /** Gives a draft whose lines changed a new version; 0 rows means it changed, or is no draft, since it was read. */
    public int linesChanged(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(LINES_CHANGED)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** sis.ApproveSubjectRegistration; 0 rows means the row changed (or vanished) since it was read. */
    public int approveSubjectRegistration(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_APPROVE_SUBJECT_REGISTRATION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, SubjectRegistrationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SubjectRegistrationEntity map(ResultSet rs, int row) throws SQLException {
        return SubjectRegistrationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .academicTermId(rs.getObject("academic_term_id", UUID.class))
                .programEnrollmentId(rs.getObject("program_enrollment_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .batchId(rs.getObject("batch_id", UUID.class))
                .referenceNo(rs.getString("reference_no"))
                .status(rs.getString("status"))
                .minUnitLoad(rs.getBigDecimal("min_unit_load"))
                .maxUnitLoad(rs.getBigDecimal("max_unit_load"))
                .readByStudent(rs.getObject("read_by_student", Boolean.class))
                .readByGuardian(rs.getObject("read_by_guardian", Boolean.class))
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
