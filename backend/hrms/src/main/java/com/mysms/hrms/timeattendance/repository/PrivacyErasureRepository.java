package com.mysms.hrms.timeattendance.repository;

/**
 * The statements of time_attendance's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code TimeAttendancePrivacyTaskHandler} through privacy-client. time_attendance knows a data principal through
 * the person of each staff record (hr.staff, its sibling area's table). The register is kept under
 * time_attendance.after_leaving: an erasure changes nothing and answers the subject's attendance days as kept,
 * counted with the policy code. There is no sweep: payroll's wage records (payroll.after_leaving) outlive it.
 */
public final class PrivacyErasureRepository {

    /** The subject's attendance days as staff, kept under time_attendance.after_leaving. */
    public static final String STAFF_ATTENDANCE_KEPT = """
            SELECT count(*)
              FROM time_attendance.staff_attendance a
              JOIN hr.staff m ON m.id = a.staff_id AND m.tenant_id = :tenantId
             WHERE a.tenant_id = :tenantId AND m.person_id = :subjectId
            """;

    private PrivacyErasureRepository() {
    }
}
