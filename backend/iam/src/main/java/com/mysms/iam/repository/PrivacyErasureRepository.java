package com.mysms.iam.repository;

/**
 * The statements of iam's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code IamPrivacyTaskHandler} through privacy-client: the subject's accounts, and the statement that anonymises one
 * — no sign-in name, address, number, password or signature remains; the username becomes {@code erased-<id>} so the
 * row still names itself. The account is kept (audit records refer to it) and deactivated by the service.
 */
public final class PrivacyErasureRepository {

    public static final String ACCOUNT_ROWS = """
            SELECT id FROM iam.user_account WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ANONYMISE_ACCOUNT = """
            UPDATE iam.user_account
               SET username = 'erased-' || id, email = NULL, mobile_no = NULL, password_hash = NULL,
                   email_verified_at = NULL, mobile_verified_at = NULL, signature = NULL, use_signature = false,
                   external_ref = NULL, locale = NULL, timezone = NULL, must_reset_password = false,
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
