package com.mysms.academics.repository;

/**
 * The statements of academics' privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code AcademicsPrivacyTaskHandler} through privacy-client: the ids of a subject's lesson plans (the person known
 * from academics.staff_record; soft-deleted rows included, since soft delete is not erasure) and the statement that
 * anonymises one. The plan stays, since it is the school's teaching record; the author's own notes go.
 */
public final class PrivacyErasureRepository {

    public static final String LESSON_PLAN_ROWS = """
            SELECT p.id
              FROM academics.lesson_plan p
              JOIN academics.teaching_assignment t ON t.id = p.teaching_assignment_id AND t.tenant_id = :tenantId
              JOIN academics.staff_record s ON s.staff_id = t.staff_id AND s.tenant_id = :tenantId
             WHERE p.tenant_id = :tenantId AND s.person_id = :subjectId
             ORDER BY p.id
            """;
    public static final String ANONYMISE_LESSON_PLAN = """
            UPDATE academics.lesson_plan
               SET notes = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
