package com.mysms.authclient;

import java.util.Optional;
import java.util.UUID;

/**
 * Port through which the edge finds the tenant a guest's {@code X-Tenant-Code} names. A module serving guest reads
 * implements it in its iam gRPC client; the edge reads it through {@link TenantCodes}.
 */
public interface TenantCodeResolver {

    /**
     * The active tenant the code names; empty for an unknown, suspended or closed school.
     *
     * @throws com.mysms.core.error.UnavailableException when the identity service cannot answer
     */
    Optional<UUID> activeTenant(String code);
}
