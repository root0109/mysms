package com.mysms.sis.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.domain.StudentEnrollmentEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to sis.student_enrollment: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class StudentEnrollmentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("effectiveFrom", "r.effective_from", ValueType.DATE),
            field("effectiveTo", "r.effective_to", ValueType.DATE),
            field("previousEnrollmentId", "r.previous_enrollment_id", ValueType.UUID),
            field("changeReason", "r.change_reason", ValueType.STRING),
            field("studentId", "r.student_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("rollNumber", "r.roll_number", ValueType.STRING),
            field("idCardNumber", "r.id_card_number", ValueType.STRING),
            field("boardRegistrationNumber", "r.board_registration_number", ValueType.STRING),
            field("enrollmentStatus", "r.enrollment_status", ValueType.STRING),
            field("resultStatus", "r.result_status", ValueType.STRING),
            field("enrolledOn", "r.enrolled_on", ValueType.DATE),
            field("leftOn", "r.left_on", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StudentEnrollmentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StudentEnrollmentEntity::getCreatedAt),
            Map.entry("updateTime", StudentEnrollmentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.effective_from, r.effective_to, r.previous_enrollment_id, r.change_reason,
                   r.student_id, r.academic_year_id, r.class_section_id, r.roll_number, r.id_card_number,
                   r.board_registration_number, r.enrollment_status, r.result_status, r.enrolled_on, r.left_on,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM sis.student_enrollment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO sis.student_enrollment
            (id, tenant_id, effective_from, change_reason, student_id, academic_year_id, class_section_id,
             roll_number, id_card_number, board_registration_number, enrollment_status, result_status, enrolled_on,
             left_on, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :effectiveFrom, COALESCE(:changeReason, 'initial'::character varying),
                    :studentId, :academicYearId, :classSectionId, :rollNumber, :idCardNumber,
                    :boardRegistrationNumber, COALESCE(:enrollmentStatus, 'enrolled'::character varying),
                    COALESCE(:resultStatus, 'pending'::character varying), :enrolledOn, :leftOn, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, effective_from, effective_to, previous_enrollment_id, change_reason, student_id,
                      academic_year_id, class_section_id, roll_number, id_card_number, board_registration_number,
                      enrollment_status, result_status, enrolled_on, left_on, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE sis.student_enrollment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE sis.student_enrollment
               SET student_id = :studentId,
                   academic_year_id = :academicYearId,
                   class_section_id = :classSectionId,
                   roll_number = :rollNumber,
                   id_card_number = :idCardNumber,
                   board_registration_number = :boardRegistrationNumber,
                   enrollment_status = :enrollmentStatus,
                   result_status = :resultStatus,
                   enrolled_on = :enrolledOn,
                   left_on = :leftOn,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String OPEN_OF_STUDENT = SELECT_LIVE
            + " AND r.student_id = :studentId AND r.effective_to IS NULL AND r.enrollment_status = 'enrolled'"
            + " ORDER BY r.created_at, r.id";
    private static final String OPEN_OF_STUDENT_IN_YEAR = SELECT_LIVE
            + " AND r.student_id = :studentId AND r.academic_year_id = :academicYearId AND r.effective_to IS NULL"
            + " FOR UPDATE";
    private static final String CURRENT_OF_STUDENT = SELECT_LIVE
            + " AND r.student_id = :studentId AND (r.academic_year_id = :academicYearId OR NOT :inYear)"
            + " ORDER BY (r.effective_to IS NULL) DESC, r.effective_from DESC NULLS LAST, r.created_at DESC, r.id"
            + " LIMIT 1";
    private static final String CURRENT_OF_STUDENTS = SELECT_LIVE
            + " AND r.student_id = ANY(:studentIds) AND r.effective_to IS NULL AND r.enrollment_status = 'enrolled'"
            + " ORDER BY r.created_at, r.id";
    private static final String ROSTER_OF_CLASS = SELECT_LIVE
            + " AND r.class_section_id = :classSectionId AND r.enrollment_status = 'enrolled'"
            + " AND r.effective_to IS NULL ORDER BY r.roll_number NULLS LAST, r.created_at, r.id";

    /** Ends a placement: its outcome, the day it ended, and (for a student who left) the leaving date. */
    private static final String CLOSE = """
            UPDATE sis.student_enrollment
               SET enrollment_status = :status,
                   effective_to = :endedOn,
                   left_on = :leftOn,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** sis.ApplyPromotions: the next year's placement, following the one the promotion closed. */
    private static final String INSERT_FOLLOWING = """
            INSERT INTO sis.student_enrollment
            (id, tenant_id, effective_from, previous_enrollment_id, change_reason, student_id, academic_year_id,
             class_section_id, enrollment_status, result_status, enrolled_on, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :effectiveFrom, :previousEnrollmentId, 'promotion', :studentId, :academicYearId,
                    :classSectionId, 'enrolled', 'pending', :effectiveFrom, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, effective_from, effective_to, previous_enrollment_id, change_reason, student_id,
                      academic_year_id, class_section_id, roll_number, id_card_number, board_registration_number,
                      enrollment_status, result_status, enrolled_on, left_on, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;

    private static final String EXISTS_PREVIOUS_ENROLLMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student_enrollment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_STUDENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STUDENT_ENROLLMENT_PREVIOUS_ENROLLMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student_enrollment WHERE tenant_id = :tenantId
                AND previous_enrollment_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM sis.student p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.student_id IN (SELECT p0.id FROM sis.student p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    /** sis.ListClassRoster: the students placed in one class section now (enrolled; not promoted, left or done). */
    private static final String ROSTER =
            " (r.class_section_id = :rosterClassSectionId AND r.enrollment_status = 'enrolled') ";

    private final JdbcClient jdbc;

    public StudentEnrollmentRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StudentEnrollmentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StudentEnrollmentRepository::map).optional();
    }

    public Page<StudentEnrollmentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StudentEnrollmentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StudentEnrollmentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StudentEnrollmentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StudentEnrollmentEntity insert(StudentEnrollmentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("effectiveFrom", e.getEffectiveFrom())
                .param("changeReason", e.getChangeReason())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("classSectionId", e.getClassSectionId())
                .param("rollNumber", e.getRollNumber())
                .param("idCardNumber", e.getIdCardNumber())
                .param("boardRegistrationNumber", e.getBoardRegistrationNumber())
                .param("enrollmentStatus", e.getEnrollmentStatus())
                .param("resultStatus", e.getResultStatus())
                .param("enrolledOn", e.getEnrolledOn())
                .param("leftOn", e.getLeftOn())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StudentEnrollmentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StudentEnrollmentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("classSectionId", e.getClassSectionId())
                .param("rollNumber", e.getRollNumber())
                .param("idCardNumber", e.getIdCardNumber())
                .param("boardRegistrationNumber", e.getBoardRegistrationNumber())
                .param("enrollmentStatus", e.getEnrollmentStatus())
                .param("resultStatus", e.getResultStatus())
                .param("enrolledOn", e.getEnrolledOn())
                .param("leftOn", e.getLeftOn())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StudentEnrollmentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** The student's open placements (enrolled, not ended), oldest first. */
    public List<StudentEnrollmentEntity> openOfStudent(UUID studentId) {
        return jdbc.sql(OPEN_OF_STUDENT).param("studentId", studentId).query(StudentEnrollmentRepository::map).list();
    }

    /** The student's placement of the year that has not ended, locked for the transaction that will close it. */
    public Optional<StudentEnrollmentEntity> lockOpenOfStudentInYear(UUID studentId, UUID academicYearId) {
        return jdbc.sql(OPEN_OF_STUDENT_IN_YEAR).param("studentId", studentId)
                .param("academicYearId", academicYearId).query(StudentEnrollmentRepository::map).optional();
    }

    /**
     * The placement a summary shows: of the given year when one is named, otherwise the open one, otherwise the
     * most recent.
     */
    public Optional<StudentEnrollmentEntity> currentOfStudent(UUID studentId, Optional<UUID> academicYearId) {
        return jdbc.sql(CURRENT_OF_STUDENT).param("studentId", studentId)
                .param("academicYearId", academicYearId.orElse(studentId)).param("inYear", academicYearId.isPresent())
                .query(StudentEnrollmentRepository::map).optional();
    }

    /** The open placements of these students, oldest first. */
    public List<StudentEnrollmentEntity> openOfStudents(Collection<UUID> studentIds) {
        return jdbc.sql(CURRENT_OF_STUDENTS).param("studentIds", studentIds.toArray(UUID[]::new))
                .query(StudentEnrollmentRepository::map).list();
    }

    /** Everyone placed in the class section now, in roll order. */
    public List<StudentEnrollmentEntity> rosterOfClass(UUID classSectionId) {
        return jdbc.sql(ROSTER_OF_CLASS).param("classSectionId", classSectionId)
                .query(StudentEnrollmentRepository::map).list();
    }

    /** Ends the placement; 0 rows means the row changed (or vanished) since it was read. */
    public int close(UUID id, int expected, String status, LocalDate endedOn, LocalDate leftOn, OffsetDateTime now,
            UUID actor) {
        return jdbc.sql(CLOSE)
                .param("id", id)
                .param("expected", expected)
                .param("status", status)
                .param("endedOn", endedOn)
                .param("leftOn", leftOn)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Inserts the placement that follows {@code previousEnrollmentId} (a promotion) and returns it as stored. */
    public StudentEnrollmentEntity insertFollowing(StudentEnrollmentEntity e) {
        return jdbc.sql(INSERT_FOLLOWING)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("effectiveFrom", e.getEffectiveFrom())
                .param("previousEnrollmentId", e.getPreviousEnrollmentId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("classSectionId", e.getClassSectionId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StudentEnrollmentRepository::map).single();
    }

    /** Whether the referenced sis.student_enrollment row exists (live). */
    public boolean existsPreviousEnrollmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PREVIOUS_ENROLLMENT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced sis.student row exists (live). */
    public boolean existsStudentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STUDENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live sis.student_enrollment rows still reference the row. */
    public boolean referencedByStudentEnrollmentPreviousEnrollmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_ENROLLMENT_PREVIOUS_ENROLLMENT_ID).param("id", id)
                .query(Boolean.class).single());
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

    /** The roster predicate of one class section, for {@link #list} and {@link #count}. */
    public static SqlFragment roster(UUID classSectionId) {
        return new SqlFragment(ROSTER, Map.of("rosterClassSectionId", classSectionId));
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, StudentEnrollmentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StudentEnrollmentEntity map(ResultSet rs, int row) throws SQLException {
        return StudentEnrollmentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .effectiveFrom(rs.getObject("effective_from", LocalDate.class))
                .effectiveTo(rs.getObject("effective_to", LocalDate.class))
                .previousEnrollmentId(rs.getObject("previous_enrollment_id", UUID.class))
                .changeReason(rs.getString("change_reason"))
                .studentId(rs.getObject("student_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .rollNumber(rs.getString("roll_number"))
                .idCardNumber(rs.getString("id_card_number"))
                .boardRegistrationNumber(rs.getString("board_registration_number"))
                .enrollmentStatus(rs.getString("enrollment_status"))
                .resultStatus(rs.getString("result_status"))
                .enrolledOn(rs.getObject("enrolled_on", LocalDate.class))
                .leftOn(rs.getObject("left_on", LocalDate.class))
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
