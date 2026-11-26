package com.mysms.academics.assessment.repository;

/**
 * The statements of assessment's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code AssessmentPrivacyTaskHandler} through privacy-client. assessment knows a data principal through the person of
 * each student record (assessment.student_record, kept from sis's events). Per table, the ids of the subject's rows
 * (soft-deleted ones included, since soft delete is not erasure) and the statement that erases one row; the marks of
 * record — exam results and marksheet lines — are kept under assessment.after_leaving and only counted.
 */
public final class PrivacyErasureRepository {

    public static final String HOMEWORK_SUBMISSION_ROWS = """
            SELECT h.id
              FROM assessment.homework_submission h
              JOIN assessment.student_record s ON s.student_id = h.student_id AND s.tenant_id = :tenantId
             WHERE h.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY h.id
            """;
    /** Homework is never part of a marksheet: the student's reply and the teacher's comments go with the row. */
    public static final String ERASE_HOMEWORK_SUBMISSION = """
            UPDATE assessment.homework_submission
               SET student_reply = NULL, teacher_comments = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String ASSIGNMENT_SUBMISSION_ROWS = """
            SELECT a.id
              FROM assessment.assignment_submission a
              JOIN assessment.student_record s ON s.student_id = a.student_id AND s.tenant_id = :tenantId
             WHERE a.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY a.id
            """;
    /** The mark and its dates stay; the student's answer and the teacher's feedback go. */
    public static final String ANONYMISE_ASSIGNMENT_SUBMISSION = """
            UPDATE assessment.assignment_submission
               SET content = NULL, feedback = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The subject's exam results, kept under assessment.after_leaving. */
    public static final String EXAM_RESULT_KEPT = """
            SELECT count(*)
              FROM assessment.exam_result r
              JOIN assessment.student_record s ON s.student_id = r.student_id AND s.tenant_id = :tenantId
             WHERE r.tenant_id = :tenantId AND s.person_id = :subjectId
            """;
    /** The subject's marksheet lines, kept under assessment.after_leaving. */
    public static final String MARKSHEET_LINE_KEPT = """
            SELECT count(*)
              FROM assessment.marksheet_line l
              JOIN assessment.student_record s ON s.student_id = l.student_id AND s.tenant_id = :tenantId
             WHERE l.tenant_id = :tenantId AND s.person_id = :subjectId
            """;

    private PrivacyErasureRepository() {
    }
}
