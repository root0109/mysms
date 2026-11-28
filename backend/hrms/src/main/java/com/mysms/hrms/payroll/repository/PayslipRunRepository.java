package com.mysms.hrms.payroll.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.id.UuidV7;
import com.mysms.hrms.hr.repository.WorkingDays;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.domain.PayslipEntity;
import com.mysms.hrms.payroll.domain.PayslipLineEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of the payslip workflow (payroll.GeneratePayslips, payroll.MarkPayslipPaid) that the standard
 * repositories of the tables do not have: which staff members a run pays, a staff member's salaries locked for the run,
 * their components with the pay component each stands for, the payslips a period would collide with, the document
 * numbers of the module's {@code number_sequence}, and the writes of a payslip with its lines and of the paid step.
 * Constant SQL, the tenant filtered on every table.
 */
@Repository
public class PayslipRunRepository {

    /** The number series of payslips, per institution. */
    public static final String PAYSLIP_NUMBERS = "payslip";
    /** The number series of the payments the paid step records, per institution. */
    public static final String PAYMENT_NUMBERS = "payroll_payment";

    /**
     * The staff members whose salary in force on the period's last day belongs to the institution (through its
     * salary template): the latest salary that has taken effect by then, optionally among the named staff only.
     */
    private static final String STAFF_PAID_BY = """
            SELECT s.staff_id
              FROM payroll.staff_salary s
              JOIN payroll.salary_template t ON t.tenant_id = :tenantId AND t.id = s.salary_template_id
             WHERE s.tenant_id = :tenantId
               AND s.deleted_at IS NULL AND s.effective_date <= :periodEnd
               AND t.institution_id = :institutionId
               AND (:everyone OR s.staff_id = ANY(:staffIds))
               AND NOT EXISTS (SELECT 1 FROM payroll.staff_salary n
                                WHERE n.tenant_id = :tenantId
                                  AND n.staff_id = s.staff_id AND n.deleted_at IS NULL
                                  AND n.effective_date <= :periodEnd AND n.effective_date > s.effective_date)
             ORDER BY s.staff_id
            """;
    /**
     * A staff member's salaries that have taken effect by the period's last day, oldest first, locked (the salary
     * rows only, not their templates): two runs for the same staff member wait for each other here, so the second
     * sees the payslip of the first. NO KEY UPDATE conflicts with itself but not with the key-share lock the
     * payslip's foreign key takes.
     */
    private static final String LOCK_SALARIES = """
            SELECT s.id, s.effective_date, t.institution_id
              FROM payroll.staff_salary s
              JOIN payroll.salary_template t ON t.tenant_id = :tenantId AND t.id = s.salary_template_id
             WHERE s.tenant_id = :tenantId
               AND s.id IN (SELECT l.id FROM payroll.staff_salary l
                             WHERE l.tenant_id = :tenantId
                               AND l.staff_id = :staffId AND l.deleted_at IS NULL
                               AND l.effective_date <= :periodEnd
                               FOR NO KEY UPDATE)
             ORDER BY s.effective_date
            """;
    private static final String COMPONENTS = """
            SELECT c.id, c.staff_salary_id, c.amount, p.code, p.name, p.component_type
              FROM payroll.staff_salary_component c
              JOIN payroll.salary_template_component tc
                ON tc.tenant_id = :tenantId AND tc.id = c.salary_template_component_id
              JOIN payroll.pay_component p ON p.tenant_id = :tenantId AND p.id = tc.pay_component_id
             WHERE c.tenant_id = :tenantId
               AND c.staff_salary_id = ANY(:staffSalaryIds) AND c.deleted_at IS NULL
             ORDER BY p.sort_order, p.code, c.id
            """;
    /** The periods of the staff member's payslips that count (not cancelled) and share a day with the period. */
    private static final String PERIODS_IN_THE_WAY = """
            SELECT r.period_start, r.period_end
              FROM payroll.payslip r
             WHERE r.tenant_id = :tenantId
               AND r.staff_id = :staffId AND r.deleted_at IS NULL AND r.status <> 'cancelled'
               AND r.period_start <= :periodEnd AND r.period_end >= :periodStart
            """;
    private static final String OPEN_SEQUENCE = """
            INSERT INTO payroll.number_sequence
            (id, tenant_id, institution_id, sequence_code, period_key, prefix, padding, next_value, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :sequenceCode, '', :prefix, 6, 1, :now, :actor, :now, :actor, 1)
            ON CONFLICT ON CONSTRAINT uq_payroll_number_sequence_1 DO NOTHING
            """;
    /** Issues the next number; the row stays locked until the issuing transaction ends, so a rollback returns it. */
    private static final String TAKE_NUMBER = """
            UPDATE payroll.number_sequence
               SET next_value = next_value + 1,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :institutionId AND sequence_code = :sequenceCode AND period_key = ''
               AND deleted_at IS NULL
            RETURNING COALESCE(prefix, '') || lpad((next_value - 1)::text, padding, '0')
            """;
    private static final String PAYSLIP_NUMBER_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.payslip
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND payslip_number = :number)
            """;
    private static final String PAYMENT_NUMBER_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.payroll_payment
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND transaction_number = :number)
            """;
    private static final String INSERT_PAYSLIP = """
            INSERT INTO payroll.payslip
            (id, tenant_id, institution_id, payslip_number, staff_id, staff_salary_id, period_start, period_end,
             pay_percentage, leave_days, leave_deduction, status, gross_amount, deduction_total, net_amount,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :payslipNumber, :staffId, :staffSalaryId, :periodStart,
                    :periodEnd, :payPercentage, :leaveDays, :leaveDeduction, 'unpaid', :grossAmount, :deductionTotal,
                    :netAmount, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    /** The lines of one payslip in one statement, in the order given. */
    private static final String INSERT_LINES = """
            INSERT INTO payroll.payslip_line
            (id, tenant_id, payslip_id, staff_salary_component_id, amount, component_code, component_name,
             component_type, created_at, created_by, updated_at, updated_by, row_version)
            SELECT line.id, :tenantId, :payslipId, line.staff_salary_component_id, line.amount, line.component_code,
                   line.component_name, line.component_type, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:staffSalaryComponentIds AS uuid[]),
                          CAST(:amounts AS numeric[]), CAST(:codes AS varchar[]), CAST(:names AS varchar[]),
                          CAST(:types AS varchar[]))
                   AS line (id, staff_salary_component_id, amount, component_code, component_name, component_type)
            """;
    /** payroll.MarkPayslipPaid: allowed while status is unpaid. */
    private static final String TRANSITION_MARK_PAYSLIP_PAID = """
            UPDATE payroll.payslip
               SET status = 'paid',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'unpaid' AND deleted_at IS NULL
            """;
    private static final String DISBURSEMENT_ACCOUNT_INSTITUTION = """
            SELECT institution_id
              FROM payroll.disbursement_account
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;

    /**
     * The working days of a span of a pay period at the institution (hrms's {@link WorkingDays} rule), and how much
     * of them the staff member's approved leave of unpaid types takes (hr.leave_application, hr.leave_type: the sibling
     * area's tables): a full day 1, a half day 0.5, at most 1 a day however many applications cover it. Leave days are
     * the UTC days of the application, as hr counts them.
     */
    private static final String ABSENCE = """
            SELECT count(*) AS working,
                   COALESCE(sum((SELECT LEAST(1, COALESCE(sum(CASE WHEN a.day_part = 'full_day' THEN 1
                                                                    ELSE 0.5 END), 0))
                                   FROM hr.leave_application a
                                   JOIN hr.leave_type t ON t.tenant_id = :tenantId AND t.id = a.leave_type_id
                                  WHERE a.tenant_id = :tenantId AND a.staff_id = :staffId AND a.deleted_at IS NULL
                                    AND a.status IN ('approved', 'completed') AND NOT t.is_paid
                                    AND wd.day BETWEEN (a.from_at AT TIME ZONE 'UTC')::date
                                                   AND (a.to_at AT TIME ZONE 'UTC')::date)), 0) AS unpaid
              FROM (SELECT g.day::date AS day, CAST(:institutionId AS uuid) AS institution_id
                      FROM generate_series(CAST(:fromDay AS timestamp), CAST(:toDay AS timestamp),
                                           interval '1 day') AS g(day)) wd
             WHERE
            """ + WorkingDays.IS_WORKING_DAY;

    private final JdbcClient jdbc;
    private final WorkingDays workingDays;

    public PayslipRunRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            WorkingDays workingDays) {
        this.jdbc = database.jdbc();
        this.workingDays = workingDays;
    }

    /** The working days of a span and the unpaid leave days among them. */
    public record Absence(BigDecimal workingDays, BigDecimal unpaidDays) {
    }

    /** A staff salary of the run (a projection), with the institution of its template. */
    public record SalaryRow(UUID id, LocalDate effectiveDate, UUID institutionId) {
    }

    /** A staff salary component with the pay component it stands for. */
    public record ComponentRow(UUID id, UUID staffSalaryId, BigDecimal amount, String code, String name,
                               String type) {
    }

    /** The period of a payslip. */
    public record Period(LocalDate start, LocalDate end) {
    }

    /** The staff members a run for the institution pays, in a stable order; {@code staffIds} null means everyone. */
    public List<UUID> staffPaidBy(UUID institutionId, LocalDate periodEnd, Collection<UUID> staffIds) {
        return jdbc.sql(STAFF_PAID_BY)
                .param("institutionId", institutionId)
                .param("periodEnd", periodEnd)
                .param("everyone", staffIds == null)
                .param("staffIds", staffIds == null ? new UUID[0] : staffIds.toArray(UUID[]::new))
                .query((rs, row) -> rs.getObject("staff_id", UUID.class)).list();
    }

    public List<SalaryRow> lockSalaries(UUID staffId, LocalDate periodEnd) {
        return jdbc.sql(LOCK_SALARIES).param("staffId", staffId).param("periodEnd", periodEnd)
                .query((rs, row) -> new SalaryRow(rs.getObject("id", UUID.class),
                        rs.getObject("effective_date", LocalDate.class), rs.getObject("institution_id", UUID.class)))
                .list();
    }

    /** The working days from {@code from} to {@code to} (both counted) and the staff member's unpaid leave in them. */
    public Absence absence(UUID staffId, UUID institutionId, LocalDate from, LocalDate to) {
        return jdbc.sql(ABSENCE).param("staffId", staffId).param("institutionId", institutionId)
                .param("fromDay", from).param("toDay", to).param(WorkingDays.PARAMETER, workingDays.weekendDays())
                .query((rs, row) -> new Absence(rs.getBigDecimal("working"), rs.getBigDecimal("unpaid"))).single();
    }

    /** The live components of these staff salaries, in the pay components' display order. */
    public List<ComponentRow> components(Collection<UUID> staffSalaryIds) {
        return jdbc.sql(COMPONENTS).param("staffSalaryIds", staffSalaryIds.toArray(UUID[]::new))
                .query((rs, row) -> new ComponentRow(rs.getObject("id", UUID.class),
                        rs.getObject("staff_salary_id", UUID.class), rs.getBigDecimal("amount"), rs.getString("code"),
                        rs.getString("name"), rs.getString("component_type")))
                .list();
    }

    public List<Period> periodsInTheWay(UUID staffId, LocalDate periodStart, LocalDate periodEnd) {
        return jdbc.sql(PERIODS_IN_THE_WAY)
                .param("staffId", staffId)
                .param("periodStart", periodStart)
                .param("periodEnd", periodEnd)
                .query((rs, row) -> new Period(rs.getObject("period_start", LocalDate.class),
                        rs.getObject("period_end", LocalDate.class)))
                .list();
    }

    /**
     * The next free document number of the institution's series (opened on first use with the prefix). A number
     * someone already gave a row by hand is passed over.
     */
    public String nextNumber(UUID institutionId, String sequenceCode, String prefix, OffsetDateTime now, UUID actor) {
        jdbc.sql(OPEN_SEQUENCE)
                .param("id", UuidV7.next())
                .param("institutionId", institutionId)
                .param("sequenceCode", sequenceCode)
                .param("prefix", prefix)
                .param("now", now)
                .param("actor", actor)
                .update();
        String taken = PAYSLIP_NUMBERS.equals(sequenceCode) ? PAYSLIP_NUMBER_TAKEN : PAYMENT_NUMBER_TAKEN;
        while (true) {
            String number = jdbc.sql(TAKE_NUMBER)
                    .param("institutionId", institutionId)
                    .param("sequenceCode", sequenceCode)
                    .param("now", now)
                    .param("actor", actor)
                    .query(String.class).single();
            if (!Boolean.TRUE.equals(jdbc.sql(taken).param("institutionId", institutionId).param("number", number)
                    .query(Boolean.class).single())) {
                return number;
            }
        }
    }

    /** Inserts a generated payslip (status unpaid) with its totals. */
    public void insertPayslip(PayslipEntity e) {
        jdbc.sql(INSERT_PAYSLIP)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("payslipNumber", e.getPayslipNumber())
                .param("staffId", e.getStaffId())
                .param("staffSalaryId", e.getStaffSalaryId())
                .param("periodStart", e.getPeriodStart())
                .param("periodEnd", e.getPeriodEnd())
                .param("payPercentage", e.getPayPercentage())
                .param("leaveDays", e.getLeaveDays())
                .param("leaveDeduction", e.getLeaveDeduction())
                .param("grossAmount", e.getGrossAmount())
                .param("deductionTotal", e.getDeductionTotal())
                .param("netAmount", e.getNetAmount())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Inserts the lines of one payslip, which carry the same stamps as the payslip. */
    public void insertLines(PayslipEntity payslip, List<PayslipLineEntity> lines) {
        jdbc.sql(INSERT_LINES)
                .param("payslipId", payslip.getId())
                .param("now", payslip.getCreatedAt())
                .param("actor", payslip.getCreatedBy())
                .param("ids", lines.stream().map(PayslipLineEntity::getId).toArray(UUID[]::new))
                .param("staffSalaryComponentIds",
                        lines.stream().map(PayslipLineEntity::getStaffSalaryComponentId).toArray(UUID[]::new))
                .param("amounts", lines.stream().map(line -> line.getAmount().toPlainString()).toArray(String[]::new))
                .param("codes", lines.stream().map(PayslipLineEntity::getComponentCode).toArray(String[]::new))
                .param("names", lines.stream().map(PayslipLineEntity::getComponentName).toArray(String[]::new))
                .param("types", lines.stream().map(PayslipLineEntity::getComponentType).toArray(String[]::new))
                .update();
    }

    /** payroll.MarkPayslipPaid; 0 rows means the row changed (or left the unpaid state) since it was read. */
    public int markPayslipPaid(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_MARK_PAYSLIP_PAID)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** The institution of a live disbursement account. */
    public Optional<UUID> institutionOfDisbursementAccount(UUID id) {
        return jdbc.sql(DISBURSEMENT_ACCOUNT_INSTITUTION).param("id", id)
                .query((rs, row) -> rs.getObject("institution_id", UUID.class)).optional();
    }
}
