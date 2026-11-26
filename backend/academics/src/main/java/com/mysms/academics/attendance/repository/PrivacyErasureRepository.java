package com.mysms.academics.attendance.repository;

/**
 * The statements of attendance's privacy tasks and retention sweep (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10), run
 * by its {@code AttendancePrivacyTaskHandler} through privacy-client. attendance knows a data principal through the
 * person of each staff and student record (attendance.member_record, kept from hr's and sis's events). An erasure
 * keeps the registers under attendance.after_leaving and counts the subject's rows (soft-deleted ones included); the
 * sweep anonymises the student records of the students gone before the cutoff.
 */
public final class PrivacyErasureRepository {

    /** The subject's attendance as a student, kept under attendance.after_leaving. */
    public static final String ATTENDANCE_RECORD_KEPT = """
            SELECT count(*)
              FROM attendance.attendance_record r
              JOIN attendance.member_record m ON m.record_id = r.student_id AND m.tenant_id = :tenantId
                   AND m.record_type = 'student'
             WHERE r.tenant_id = :tenantId AND m.person_id = :subjectId
            """;
    /** The subject's attendance as staff, kept under attendance.after_leaving. */
    public static final String STAFF_ATTENDANCE_KEPT = """
            SELECT count(*)
              FROM attendance.staff_attendance a
              JOIN attendance.member_record m ON m.record_id = a.staff_id AND m.tenant_id = :tenantId
                   AND m.record_type = 'staff'
             WHERE a.tenant_id = :tenantId AND m.person_id = :subjectId
            """;

    /**
     * attendance.after_leaving: the persons whose student attendance all lies before the cutoff — a student not marked
     * since the cutoff is taken to have left by then (attendance holds no leaving date).
     */
    public static final String AFTER_LEAVING_SUBJECTS = """
            SELECT m.person_id
              FROM attendance.member_record m
              JOIN attendance.attendance_record r ON r.student_id = m.record_id AND r.tenant_id = :tenantId
             WHERE m.tenant_id = :tenantId AND m.record_type = 'student' AND m.person_id > :after
             GROUP BY m.person_id
            HAVING max(r.attendance_date) < :cutoff
             ORDER BY m.person_id
             LIMIT :limit
            """;
    /** One subject's student attendance due under attendance.after_leaving. */
    public static final String AFTER_LEAVING_ROWS = """
            SELECT r.id
              FROM attendance.attendance_record r
              JOIN attendance.member_record m ON m.record_id = r.student_id AND m.tenant_id = :tenantId
                   AND m.record_type = 'student'
             WHERE r.tenant_id = :tenantId AND m.person_id = :subjectId AND r.attendance_date < :cutoff
             ORDER BY r.id
            """;
    /**
     * The mark stays in the register for its counts; whose it was goes: the student becomes a placeholder derived from
     * the row's own id (distinct per row, so the sheet's one-mark-per-student rule holds), the remark and the leave
     * request are cleared.
     */
    public static final String ANONYMISE_ATTENDANCE_RECORD = """
            UPDATE attendance.attendance_record
               SET student_id = md5('mysms.erased:' || id::text)::uuid, remark = NULL, leave_request_id = NULL,
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
