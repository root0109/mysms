package com.mysms.core.context;

import java.util.Optional;
import java.util.UUID;

/**
 * Who is calling and on whose behalf, for the duration of one request (or one message). Set at the edge by the
 * auth filter and the trace filter; read by the tenant binding (tenant), the audit writer (actor) and logging.
 * Business code never builds one.
 *
 * @param tenantId  tenant the request acts in (from the token, never from the request), or empty for platform calls
 * @param userId    the authenticated user account, or empty for public operations
 * @param traceId   W3C trace id of the request
 */
public record CallContext(Optional<UUID> tenantId, Optional<UUID> userId, String traceId) {

    public CallContext {
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        userId = userId == null ? Optional.empty() : userId;
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId is required");
        }
    }

    /** An anonymous call carrying only its trace. */
    public static CallContext anonymous(String traceId) {
        return new CallContext(Optional.empty(), Optional.empty(), traceId);
    }

    public CallContext withIdentity(UUID tenant, UUID user) {
        return new CallContext(Optional.ofNullable(tenant), Optional.ofNullable(user), traceId);
    }
}
