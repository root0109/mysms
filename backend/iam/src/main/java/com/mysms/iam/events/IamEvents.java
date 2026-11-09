package com.mysms.iam.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain events iam publishes through its outbox. Payloads carry ids only — never names, identifiers typed at
 * sign-in, passwords or tokens.
 */
public final class IamEvents {

    public static final String USER_ACCOUNT = "iam.user_account";
    public static final String SIGNED_IN = USER_ACCOUNT + ".signed_in";
    public static final String SIGN_IN_FAILED = USER_ACCOUNT + ".sign_in_failed";
    public static final String SIGNED_OUT = USER_ACCOUNT + ".signed_out";
    /** A rotated refresh token was presented again: the whole session was revoked (possible token theft). */
    public static final String REFRESH_REUSE_DETECTED = USER_ACCOUNT + ".refresh_reuse_detected";
    /** Second factors and account credentials (ids only, never codes, secrets or passwords). */
    public static final String MFA_ENROLLED = USER_ACCOUNT + ".mfa_enrolled";
    public static final String MFA_DISABLED = USER_ACCOUNT + ".mfa_disabled";
    public static final String RECOVERY_CODES_REGENERATED = USER_ACCOUNT + ".recovery_codes_regenerated";
    public static final String PASSWORD_CHANGED = USER_ACCOUNT + ".password_changed";
    public static final String CREDENTIALS_SET = USER_ACCOUNT + ".credentials_set";
    public static final String TERMS_ACCEPTED = USER_ACCOUNT + ".terms_accepted";
    /** Aggregate id of a failed sign-in that matched no account (the outbox needs one; it names nobody). */
    public static final UUID UNKNOWN_ACCOUNT = new UUID(0L, 0L);
    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";

    private IamEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    public static Map<String, Object> session(UUID user, UUID session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.toString());
        payload.put("sessionId", session.toString());
        return payload;
    }
}
