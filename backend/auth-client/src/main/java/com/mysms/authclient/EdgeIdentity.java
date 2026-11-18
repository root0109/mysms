package com.mysms.authclient;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.grpc.CallBinding;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Binds an admitted caller ({@link AuthContext}, {@link CallContext}, logging MDC) for one step of a call and restores
 * what was bound before; both edges bind through here, so both bind exactly the same.
 */
final class EdgeIdentity {

    private static final String MDC_TENANT = "tenantId";
    private static final String MDC_USER = "userId";

    private EdgeIdentity() {
    }

    static CallBinding.Scope bind(EdgeDecision.Admitted admitted) {
        if (admitted.caller().isEmpty()) {
            return admitted.guestTenant().map(EdgeIdentity::bindGuest).orElse(() -> { });
        }
        AuthContext context = admitted.caller().get();
        Optional<CallContext> before = CallContextHolder.current();
        String traceId = before.map(CallContext::traceId).orElseGet(() -> UUID.randomUUID().toString());
        // an administrator acting as the user (iam.StartImpersonation): every audit record names both
        CallContextHolder.set(CallContext.anonymous(traceId).withIdentity(context.tenantId().orElse(null),
                context.userId()).withImpersonator(context.impersonatorId().orElse(null)));
        AuthContextHolder.set(context, admitted.token(), admitted.authMethods());
        context.tenantId().ifPresent(t -> MDC.put(MDC_TENANT, t.toString()));
        MDC.put(MDC_USER, context.userId().toString());
        return () -> {
            MDC.remove(MDC_TENANT);
            MDC.remove(MDC_USER);
            AuthContextHolder.clear();
            before.ifPresentOrElse(CallContextHolder::set, CallContextHolder::clear);
        };
    }

    /** A guest: the tenant its school code named (row-level security binds it), no user and no AuthContext. */
    private static CallBinding.Scope bindGuest(UUID tenant) {
        Optional<CallContext> before = CallContextHolder.current();
        String traceId = before.map(CallContext::traceId).orElseGet(() -> UUID.randomUUID().toString());
        CallContextHolder.set(CallContext.anonymous(traceId).withIdentity(tenant, null));
        MDC.put(MDC_TENANT, tenant.toString());
        return () -> {
            MDC.remove(MDC_TENANT);
            before.ifPresentOrElse(CallContextHolder::set, CallContextHolder::clear);
        };
    }
}
