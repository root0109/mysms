package com.mysms.core.trace;

/**
 * How many SQL statements and calls to other modules the current request made, counted on its thread: the trace an
 * N+1 (a query or a call per row) leaves. {@link TraceFilter} opens a count for every HTTP request and logs a warning
 * when the request ends above {@link #WARN_STATEMENTS} statements or {@link #WARN_REMOTE_CALLS} remote calls; a test
 * opens its own with {@link #open()} around the code it measures and asserts on the totals. Counts nest: a statement
 * counts in every open count of the thread. With no count open, counting does nothing.
 *
 * <p>Statements are counted where a module's {@code JdbcClient} starts one ({@code TenantJdbcClient}), remote calls
 * where the caller interceptor sends one; work a request hands to another thread is not counted.
 */
public final class RequestCost {

    /** Statements one request may run before its end is logged as a likely N+1. */
    public static final int WARN_STATEMENTS = 100;
    /** Calls to other modules one request may make before its end is logged as a likely N+1. */
    public static final int WARN_REMOTE_CALLS = 20;

    private static final ThreadLocal<Count> CURRENT = new ThreadLocal<>();

    private RequestCost() {
    }

    /** Opens a count on this thread; closing it brings back the count it was opened inside, if any. */
    public static Count open() {
        Count count = new Count(CURRENT.get());
        CURRENT.set(count);
        return count;
    }

    /** One SQL statement was started on this thread. */
    public static void statement() {
        for (Count count = CURRENT.get(); count != null; count = count.outer) {
            count.statements++;
        }
    }

    /** One call to another module was started on this thread. */
    public static void remoteCall() {
        for (Count count = CURRENT.get(); count != null; count = count.outer) {
            count.remoteCalls++;
        }
    }

    /** The totals of one open count; close it on the thread that opened it. */
    public static final class Count implements AutoCloseable {

        private final Count outer;
        private int statements;
        private int remoteCalls;

        private Count(Count outer) {
            this.outer = outer;
        }

        public int statements() {
            return statements;
        }

        public int remoteCalls() {
            return remoteCalls;
        }

        /** Whether the totals are past what one request should need: a likely query or call per row. */
        public boolean aboveWarning() {
            return statements > WARN_STATEMENTS || remoteCalls > WARN_REMOTE_CALLS;
        }

        @Override
        public void close() {
            if (outer == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(outer);
            }
        }
    }
}
