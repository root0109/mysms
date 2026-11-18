package com.mysms.core.grpc;

/**
 * Binds a call's thread-local state (trace, call context, identity) for the duration of one step of the call. gRPC
 * runs the steps of one call (start, each message, half-close, cancel) on pool threads that may differ from step to
 * step, so state is bound around every step and restored afterwards, never left on a pooled thread.
 */
@FunctionalInterface
public interface CallBinding {

    /** Binds the state and returns the scope that restores what was bound before. */
    Scope bind();

    /** What {@link #bind()} opened; closing it restores the previous state. */
    interface Scope extends AutoCloseable {

        @Override
        void close();
    }
}
