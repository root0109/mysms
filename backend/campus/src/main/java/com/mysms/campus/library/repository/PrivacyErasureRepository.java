package com.mysms.campus.library.repository;

/**
 * The statements of library's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code LibraryPrivacyTaskHandler} through privacy-client: per table, the ids of a subject's rows (as the person of a
 * library card, a reservation's or a purchase request's requester; soft-deleted rows included, since soft delete is
 * not erasure) and the statement that erases one row. A loan still open — the copy not back, or a fine charged and not
 * yet invoiced — is kept with its card: the erasing statements leave them unchanged, and {@link #OPEN_LOAN_ROWS} and
 * {@link #OPEN_CARD_ROWS} name them. An invoiced fine is finance's to keep.
 */
public final class PrivacyErasureRepository {

    public static final String RESERVATION_ROWS = """
            SELECT id FROM library.reservation WHERE tenant_id = :tenantId AND requested_by_person_id = :subjectId
             ORDER BY id
            """;
    /** Who asked for the title goes with the row. */
    public static final String ERASE_RESERVATION = """
            UPDATE library.reservation
               SET requested_by_person_id = NULL, requested_by_user_id = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PURCHASE_REQUEST_ROWS = """
            SELECT id FROM library.purchase_request WHERE tenant_id = :tenantId AND requested_by_person_id = :subjectId
             ORDER BY id
            """;
    /** The acquisition request stays (title, author, status); who asked for it goes. */
    public static final String ANONYMISE_PURCHASE_REQUEST = """
            UPDATE library.purchase_request
               SET requested_by_person_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String LOAN_ROWS = """
            SELECT l.id
              FROM library.loan l
              JOIN library.library_card c ON c.id = l.library_card_id AND c.tenant_id = :tenantId
             WHERE l.tenant_id = :tenantId AND c.person_id = :subjectId
             ORDER BY l.id
            """;
    /**
     * The loan stays in the circulation history (copy, dates, fine) against the card, which is anonymised after it;
     * the borrower's account goes. An open loan is left unchanged (0 rows).
     */
    public static final String ANONYMISE_LOAN = """
            UPDATE library.loan
               SET borrower_user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
               AND NOT (status IN ('reserved', 'issued', 'return_pending') OR (fine_amount > 0 AND invoice_id IS NULL))
            """;
    /** The subject's open loans: the copy not back, or a fine charged and not yet invoiced. */
    public static final String OPEN_LOAN_ROWS = """
            SELECT l.id
              FROM library.loan l
              JOIN library.library_card c ON c.id = l.library_card_id AND c.tenant_id = :tenantId
             WHERE l.tenant_id = :tenantId AND c.person_id = :subjectId
               AND (l.status IN ('reserved', 'issued', 'return_pending') OR (l.fine_amount > 0 AND l.invoice_id IS NULL))
             ORDER BY l.id
            """;

    public static final String CARD_ROWS = """
            SELECT id FROM library.library_card WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /**
     * The card is deactivated and soft-deleted, kept for the loans that name it; whose it was goes: the person and the
     * holder's student or staff record become placeholders derived from the row's own id (the holder-type rule holds),
     * the card number one too (unique per institution). A card with an open loan is left unchanged (0 rows).
     */
    public static final String ERASE_CARD = """
            UPDATE library.library_card
               SET person_id = md5('mysms.erased:' || id::text)::uuid,
                   student_id = CASE WHEN student_id IS NULL THEN NULL ELSE md5('mysms.erased:' || id::text)::uuid END,
                   staff_id = CASE WHEN staff_id IS NULL THEN NULL ELSE md5('mysms.erased:' || id::text)::uuid END,
                   card_number = 'erased-' || replace(id::text, '-', ''), max_items_override = NULL,
                   status = 'inactive', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
               AND NOT EXISTS (SELECT 1
                                 FROM library.loan l
                                WHERE l.tenant_id = :tenantId AND l.library_card_id = library_card.id
                                  AND (l.status IN ('reserved', 'issued', 'return_pending')
                                       OR (l.fine_amount > 0 AND l.invoice_id IS NULL)))
            """;
    /** The subject's cards kept for an open loan. */
    public static final String OPEN_CARD_ROWS = """
            SELECT c.id
              FROM library.library_card c
             WHERE c.tenant_id = :tenantId AND c.person_id = :subjectId
               AND EXISTS (SELECT 1
                             FROM library.loan l
                            WHERE l.tenant_id = :tenantId AND l.library_card_id = c.id
                              AND (l.status IN ('reserved', 'issued', 'return_pending')
                                   OR (l.fine_amount > 0 AND l.invoice_id IS NULL)))
             ORDER BY c.id
            """;

    private PrivacyErasureRepository() {
    }
}
