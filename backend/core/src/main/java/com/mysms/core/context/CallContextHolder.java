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
