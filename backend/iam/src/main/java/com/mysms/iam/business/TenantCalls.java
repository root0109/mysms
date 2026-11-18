package com.mysms.iam.business;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * For the public flows that establish identity themselves (password reset, verification, sign-up, the API-token
 * exchange): they learn tenant and user from the request or the secret and run as them, so row-level security, audit
 * and events carry them.
 */
final class TenantCalls {

    private TenantCalls() {
    }

    /** Runs work as the tenant (and user, once known), restoring the caller's context afterwards. */
    static <T> T as(UUID tenant, UUID user, Supplier<T> work) {
        CallContext base = CallContextHolder.current().orElseGet(() -> CallContext.anonymous(
                UUID.randomUUID().toString().replace("-", "")));
        return CallContextHolder.callAs(base.withIdentity(tenant, user), work);
    }

    /** Now, in UTC, at the database's precision. */
    static OffsetDateTime now(Clock clock) {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
