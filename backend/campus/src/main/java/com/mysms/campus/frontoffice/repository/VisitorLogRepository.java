package com.mysms.campus.frontoffice.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.campus.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.campus.frontoffice.domain.VisitorLogEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Plain-JDBC access to frontoffice.visitor_log: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class VisitorLogRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("visitorName", "r.visitor_name", ValueType.STRING),
            field("visitorType", "r.visitor_type", ValueType.STRING),
            field("email", "r.email", ValueType.STRING),
            field("phone", "r.phone", ValueType.STRING),
            field("companyName", "r.company_name", ValueType.STRING),
            field("address", "r.address", ValueType.STRING),
            field("studentId", "r.student_id", ValueType.UUID),
            field("relationToStudent", "r.relation_to_student", ValueType.STRING),
            field("relatedPersonName", "r.related_person_name", ValueType.STRING),
            field("visitorCount", "r.visitor_count", ValueType.INTEGER),
            field("purpose", "r.purpose", ValueType.STRING),
            field("hostStaffId", "r.host_staff_id", ValueType.UUID),
            field("visitDate", "r.visit_date", ValueType.DATE),
            field("remarks", "r.remarks", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<VisitorLogEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", VisitorLogEntity::getCreatedAt),
            Map.entry("updateTime", VisitorLogEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.visitor_name, r.visitor_type, r.email,
                   r.phone, r.company_name, r.address, r.student_id, r.relation_to_student, r.related_person_name,
                   r.visitor_count, r.purpose, r.host_staff_id, r.visit_date, r.entry_time, r.exit_time, r.remarks,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM frontoffice.visitor_log r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO frontoffice.visitor_log
            (id, tenant_id, institution_id, academic_year_id, visitor_name, visitor_type, email, phone, company_name,
             address, student_id, relation_to_student, related_person_name, visitor_count, purpose, host_staff_id,
             visit_date, entry_time, exit_time, remarks, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :visitorName, :visitorType, :email, :phone,
                    :companyName, :address, :studentId, :relationToStudent, :relatedPersonName,
                    COALESCE(:visitorCount, 1), :purpose, :hostStaffId, :visitDate, :entryTime, :exitTime, :remarks,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, visitor_name, visitor_type, email, phone,
                      company_name, address, student_id, relation_to_student, related_person_name, visitor_count,
                      purpose, host_staff_id, visit_date, entry_time, exit_time, remarks, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE frontoffice.visitor_log
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE frontoffice.visitor_log
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   visitor_name = :visitorName,
                   visitor_type = :visitorType,
                   email = :email,
                   phone = :phone,
                   company_name = :companyName,
                   address = :address,
                   student_id = :studentId,
                   relation_to_student = :relationToStudent,
                   related_person_name = :relatedPersonName,
                   visitor_count = :visitorCount,
                   purpose = :purpose,
                   host_staff_id = :hostStaffId,
                   visit_date = :visitDate,
                   entry_time = :entryTime,
                   exit_time = :exitTime,
                   remarks = :remarks,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String CHECK_OUT = """
            UPDATE frontoffice.visitor_log
               SET exit_time = :exitTime,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL AND exit_time IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public VisitorLogRepository(@Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<VisitorLogEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(VisitorLogRepository::map).optional();
    }

    public Page<VisitorLogEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<VisitorLogEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(VisitorLogRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), VisitorLogEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public VisitorLogEntity insert(VisitorLogEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("visitorName", e.getVisitorName())
                .param("visitorType", e.getVisitorType())
                .param("email", e.getEmail())
                .param("phone", e.getPhone())
                .param("companyName", e.getCompanyName())
                .param("address", e.getAddress())
                .param("studentId", e.getStudentId())
                .param("relationToStudent", e.getRelationToStudent())
                .param("relatedPersonName", e.getRelatedPersonName())
                .param("visitorCount", e.getVisitorCount())
                .param("purpose", e.getPurpose())
                .param("hostStaffId", e.getHostStaffId())
                .param("visitDate", e.getVisitDate())
                .param("entryTime", e.getEntryTime())
                .param("exitTime", e.getExitTime())
                .param("remarks", e.getRemarks())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(VisitorLogRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(VisitorLogEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("visitorName", e.getVisitorName())
                .param("visitorType", e.getVisitorType())
                .param("email", e.getEmail())
                .param("phone", e.getPhone())
                .param("companyName", e.getCompanyName())
                .param("address", e.getAddress())
                .param("studentId", e.getStudentId())
                .param("relationToStudent", e.getRelationToStudent())
                .param("relatedPersonName", e.getRelatedPersonName())
                .param("visitorCount", e.getVisitorCount())
                .param("purpose", e.getPurpose())
                .param("hostStaffId", e.getHostStaffId())
                .param("visitDate", e.getVisitDate())
                .param("entryTime", e.getEntryTime())
                .param("exitTime", e.getExitTime())
                .param("remarks", e.getRemarks())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Sets the exit time of a visitor still inside, under the version guard; 0 rows means it changed meanwhile. */
    public int checkOut(VisitorLogEntity e, int expectedVersion) {
        return jdbc.sql(CHECK_OUT)
                .param("id", e.getId())
                .param("exitTime", e.getExitTime())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(VisitorLogEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, VisitorLogEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static VisitorLogEntity map(ResultSet rs, int row) throws SQLException {
        return VisitorLogEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .visitorName(rs.getString("visitor_name"))
                .visitorType(rs.getString("visitor_type"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .companyName(rs.getString("company_name"))
                .address(rs.getString("address"))
                .studentId(rs.getObject("student_id", UUID.class))
                .relationToStudent(rs.getString("relation_to_student"))
                .relatedPersonName(rs.getString("related_person_name"))
                .visitorCount(rs.getObject("visitor_count", Integer.class))
                .purpose(rs.getString("purpose"))
                .hostStaffId(rs.getObject("host_staff_id", UUID.class))
                .visitDate(rs.getObject("visit_date", LocalDate.class))
                .entryTime(rs.getObject("entry_time", LocalTime.class))
                .exitTime(rs.getObject("exit_time", LocalTime.class))
                .remarks(rs.getString("remarks"))
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
