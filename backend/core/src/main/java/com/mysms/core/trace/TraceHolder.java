package com.mysms.core.trace;

import java.util.Optional;

/** Thread-bound {@link TraceContext} of the current request, used to propagate traceparent on outbound calls. */
public final class TraceHolder {

    private static final ThreadLocal<TraceContext> CURRENT = new ThreadLocal<>();

    private TraceHolder() {
    }

    public static void set(TraceContext context) {
        CURRENT.set(context);
    }

    public static Optional<TraceContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
