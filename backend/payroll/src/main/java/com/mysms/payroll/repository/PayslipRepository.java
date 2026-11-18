package com.mysms.payroll.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.domain.PayslipEntity;
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
 * Plain-JDBC access to payroll.payslip: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PayslipRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("grossAmount", "r.gross_amount", ValueType.DECIMAL),
            field("deductionTotal", "r.deduction_total", ValueType.DECIMAL),
            field("netAmount", "r.net_amount", ValueType.DECIMAL),
            field("documentFileId", "r.document_file_id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("payslipNumber", "r.payslip_number", ValueType.STRING),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("staffSalaryId", "r.staff_salary_id", ValueType.UUID),
            field("periodStart", "r.period_start", ValueType.DATE),
            field("periodEnd", "r.period_end", ValueType.DATE),
            field("payPercentage", "r.pay_percentage", ValueType.DECIMAL),
            field("leaveDays", "r.leave_days", ValueType.DECIMAL),
            field("lateCount", "r.late_count", ValueType.INTEGER),
            field("leaveDeduction", "r.leave_deduction", ValueType.DECIMAL),
            field("status", "r.status", ValueType.STRING),
            field("remarks", "r.remarks", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PayslipEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PayslipEntity::getCreatedAt),
            Map.entry("updateTime", PayslipEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.gross_amount, r.deduction_total, r.net_amount, r.document_file_id,
                   r.institution_id, r.payslip_number, r.staff_id, r.staff_salary_id, r.period_start, r.period_end,
                   r.pay_percentage, r.leave_days, r.late_count, r.leave_deduction, r.status, r.remarks, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM payroll.payslip r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO payroll.payslip
            (id, tenant_id, institution_id, payslip_number, staff_id, staff_salary_id, period_start, period_end,
             pay_percentage, leave_days, late_count, leave_deduction, remarks, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :payslipNumber, :staffId, :staffSalaryId, :periodStart,
                    :periodEnd, COALESCE(:payPercentage, 100), COALESCE(:leaveDays, 0), :lateCount,
                    COALESCE(:leaveDeduction, 0), :remarks, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, gross_amount, deduction_total, net_amount, document_file_id, institution_id,
                      payslip_number, staff_id, staff_salary_id, period_start, period_end, pay_percentage, leave_days,
                      late_count, leave_deduction, status, remarks, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE payroll.payslip
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE payroll.payslip
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PAYSLIP_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.payslip
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND payslip_number = :payslipNumber AND id <> :id)
            """;
    private static final String EXISTS_STAFF_SALARY_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.staff_salary WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PAYROLL_PAYMENT_PAYSLIP_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.payroll_payment WHERE tenant_id = :tenantId
                AND payslip_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PAYSLIP_LINE_PAYSLIP_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.payslip_line WHERE tenant_id = :tenantId
                AND payslip_id = :id AND deleted_at IS NULL)
            """;

    /** payroll.CancelPayslip: allowed while status is unpaid. */
    private static final String TRANSITION_CANCEL_PAYSLIP = """
            UPDATE payroll.payslip
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    /**
     * Owner-only reads: the payslips of the institutions where the reader is a payroll officer, and the payslips of
     * the reader's own staff records (payroll.staff_member, kept from hr's events).
     */
    private static final String READABLE_BY = """
             (:officerAll OR r.institution_id = ANY(:officerIds) OR r.staff_id IN (SELECT m.staff_id
                 FROM payroll.staff_member m WHERE m.tenant_id = :tenantId AND NOT m.is_deleted
                  AND m.user_id = :reader))
            """;

    private final JdbcClient jdbc;

    public PayslipRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PayslipEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PayslipRepository::map).optional();
    }

    public Page<PayslipEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PayslipEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PayslipRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PayslipEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PayslipEntity insert(PayslipEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("payslipNumber", e.getPayslipNumber())
                .param("staffId", e.getStaffId())
                .param("staffSalaryId", e.getStaffSalaryId())
                .param("periodStart", e.getPeriodStart())
                .param("periodEnd", e.getPeriodEnd())
                .param("payPercentage", e.getPayPercentage())
                .param("leaveDays", e.getLeaveDays())
                .param("lateCount", e.getLateCount())
                .param("leaveDeduction", e.getLeaveDeduction())
                .param("remarks", e.getRemarks())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PayslipRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PayslipEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PayslipEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_payslip_1: institution_id, payslip_number. */
    public boolean takenInstitutionIdPayslipNumber1(PayslipEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PAYSLIP_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("payslipNumber", e.getPayslipNumber())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced payroll.staff_salary row exists (live). */
    public boolean existsStaffSalaryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STAFF_SALARY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live payroll.payroll_payment rows still reference the row. */
    public boolean referencedByPayrollPaymentPayslipId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PAYROLL_PAYMENT_PAYSLIP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live payroll.payslip_line rows still reference the row. */
    public boolean referencedByPayslipLinePayslipId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PAYSLIP_LINE_PAYSLIP_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** payroll.CancelPayslip; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelPayslip(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_PAYSLIP)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    /** The payslips the reader may read: see {@code READABLE_BY}; {@code officer} is where the reader is an officer. */
    public static SqlFragment readableBy(DataScope officer, UUID reader) {
        return new SqlFragment(READABLE_BY, Map.of("officerAll", officer.all(), "officerIds", officer.institutionArray(),
                "reader", reader));
    }

    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, PayslipEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PayslipEntity map(ResultSet rs, int row) throws SQLException {
        return PayslipEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .grossAmount(rs.getBigDecimal("gross_amount"))
                .deductionTotal(rs.getBigDecimal("deduction_total"))
                .netAmount(rs.getBigDecimal("net_amount"))
                .documentFileId(rs.getObject("document_file_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .payslipNumber(rs.getString("payslip_number"))
                .staffId(rs.getObject("staff_id", UUID.class))
                .staffSalaryId(rs.getObject("staff_salary_id", UUID.class))
                .periodStart(rs.getObject("period_start", LocalDate.class))
                .periodEnd(rs.getObject("period_end", LocalDate.class))
                .payPercentage(rs.getBigDecimal("pay_percentage"))
                .leaveDays(rs.getBigDecimal("leave_days"))
                .lateCount(rs.getObject("late_count", Integer.class))
                .leaveDeduction(rs.getBigDecimal("leave_deduction"))
                .status(rs.getString("status"))
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
