package com.mysms.admission.repository;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.domain.AdmissionCycleEntity;
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
 * Plain-JDBC access to admission.admission_cycle (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class AdmissionCycleRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("admissionBasis", "r.admission_basis", ValueType.STRING),
            field("programId", "r.program_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("gradeLevelId", "r.grade_level_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("academicTermId", "r.academic_term_id", ValueType.UUID),
            field("startDate", "r.start_date", ValueType.DATE),
            field("endDate", "r.end_date", ValueType.DATE),
            field("minApplications", "r.min_applications", ValueType.INTEGER),
            field("maxApplications", "r.max_applications", ValueType.INTEGER),
            field("minimumAgeYears", "r.minimum_age_years", ValueType.INTEGER),
            field("applicationFeeHeadId", "r.application_fee_head_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AdmissionCycleEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AdmissionCycleEntity::getCreatedAt),
            Map.entry("updateTime", AdmissionCycleEntity::getUpdatedAt),
            Map.entry("name", AdmissionCycleEntity::getName),
            Map.entry("startDate", AdmissionCycleEntity::getStartDate));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.name, r.admission_basis, r.program_id, r.course_id,
                   r.grade_level_id, r.academic_year_id, r.academic_term_id, r.start_date, r.end_date,
                   r.min_applications, r.max_applications, r.minimum_age_years, r.application_fee_head_id, r.status,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM admission.admission_cycle r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO admission.admission_cycle
            (id, tenant_id, institution_id, name, admission_basis, program_id, course_id, grade_level_id,
             academic_year_id, academic_term_id, start_date, end_date, min_applications, max_applications,
             minimum_age_years, application_fee_head_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :name, COALESCE(:admissionBasis, 'course'::character varying),
                    :programId, :courseId, :gradeLevelId, :academicYearId, :academicTermId, :startDate, :endDate,
                    :minApplications, :maxApplications, :minimumAgeYears, :applicationFeeHeadId, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, name, admission_basis, program_id, course_id, grade_level_id,
                      academic_year_id, academic_term_id, start_date, end_date, min_applications, max_applications,
                      minimum_age_years, application_fee_head_id, status, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE admission.admission_cycle
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE admission.admission_cycle
               SET institution_id = :institutionId,
                   name = :name,
                   admission_basis = :admissionBasis,
                   program_id = :programId,
                   course_id = :courseId,
                   grade_level_id = :gradeLevelId,
                   academic_year_id = :academicYearId,
                   academic_term_id = :academicTermId,
                   start_date = :startDate,
                   end_date = :endDate,
                   min_applications = :minApplications,
                   max_applications = :maxApplications,
                   minimum_age_years = :minimumAgeYears,
                   application_fee_head_id = :applicationFeeHeadId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_ADMISSION_CYCLE_FEE_ADMISSION_CYCLE_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.admission_cycle_fee WHERE tenant_id = :tenantId
                AND admission_cycle_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_APPLICATION_ADMISSION_CYCLE_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.application WHERE tenant_id = :tenantId
                AND admission_cycle_id = :id AND deleted_at IS NULL)
            """;

    /** admission.ConfirmAdmissionCycle: allowed while status is draft. */
    private static final String TRANSITION_CONFIRM_ADMISSION_CYCLE = """
            UPDATE admission.admission_cycle
               SET status = 'confirmed',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** admission.CloseAdmissionCycle: allowed while status is open or admission. */
    private static final String TRANSITION_CLOSE_ADMISSION_CYCLE = """
            UPDATE admission.admission_cycle
               SET status = 'closed',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** admission.CancelAdmissionCycle: allowed while status is draft or confirmed or open. */
    private static final String TRANSITION_CANCEL_ADMISSION_CYCLE = """
            UPDATE admission.admission_cycle
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** admission.ResetAdmissionCycle: allowed while status is cancelled. */
    private static final String TRANSITION_RESET_ADMISSION_CYCLE = """
            UPDATE admission.admission_cycle
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

    public AdmissionCycleRepository(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AdmissionCycleEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AdmissionCycleRepository::map).optional();
    }

    public Page<AdmissionCycleEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AdmissionCycleEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AdmissionCycleRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AdmissionCycleEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AdmissionCycleEntity insert(AdmissionCycleEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
                .param("admissionBasis", e.getAdmissionBasis())
                .param("programId", e.getProgramId())
                .param("courseId", e.getCourseId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("academicYearId", e.getAcademicYearId())
                .param("academicTermId", e.getAcademicTermId())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("minApplications", e.getMinApplications())
                .param("maxApplications", e.getMaxApplications())
                .param("minimumAgeYears", e.getMinimumAgeYears())
                .param("applicationFeeHeadId", e.getApplicationFeeHeadId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AdmissionCycleRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AdmissionCycleEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
                .param("admissionBasis", e.getAdmissionBasis())
                .param("programId", e.getProgramId())
                .param("courseId", e.getCourseId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("academicYearId", e.getAcademicYearId())
                .param("academicTermId", e.getAcademicTermId())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("minApplications", e.getMinApplications())
                .param("maxApplications", e.getMaxApplications())
                .param("minimumAgeYears", e.getMinimumAgeYears())
                .param("applicationFeeHeadId", e.getApplicationFeeHeadId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AdmissionCycleEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live admission.admission_cycle_fee rows still reference the row. */
    public boolean referencedByAdmissionCycleFeeAdmissionCycleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ADMISSION_CYCLE_FEE_ADMISSION_CYCLE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live admission.application rows still reference the row. */
    public boolean referencedByApplicationAdmissionCycleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_APPLICATION_ADMISSION_CYCLE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** admission.ConfirmAdmissionCycle; 0 rows means the row changed (or vanished) since it was read. */
    public int confirmAdmissionCycle(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CONFIRM_ADMISSION_CYCLE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** admission.CloseAdmissionCycle; 0 rows means the row changed (or vanished) since it was read. */
    public int closeAdmissionCycle(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CLOSE_ADMISSION_CYCLE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** admission.CancelAdmissionCycle; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelAdmissionCycle(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_ADMISSION_CYCLE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** admission.ResetAdmissionCycle; 0 rows means the row changed (or vanished) since it was read. */
    public int resetAdmissionCycle(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESET_ADMISSION_CYCLE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, AdmissionCycleEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AdmissionCycleEntity map(ResultSet rs, int row) throws SQLException {
        return AdmissionCycleEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .name(rs.getString("name"))
                .admissionBasis(rs.getString("admission_basis"))
                .programId(rs.getObject("program_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .gradeLevelId(rs.getObject("grade_level_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .academicTermId(rs.getObject("academic_term_id", UUID.class))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
                .minApplications(rs.getObject("min_applications", Integer.class))
                .maxApplications(rs.getObject("max_applications", Integer.class))
                .minimumAgeYears(rs.getObject("minimum_age_years", Integer.class))
                .applicationFeeHeadId(rs.getObject("application_fee_head_id", UUID.class))
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
