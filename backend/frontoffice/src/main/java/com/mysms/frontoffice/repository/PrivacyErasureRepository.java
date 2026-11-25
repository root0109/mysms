package com.mysms.frontoffice.repository;

/**
 * The statements of frontoffice's retention sweep (design PRIVACY-AND-CONSENT-CENTRE §10), run by its
 * {@code FrontofficePrivacyTaskHandler} through privacy-client. A visitor is mostly not a person of the tenant and a
 * visitor entry names no person, so the sweep's subjects are the due entries themselves: each entry's id stands for
 * its visitor (a pseudo-subject), and the step erases that one entry when it lies before the cutoff.
 */
public final class PrivacyErasureRepository {

    /**
     * frontoffice.visitor_log: the entries visited before the cutoff and not yet erased, as pseudo-subjects in id
     * order; a user's soft delete is not erasure, so a soft-deleted entry is due until it is erased.
     */
    public static final String VISITOR_LOG_SUBJECTS = """
            SELECT id
              FROM frontoffice.visitor_log
             WHERE tenant_id = :tenantId AND visit_date < :cutoff AND id > :after
               AND (deleted_at IS NULL OR visitor_name <> 'Erased')
             ORDER BY id
             LIMIT :limit
            """;
    /** The one entry a pseudo-subject stands for, while it is due. */
    public static final String VISITOR_LOG_DUE_ROWS = """
            SELECT id FROM frontoffice.visitor_log WHERE tenant_id = :tenantId AND id = :subjectId AND visit_date < :cutoff
            """;
    /**
     * Who visited goes — name and purpose (both required) become the placeholder {@code Erased}; contacts, company,
     * address, the student visited and the remarks are cleared — and the entry is soft-deleted; the date, the visitor
     * type, the head count and the staff member met stay for the register's counts.
     */
    public static final String ERASE_VISITOR_LOG = """
            UPDATE frontoffice.visitor_log
               SET visitor_name = 'Erased', email = NULL, phone = NULL, company_name = NULL, address = NULL,
                   student_id = NULL, relation_to_student = NULL, related_person_name = NULL, purpose = 'Erased',
                   remarks = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
