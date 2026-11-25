package com.mysms.iam.events;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
    /** Mailed one-time secrets and what they complete (ids only, never the secret or the address). */
    public static final String PASSWORD_RESET_REQUESTED = USER_ACCOUNT + ".password_reset_requested";
    public static final String PASSWORD_RESET = USER_ACCOUNT + ".password_reset";
    public static final String PASSWORD_RESET_REQUIRED = USER_ACCOUNT + ".password_reset_required";
    public static final String VERIFICATION_SENT = USER_ACCOUNT + ".verification_sent";
    public static final String EMAIL_VERIFIED = USER_ACCOUNT + ".email_verified";
    public static final String SIGNED_UP = USER_ACCOUNT + ".signed_up";
    public static final String USERS_IMPORTED = USER_ACCOUNT + ".users_imported";
    /** Tokens other than sign-in issues: impersonation (both identities), role switch, personal API tokens. */
    public static final String IMPERSONATION_STARTED = USER_ACCOUNT + ".impersonation_started";
    public static final String IMPERSONATION_ENDED = USER_ACCOUNT + ".impersonation_ended";
    public static final String ROLE_SWITCHED = USER_ACCOUNT + ".role_switched";
    public static final String ROLES_SET = USER_ACCOUNT + ".roles_set";
    public static final String API_TOKEN_CREATED = USER_ACCOUNT + ".api_token_created";
    public static final String API_TOKEN_REVOKED = USER_ACCOUNT + ".api_token_revoked";
    public static final String TENANT_DIRECTORY = "iam.tenant_directory";
    /** The installation bootstrap ensured its tenant (id and code): the platform registers the tenant it lacks. */
    public static final String TENANT_BOOTSTRAPPED = TENANT_DIRECTORY + ".bootstrapped";
    /** Aggregate id of a failed sign-in that matched no account (the outbox needs one; it names nobody). */
    public static final UUID UNKNOWN_ACCOUNT = new UUID(0L, 0L);
    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";
    /** The setting scopes whose values a setting-value event carries. */
    static final Set<String> VALUE_SCOPES = Set.of("tenant", "institution");

    private IamEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /**
     * The payload of an administrator's setting value (iam.setting_value): besides its id, the setting, the scope it
     * applies to and the row version the event reflects; the value only for a tenant or school setting, since a
     * department's, class's or person's value is nobody else's business. org keeps whether a school is in the public
     * school finder from these (org.finder_listing); the version lets it ignore a late event.
     */
    public static Map<String, Object> settingValue(UUID id, String settingKey, String scopeType, UUID scopeId,
            String value, int rowVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        payload.put("settingKey", settingKey);
        payload.put("scopeType", scopeType);
        payload.put("scopeId", scopeId == null ? null : scopeId.toString());
        if (VALUE_SCOPES.contains(scopeType)) {
            payload.put("value", value);
        }
        payload.put("rowVersion", rowVersion);
        return payload;
    }

    /** An impersonation: the account acted as, the administrator acting and the session it happens in. */
    public static Map<String, Object> impersonation(UUID user, UUID impersonator, UUID session) {
        Map<String, Object> payload = session(user, session);
        payload.put("impersonatorId", impersonator.toString());
        return payload;
    }

    public static Map<String, Object> session(UUID user, UUID session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.toString());
        payload.put("sessionId", session.toString());
        return payload;
    }
}
