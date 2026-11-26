package com.mysms.hrms.payroll.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.domain.StaffSalaryEntity;
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
 * Plain-JDBC access to payroll.staff_salary: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class StaffSalaryRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("salaryTemplateId", "r.salary_template_id", ValueType.UUID),
            field("grossSalary", "r.gross_salary", ValueType.DECIMAL),
            field("effectiveDate", "r.effective_date", ValueType.DATE),
            field("remarks", "r.remarks", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StaffSalaryEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StaffSalaryEntity::getCreatedAt),
            Map.entry("updateTime", StaffSalaryEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.staff_id, r.salary_template_id, r.gross_salary, r.effective_date, r.remarks,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM payroll.staff_salary r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO payroll.staff_salary
            (id, tenant_id, staff_id, salary_template_id, gross_salary, effective_date, remarks, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :staffId, :salaryTemplateId, :grossSalary, :effectiveDate, :remarks, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, staff_id, salary_template_id, gross_salary, effective_date, remarks, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE payroll.staff_salary
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE payroll.staff_salary
               SET staff_id = :staffId,
                   salary_template_id = :salaryTemplateId,
                   gross_salary = :grossSalary,
                   effective_date = :effectiveDate,
                   remarks = :remarks,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_STAFF_SALARY_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.staff_salary
                            WHERE tenant_id = :tenantId
                              AND staff_id = :staffId AND effective_date = :effectiveDate AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_SALARY_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.salary_template WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PAYSLIP_STAFF_SALARY_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.payslip WHERE tenant_id = :tenantId
                AND staff_salary_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STAFF_SALARY_COMPONENT_STAFF_SALARY_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.staff_salary_component WHERE tenant_id = :tenantId
                AND staff_salary_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM payroll.salary_template p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.salary_template_id IN (SELECT p0.id FROM payroll.salary_template p0 WHERE p0.tenant_id =
                 :tenantId
                 AND
             (p0.institution_id = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public StaffSalaryRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StaffSalaryEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StaffSalaryRepository::map).optional();
    }

    public Page<StaffSalaryEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StaffSalaryEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StaffSalaryRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StaffSalaryEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StaffSalaryEntity insert(StaffSalaryEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("staffId", e.getStaffId())
                .param("salaryTemplateId", e.getSalaryTemplateId())
                .param("grossSalary", e.getGrossSalary())
                .param("effectiveDate", e.getEffectiveDate())
                .param("remarks", e.getRemarks())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StaffSalaryRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StaffSalaryEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("staffId", e.getStaffId())
                .param("salaryTemplateId", e.getSalaryTemplateId())
                .param("grossSalary", e.getGrossSalary())
                .param("effectiveDate", e.getEffectiveDate())
                .param("remarks", e.getRemarks())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StaffSalaryEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_staff_salary_1: staff_id, effective_date where (deleted_at IS NULL). */
    public boolean takenStaffIdEffectiveDate1(StaffSalaryEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_STAFF_SALARY_1_TAKEN)
                .param("staffId", e.getStaffId())
                .param("effectiveDate", e.getEffectiveDate())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced payroll.salary_template row exists (live). */
    public boolean existsSalaryTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SALARY_TEMPLATE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live payroll.payslip rows still reference the row. */
    public boolean referencedByPayslipStaffSalaryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PAYSLIP_STAFF_SALARY_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live payroll.staff_salary_component rows still reference the row. */
    public boolean referencedByStaffSalaryComponentStaffSalaryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STAFF_SALARY_COMPONENT_STAFF_SALARY_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through salary_template_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, StaffSalaryEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StaffSalaryEntity map(ResultSet rs, int row) throws SQLException {
        return StaffSalaryEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .salaryTemplateId(rs.getObject("salary_template_id", UUID.class))
                .grossSalary(rs.getBigDecimal("gross_salary"))
                .effectiveDate(rs.getObject("effective_date", LocalDate.class))
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
