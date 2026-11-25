package com.mysms.finance.repository;

/**
 * The statements of finance's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code FinancePrivacyTaskHandler} through privacy-client. finance knows a data principal as the person of a student
 * record (finance.student_record, kept from sis's events) and as the payer person of a payment; a payment is the
 * subject's when they paid it or when it settles an invoice of theirs. The books of account — fee dues, concessions,
 * invoices, payments and refunds — are kept under finance.after_leaving and only counted; what the gateway sent about
 * the payer (a notification's raw body: contact and card details) goes, the gateway's ids and the matched payment stay.
 */
public final class PrivacyErasureRepository {

    public static final String GATEWAY_EVENT_ROWS = """
            SELECT e.id
              FROM finance.gateway_event e
              JOIN finance.payment p ON p.id = e.payment_id AND p.tenant_id = :tenantId
             WHERE e.tenant_id = :tenantId
               AND (p.payer_person_id = :subjectId
                    OR p.invoice_id IN (SELECT i.id
                                          FROM finance.invoice i
                                          JOIN finance.student_record s
                                            ON s.student_id = i.student_id AND s.tenant_id = :tenantId
                                         WHERE i.tenant_id = :tenantId AND s.person_id = :subjectId))
             ORDER BY e.id
            """;
    /** The notification stays as evidence of the payment (gateway, event id and type, the payment matched). */
    public static final String ANONYMISE_GATEWAY_EVENT = """
            UPDATE finance.gateway_event
               SET raw_payload = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The subject's fee dues, kept under finance.after_leaving. */
    public static final String STUDENT_FEE_DUE_KEPT = """
            SELECT count(*)
              FROM finance.student_fee_due d
              JOIN finance.student_record s ON s.student_id = d.student_id AND s.tenant_id = :tenantId
             WHERE d.tenant_id = :tenantId AND s.person_id = :subjectId
            """;
    /** The subject's fee concessions, kept under finance.after_leaving. */
    public static final String FEE_CONCESSION_KEPT = """
            SELECT count(*)
              FROM finance.fee_concession c
              JOIN finance.student_record s ON s.student_id = c.student_id AND s.tenant_id = :tenantId
             WHERE c.tenant_id = :tenantId AND s.person_id = :subjectId
            """;
    /** The subject's invoices and credit notes, kept under finance.after_leaving. */
    public static final String INVOICE_KEPT = """
            SELECT count(*)
              FROM finance.invoice i
              JOIN finance.student_record s ON s.student_id = i.student_id AND s.tenant_id = :tenantId
             WHERE i.tenant_id = :tenantId AND s.person_id = :subjectId
            """;
    /** The subject's payments, payer and billing details included, kept under finance.after_leaving. */
    public static final String PAYMENT_KEPT = """
            SELECT count(*)
              FROM finance.payment p
             WHERE p.tenant_id = :tenantId
               AND (p.payer_person_id = :subjectId
                    OR p.invoice_id IN (SELECT i.id
                                          FROM finance.invoice i
                                          JOIN finance.student_record s
                                            ON s.student_id = i.student_id AND s.tenant_id = :tenantId
                                         WHERE i.tenant_id = :tenantId AND s.person_id = :subjectId))
            """;
    /** The refunds of the subject's payments, kept under finance.after_leaving. */
    public static final String REFUND_KEPT = """
            SELECT count(*)
              FROM finance.refund r
              JOIN finance.payment p ON p.id = r.payment_id AND p.tenant_id = :tenantId
             WHERE r.tenant_id = :tenantId
               AND (p.payer_person_id = :subjectId
                    OR p.invoice_id IN (SELECT i.id
                                          FROM finance.invoice i
                                          JOIN finance.student_record s
                                            ON s.student_id = i.student_id AND s.tenant_id = :tenantId
                                         WHERE i.tenant_id = :tenantId AND s.person_id = :subjectId))
            """;

    private PrivacyErasureRepository() {
    }
}
