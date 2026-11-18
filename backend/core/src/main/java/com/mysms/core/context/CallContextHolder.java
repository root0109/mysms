package com.mysms.core.context;

import java.util.Optional;
import java.util.function.Supplier;

/** Thread-bound {@link CallContext}. Set and cleared only by the edge filters (and test fixtures). */
public final class CallContextHolder {

    private static final ThreadLocal<CallContext> CURRENT = new ThreadLocal<>();

    private CallContextHolder() {
    }

    public static void set(CallContext context) {
        CURRENT.set(context);
    }

    public static Optional<CallContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** The current context; failing here means code ran outside a request, which is a programming error. */
    public static CallContext require() {
        CallContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("no call context bound to this thread");
        }
        return context;
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** The binding opened by {@link #withoutTenant()}; closing it restores the context bound before. */
    public interface Scope extends AutoCloseable {

        @Override
        void close();
    }

    /**
     * Binds the current context without its tenant (user and trace kept) until the returned scope is closed, for a
     * write to global tables: the events and audit records it writes are then global ones, even when a signed-in
     * tenant user triggered it (a public form, say). Without a context, or without a tenant in it, nothing changes.
     */
    public static Scope withoutTenant() {
        CallContext before = CURRENT.get();
        if (before == null || before.tenantId().isEmpty()) {
            return () -> { };
        }
        CURRENT.set(new CallContext(Optional.empty(), before.userId(), before.traceId(), before.impersonatorId()));
        return () -> CURRENT.set(before);
    }

    /**
     * Runs work with {@code context} bound and restores whatever was bound before, also on failure. For the few
     * flows that establish identity themselves instead of receiving it from the edge: sign-in and token refresh (the
     * tenant is resolved from the request, the user from the credentials) and startup bootstrap.
     */
    public static <T> T callAs(CallContext context, Supplier<T> work) {
        CallContext before = CURRENT.get();
        CURRENT.set(context);
        try {
            return work.get();
        } finally {
            if (before == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(before);
            }
        }
    }
}
