package com.mysms.core.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;
import java.util.function.Consumer;

/**
 * Decorator of a call's listener inside {@link GlobalGrpcExceptionHandler}: whatever a step that runs the resolver
 * throws is answered with the mapped status instead of gRPC's UNKNOWN.
 *
 * @param <ReqT> the request message type
 */
final class ExceptionHandlingServerCallListener<ReqT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final Consumer<RuntimeException> failure;

    ExceptionHandlingServerCallListener(ServerCall.Listener<ReqT> delegate, Consumer<RuntimeException> failure) {
        super(delegate);
        this.failure = failure;
    }

    @Override
    public void onMessage(ReqT message) {
        try {
            super.onMessage(message);
        } catch (RuntimeException e) {
            failure.accept(e);
        }
    }

    @Override
    public void onHalfClose() {
        try {
            super.onHalfClose();
        } catch (RuntimeException e) {
            failure.accept(e);
        }
    }

    @Override
    public void onReady() {
        try {
            super.onReady();
        } catch (RuntimeException e) {
            failure.accept(e);
        }
    }
}
