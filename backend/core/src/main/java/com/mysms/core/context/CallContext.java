package com.mysms.core.context;

import java.util.Optional;
import java.util.UUID;

/**
 * Who is calling and on whose behalf, for one request or message. Set only at the edge; business code never builds
 * one.
 *
 * @param tenantId       tenant the request acts in (from the token, never from the request), or empty for platform calls
 * @param userId         the authenticated user account, or empty for public operations
 * @param traceId        W3C trace id of the request
 * @param impersonatorId the administrator acting as {@code userId} (the token's {@code act.sub}), when impersonating:
 *                       every audit record then names both
 */
public record CallContext(Optional<UUID> tenantId, Optional<UUID> userId, String traceId,
                          Optional<UUID> impersonatorId) {

    public CallContext {
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        userId = userId == null ? Optional.empty() : userId;
        impersonatorId = impersonatorId == null ? Optional.empty() : impersonatorId;
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId is required");
        }
    }

    /** A call without an impersonator. */
    public CallContext(Optional<UUID> tenantId, Optional<UUID> userId, String traceId) {
        this(tenantId, userId, traceId, Optional.empty());
    }

    /** An anonymous call carrying only its trace. */
    public static CallContext anonymous(String traceId) {
        return new CallContext(Optional.empty(), Optional.empty(), traceId);
    }

    public CallContext withIdentity(UUID tenant, UUID user) {
        return new CallContext(Optional.ofNullable(tenant), Optional.ofNullable(user), traceId, impersonatorId);
    }

    /** The same call made by an administrator acting as the user (null: nobody is). */
    public CallContext withImpersonator(UUID impersonator) {
        return new CallContext(tenantId, userId, traceId, Optional.ofNullable(impersonator));
    }
}
