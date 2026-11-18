package com.mysms.authclient;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ResourceNotFoundException;
import java.util.UUID;

/**
 * What a public read needs to know about a caller without a token: whether it is a guest, and the tenant it named with
 * {@code X-Tenant-Code}. A guest who named no school is answered 404, never with a guessed tenant's rows.
 */
public final class Guest {

    private Guest() {
    }

    /** True when the call carries no token: the service serves its guest projection (published rows only). */
    public static boolean isGuest() {
        return AuthContext.current().isEmpty();
    }

    /** The tenant the guest named; 404 when the request named none (no {@code X-Tenant-Code}). */
    public static UUID requireTenant() {
        return CallContextHolder.current().flatMap(CallContext::tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("school", AuthFilter.TENANT_CODE_HEADER));
    }
}
