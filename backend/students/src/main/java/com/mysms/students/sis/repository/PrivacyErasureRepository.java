package com.mysms.students.sis.repository;

/**
 * The statements of sis's privacy tasks and retention sweep (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10), run by its
 * {@code SisPrivacyTaskHandler} through privacy-client: per table, the ids of a subject's rows (as a student or as a
 * guardian; soft-deleted rows included, since soft delete is not erasure) and the statement that erases one row. The
 * admission register (the student row's number and dates, enrolments, promotions) is kept; what describes the subject
 * beyond it is cleared.
 */
public final class PrivacyErasureRepository {

    public static final String HEALTH_RECORD_ROWS = """
            SELECT h.id
              FROM sis.student_health_record h
              JOIN sis.student s ON s.id = h.student_id AND s.tenant_id = :tenantId
             WHERE h.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY h.id
            """;
    public static final String ERASE_HEALTH_RECORD = """
            UPDATE sis.student_health_record
               SET height_cm = NULL, weight_kg = NULL, medical_conditions = NULL, medication_needs = NULL,
                   medication_allergies = NULL, food_allergies = NULL, other_allergies = NULL,
                   other_medical_info = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String ACTIVITY_ROWS = """
            SELECT a.id
              FROM sis.student_activity a
              JOIN sis.student s ON s.id = a.student_id AND s.tenant_id = :tenantId
             WHERE a.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY a.id
            """;
    public static final String ERASE_ACTIVITY = """
            UPDATE sis.student_activity
               SET description = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String LEAVE_REQUEST_ROWS = """
            SELECT l.id
              FROM sis.student_leave_request l
              JOIN sis.student s ON s.id = l.student_id AND s.tenant_id = :tenantId
             WHERE l.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY l.id
            """;
    public static final String ERASE_LEAVE_REQUEST = """
            UPDATE sis.student_leave_request
               SET reason = NULL, decision_note = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String GUARDIAN_ROWS = """
            SELECT id FROM sis.guardian WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** The guardian stays linked to the students; only what describes the person goes. */
    public static final String ANONYMISE_GUARDIAN = """
            UPDATE sis.guardian
               SET profession = NULL, sub_occupation = NULL, designation = NULL, organization_name = NULL,
                   annual_income = NULL, qualification_id = NULL, mobile = NULL, email = NULL, updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String STUDENT_ROWS = """
            SELECT id FROM sis.student WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** The admission register stays; why the student left goes. */
    public static final String ANONYMISE_STUDENT = """
            UPDATE sis.student
               SET exit_reason = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** sis.health_after_leaving: the students whose health records outlived the cutoff after they left. */
    public static final String HEALTH_AFTER_LEAVING_SUBJECTS = """
            SELECT DISTINCT s.person_id
              FROM sis.student s
              JOIN sis.student_health_record h ON h.student_id = s.id AND h.tenant_id = :tenantId
                   AND h.deleted_at IS NULL
             WHERE s.tenant_id = :tenantId AND s.exit_date < :cutoff AND s.person_id > :after
             ORDER BY s.person_id
             LIMIT :limit
            """;
    /** One subject's health records due under sis.health_after_leaving. */
    public static final String HEALTH_AFTER_LEAVING_ROWS = """
            SELECT h.id
              FROM sis.student_health_record h
              JOIN sis.student s ON s.id = h.student_id AND s.tenant_id = :tenantId
             WHERE h.tenant_id = :tenantId AND s.person_id = :subjectId AND s.exit_date < :cutoff
               AND h.deleted_at IS NULL
             ORDER BY h.id
            """;

    private PrivacyErasureRepository() {
    }
}
