package com.mysms.careers.repository;

/**
 * The statements of careers' privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code CareersPrivacyTaskHandler} through privacy-client: per table, the ids of a subject's rows (soft-deleted rows
 * included, since soft delete is not erasure) and the statement that erases one row. The subject's job-search
 * preferences are found by the person they name; job applications name only the applicant's login account, which
 * careers cannot tie to a person, and the employer side (openings, the reference catalogues) is not the subject's.
 */
public final class PrivacyErasureRepository {

    public static final String PREFERRED_FUNCTION_ROWS = """
            SELECT f.id
              FROM careers.candidate_preferred_function f
              JOIN careers.candidate_preference p ON p.id = f.candidate_preference_id AND p.tenant_id = :tenantId
             WHERE f.tenant_id = :tenantId AND p.person_id = :subjectId
             ORDER BY f.id
            """;
    /** A preferred function says nothing beyond its link: the row is soft-deleted. */
    public static final String ERASE_PREFERRED_FUNCTION = """
            UPDATE careers.candidate_preferred_function
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PREFERRED_INDUSTRY_ROWS = """
            SELECT i.id
              FROM careers.candidate_preferred_industry i
              JOIN careers.candidate_preference p ON p.id = i.candidate_preference_id AND p.tenant_id = :tenantId
             WHERE i.tenant_id = :tenantId AND p.person_id = :subjectId
             ORDER BY i.id
            """;
    /** A preferred industry says nothing beyond its link: the row is soft-deleted. */
    public static final String ERASE_PREFERRED_INDUSTRY = """
            UPDATE careers.candidate_preferred_industry
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String CANDIDATE_PREFERENCE_ROWS = """
            SELECT id FROM careers.candidate_preference WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** Every preference is cleared (each column is optional) and the profile soft-deleted. */
    public static final String ERASE_CANDIDATE_PREFERENCE = """
            UPDATE careers.candidate_preference
               SET open_to = NULL, preferred_location = NULL, dream_companies = NULL, attire = NULL,
                   collaboration = NULL, role_clarity = NULL, location_preference = NULL, work_hours = NULL,
                   company_type = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
