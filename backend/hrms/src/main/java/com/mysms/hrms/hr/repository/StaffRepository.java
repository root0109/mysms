package com.mysms.hrms.hr.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.domain.StaffEntity;
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
 * Plain-JDBC access to hr.staff: live rows only, keyset paging, and the row_version guard on every update and delete.
 * Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class StaffRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("employeeCode", "r.employee_code", ValueType.STRING),
            field("designationId", "r.designation_id", ValueType.UUID),
            field("subDesignation", "r.sub_designation", ValueType.STRING),
            field("employmentType", "r.employment_type", ValueType.STRING),
            field("joiningDate", "r.joining_date", ValueType.DATE),
            field("reportingToStaffId", "r.reporting_to_staff_id", ValueType.UUID),
            field("primaryDepartmentId", "r.primary_department_id", ValueType.UUID),
            field("highestQualificationId", "r.highest_qualification_id", ValueType.UUID),
            field("qualificationDetail", "r.qualification_detail", ValueType.STRING),
            field("specialization", "r.specialization", ValueType.STRING),
            field("interests", "r.interests", ValueType.STRING),
            field("maxPeriodsPerWeek", "r.max_periods_per_week", ValueType.INTEGER),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StaffEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StaffEntity::getCreatedAt),
            Map.entry("updateTime", StaffEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.user_id, r.institution_id, r.employee_code, r.designation_id,
                   r.sub_designation, r.employment_type, r.joining_date, r.reporting_to_staff_id,
                   r.primary_department_id, r.highest_qualification_id, r.qualification_detail, r.specialization,
                   r.interests, r.max_periods_per_week, r.status, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM hr.staff r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO hr.staff
            (id, tenant_id, person_id, user_id, institution_id, employee_code, designation_id, sub_designation,
             employment_type, joining_date, reporting_to_staff_id, primary_department_id, highest_qualification_id,
             qualification_detail, specialization, interests, max_periods_per_week, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :personId, :userId, :institutionId, :employeeCode, :designationId,
                    :subDesignation, COALESCE(:employmentType, 'full_time'::character varying), :joiningDate,
                    :reportingToStaffId, :primaryDepartmentId, :highestQualificationId, :qualificationDetail,
                    :specialization, :interests, :maxPeriodsPerWeek, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, person_id, user_id, institution_id, employee_code, designation_id, sub_designation,
                      employment_type, joining_date, reporting_to_staff_id, primary_department_id,
                      highest_qualification_id, qualification_detail, specialization, interests, max_periods_per_week,
                      status, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE hr.staff
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE hr.staff
               SET person_id = :personId,
                   user_id = :userId,
                   institution_id = :institutionId,
                   employee_code = :employeeCode,
                   designation_id = :designationId,
                   sub_designation = :subDesignation,
                   employment_type = :employmentType,
                   joining_date = :joiningDate,
                   reporting_to_staff_id = :reportingToStaffId,
                   primary_department_id = :primaryDepartmentId,
                   highest_qualification_id = :highestQualificationId,
                   qualification_detail = :qualificationDetail,
                   specialization = :specialization,
                   interests = :interests,
                   max_periods_per_week = :maxPeriodsPerWeek,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** hr.ChangeStaffStatus; the service decides which moves are allowed. */
    private static final String TRANSITION_CHANGE_STATUS = """
            UPDATE hr.staff
               SET status = :status,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /**
     * hr.CreateStaffPortalAccount: only a staff member without a login account, who has not left, is given one — and
     * only as the service last read the row (its version).
     */
    private static final String ATTACH_ACCOUNT = """
            UPDATE hr.staff
               SET user_id = :userId,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND user_id IS NULL AND status <> 'exited'
               AND deleted_at IS NULL
            """;
    private static final String UQ_STAFF_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM hr.staff
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND person_id = :personId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String UQ_STAFF_2_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM hr.staff
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND user_id = :userId AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_DESIGNATION_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.designation WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_REPORTING_TO_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.staff WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LEAVE_APPLICATION_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_application WHERE tenant_id = :tenantId
                AND staff_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LEAVE_BALANCE_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.leave_balance WHERE tenant_id = :tenantId
                AND staff_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STAFF_REPORTING_TO_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.staff WHERE tenant_id = :tenantId
                AND reporting_to_staff_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STAFF_DEPARTMENT_STAFF_ID = """
            SELECT EXISTS (SELECT 1 FROM hr.staff_department WHERE tenant_id = :tenantId
                AND staff_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public StaffRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StaffEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StaffRepository::map).optional();
    }

    public Page<StaffEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StaffEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StaffRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StaffEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StaffEntity insert(StaffEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("userId", e.getUserId())
                .param("institutionId", e.getInstitutionId())
                .param("employeeCode", e.getEmployeeCode())
                .param("designationId", e.getDesignationId())
                .param("subDesignation", e.getSubDesignation())
                .param("employmentType", e.getEmploymentType())
                .param("joiningDate", e.getJoiningDate())
                .param("reportingToStaffId", e.getReportingToStaffId())
                .param("primaryDepartmentId", e.getPrimaryDepartmentId())
                .param("highestQualificationId", e.getHighestQualificationId())
                .param("qualificationDetail", e.getQualificationDetail())
                .param("specialization", e.getSpecialization())
                .param("interests", e.getInterests())
                .param("maxPeriodsPerWeek", e.getMaxPeriodsPerWeek())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StaffRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StaffEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("personId", e.getPersonId())
                .param("userId", e.getUserId())
                .param("institutionId", e.getInstitutionId())
                .param("employeeCode", e.getEmployeeCode())
                .param("designationId", e.getDesignationId())
                .param("subDesignation", e.getSubDesignation())
                .param("employmentType", e.getEmploymentType())
                .param("joiningDate", e.getJoiningDate())
                .param("reportingToStaffId", e.getReportingToStaffId())
                .param("primaryDepartmentId", e.getPrimaryDepartmentId())
                .param("highestQualificationId", e.getHighestQualificationId())
                .param("qualificationDetail", e.getQualificationDetail())
                .param("specialization", e.getSpecialization())
                .param("interests", e.getInterests())
                .param("maxPeriodsPerWeek", e.getMaxPeriodsPerWeek())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StaffEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** hr.ChangeStaffStatus; 0 rows means the row changed (or vanished) since it was read. */
    public int changeStatus(UUID id, int expected, String status, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CHANGE_STATUS)
                .param("id", id)
                .param("expected", expected)
                .param("status", status)
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    /**
     * Stores the login account of a staff member who has none and has not left, at the version the caller read; 0
     * rows means the row changed (or vanished, or the staff member left or has an account by now).
     */
    public int attachAccount(UUID id, int expected, UUID userId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(ATTACH_ACCOUNT)
                .param("id", id)
                .param("expected", expected)
                .param("userId", userId)
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    /** uq_staff_1: institution_id, person_id where (deleted_at IS NULL). */
    public boolean takenInstitutionIdPersonId1(StaffEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_STAFF_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("personId", e.getPersonId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** uq_staff_2: institution_id, user_id where (deleted_at IS NULL). */
    public boolean takenInstitutionIdUserId2(StaffEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_STAFF_2_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced hr.designation row exists (live). */
    public boolean existsDesignationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_DESIGNATION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced hr.staff row exists (live). */
    public boolean existsReportingToStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_REPORTING_TO_STAFF_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live hr.leave_application rows still reference the row. */
    public boolean referencedByLeaveApplicationStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEAVE_APPLICATION_STAFF_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live hr.leave_balance rows still reference the row. */
    public boolean referencedByLeaveBalanceStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEAVE_BALANCE_STAFF_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live hr.staff rows still reference the row. */
    public boolean referencedByStaffReportingToStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STAFF_REPORTING_TO_STAFF_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live hr.staff_department rows still reference the row. */
    public boolean referencedByStaffDepartmentStaffId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STAFF_DEPARTMENT_STAFF_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, StaffEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StaffEntity map(ResultSet rs, int row) throws SQLException {
        return StaffEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .employeeCode(rs.getString("employee_code"))
                .designationId(rs.getObject("designation_id", UUID.class))
                .subDesignation(rs.getString("sub_designation"))
                .employmentType(rs.getString("employment_type"))
                .joiningDate(rs.getObject("joining_date", LocalDate.class))
                .reportingToStaffId(rs.getObject("reporting_to_staff_id", UUID.class))
                .primaryDepartmentId(rs.getObject("primary_department_id", UUID.class))
                .highestQualificationId(rs.getObject("highest_qualification_id", UUID.class))
                .qualificationDetail(rs.getString("qualification_detail"))
                .specialization(rs.getString("specialization"))
                .interests(rs.getString("interests"))
                .maxPeriodsPerWeek(rs.getObject("max_periods_per_week", Integer.class))
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
