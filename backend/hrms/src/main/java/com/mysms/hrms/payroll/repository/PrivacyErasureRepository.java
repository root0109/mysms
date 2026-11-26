package com.mysms.hrms.payroll.repository;

/**
 * The statements of payroll's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code PayrollPrivacyTaskHandler} through privacy-client. payroll knows a data principal through the person of each
 * staff record (hr.staff, its sibling area's table). The subject's bank accounts are erased (soft-deleted
 * rows included, since soft delete is not erasure); the wage records — salary structures, payslips and their lines,
 * payments — are kept under payroll.after_leaving and only counted.
 */
public final class PrivacyErasureRepository {

    public static final String BANK_ACCOUNT_ROWS = """
            SELECT b.id
              FROM payroll.staff_bank_account b
              JOIN hr.staff m ON m.id = b.staff_id AND m.tenant_id = :tenantId
             WHERE b.tenant_id = :tenantId AND m.person_id = :subjectId
             ORDER BY b.id
            """;
    /** The holder's name gets a placeholder (the column is required); the account number's envelope is cleared. */
    public static final String ERASE_BANK_ACCOUNT = """
            UPDATE payroll.staff_bank_account
               SET account_holder_name = 'Erased', account_number_enc = '', account_number_masked = NULL,
                   ifsc_code = NULL, bank_name = NULL, branch_name = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The subject's salary structures, kept under payroll.after_leaving. */
    public static final String SALARY_KEPT = """
            SELECT count(*)
              FROM payroll.staff_salary s
              JOIN hr.staff m ON m.id = s.staff_id AND m.tenant_id = :tenantId
             WHERE s.tenant_id = :tenantId AND m.person_id = :subjectId
            """;
    /** The components of the subject's salary structures, kept under payroll.after_leaving. */
    public static final String SALARY_COMPONENT_KEPT = """
            SELECT count(*)
              FROM payroll.staff_salary_component c
              JOIN payroll.staff_salary s ON s.id = c.staff_salary_id AND s.tenant_id = :tenantId
              JOIN hr.staff m ON m.id = s.staff_id AND m.tenant_id = :tenantId
             WHERE c.tenant_id = :tenantId AND m.person_id = :subjectId
            """;
    /** The subject's payslips, kept under payroll.after_leaving. */
    public static final String PAYSLIP_KEPT = """
            SELECT count(*)
              FROM payroll.payslip p
              JOIN hr.staff m ON m.id = p.staff_id AND m.tenant_id = :tenantId
             WHERE p.tenant_id = :tenantId AND m.person_id = :subjectId
            """;
    /** The lines of the subject's payslips, kept under payroll.after_leaving. */
    public static final String PAYSLIP_LINE_KEPT = """
            SELECT count(*)
              FROM payroll.payslip_line l
              JOIN payroll.payslip p ON p.id = l.payslip_id AND p.tenant_id = :tenantId
              JOIN hr.staff m ON m.id = p.staff_id AND m.tenant_id = :tenantId
             WHERE l.tenant_id = :tenantId AND m.person_id = :subjectId
            """;
    /** The salary payments to the subject, kept under payroll.after_leaving. */
    public static final String PAYMENT_KEPT = """
            SELECT count(*)
              FROM payroll.payroll_payment p
              JOIN hr.staff m ON m.id = p.staff_id AND m.tenant_id = :tenantId
             WHERE p.tenant_id = :tenantId AND m.person_id = :subjectId
            """;

    private PrivacyErasureRepository() {
    }
}
