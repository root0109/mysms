package com.mysms.hr.repository;

/**
 * The statements of hr's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its {@code HrPrivacyTaskHandler}
 * through privacy-client: per table, the ids of a subject's rows (as a staff member; soft-deleted rows included, since
 * soft delete is not erasure) and the statement that anonymises one row. The employment record (employee code, dates,
 * designation, departments, subjects) and the leave ledger (applications' dates and status, balances) stay, since
 * payroll and attendance refer to them; what describes the person beyond it is cleared.
 */
public final class PrivacyErasureRepository {

    public static final String LEAVE_APPLICATION_ROWS = """
            SELECT l.id
              FROM hr.leave_application l
              JOIN hr.staff s ON s.id = l.staff_id AND s.tenant_id = :tenantId
             WHERE l.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY l.id
            """;
    /** The leave stays in the ledger; the applicant's and the decider's free text goes. */
    public static final String ANONYMISE_LEAVE_APPLICATION = """
            UPDATE hr.leave_application
               SET remarks = NULL, decision_comments = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String STAFF_ROWS = """
            SELECT id FROM hr.staff WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** The employment record stays; the person's qualifications and interests go. */
    public static final String ANONYMISE_STAFF = """
            UPDATE hr.staff
               SET sub_designation = NULL, highest_qualification_id = NULL, qualification_detail = NULL,
                   specialization = NULL, interests = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
