package com.mysms.core.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

/**
 * Runs every step of a call's listener inside a {@link CallBinding} (Decorator): the thread-local state of the call is
 * present while the step runs on whichever pool thread gRPC chose, and gone afterwards.
 *
 * @param <ReqT> the request message type
 */
public final class BoundListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final CallBinding binding;

    public BoundListener(ServerCall.Listener<ReqT> delegate, CallBinding binding) {
        super(delegate);
        this.binding = binding;
    }

    @Override
    public void onMessage(ReqT message) {
        try (CallBinding.Scope scope = binding.bind()) {
            super.onMessage(message);
        }
    }

    @Override
    public void onHalfClose() {
        try (CallBinding.Scope scope = binding.bind()) {
            super.onHalfClose();
        }
    }

    @Override
    public void onCancel() {
        try (CallBinding.Scope scope = binding.bind()) {
            super.onCancel();
        }
    }

    @Override
    public void onComplete() {
        try (CallBinding.Scope scope = binding.bind()) {
            super.onComplete();
        }
    }

    @Override
    public void onReady() {
        try (CallBinding.Scope scope = binding.bind()) {
            super.onReady();
        }
    }
}
