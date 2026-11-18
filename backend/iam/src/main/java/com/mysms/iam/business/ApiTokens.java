package com.mysms.iam.business;

import java.util.Optional;
import java.util.UUID;

/**
 * Personal API tokens: {@code mysms_pat_<tenant>.<256 random bits>}. The prefix tells them from refresh tokens at
 * iam.RefreshToken (where an API token is exchanged for a short-lived access token) and lets secret scanners
 * recognise a leaked one; the tenant part binds the right tenant before the lookup. Only the SHA-256 is stored.
 */
public final class ApiTokens {

    static final String PREFIX = "mysms_pat_";

    private ApiTokens() {
    }

    static String generate(UUID tenant) {
        return PREFIX + RefreshTokens.generate(tenant);
    }

    /** Whether the presented value has the API-token form (it may still be unknown or revoked). */
    public static boolean recognises(String presented) {
        return presented != null && presented.startsWith(PREFIX);
    }

    /** The tenant of a well-formed API token; empty for anything malformed. */
    static Optional<UUID> tenantOf(String presented) {
        return recognises(presented) ? RefreshTokens.tenantOf(presented.substring(PREFIX.length()))
                : Optional.empty();
    }
}
