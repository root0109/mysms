package com.mysms.core.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;

/**
 * Answers whatever a resolver throws, or passes to {@code onError}, through {@link GrpcStatuses} exactly as REST
 * answers it, so both transports fail alike and nothing internal leaks (a bare exception is INTERNAL without its
 * message, another service's status is 503).
 */
@GrpcGlobalServerInterceptor
@Order(GrpcEdge.ERROR_ORDER)
public final class GlobalGrpcExceptionHandler implements ServerInterceptor {

    private final GrpcStatuses statuses;

    public GlobalGrpcExceptionHandler(GrpcStatuses statuses) {
        this.statuses = statuses;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        GuardedCall<ReqT, RespT> guarded = new GuardedCall<>(call, statuses);
        ServerCall.Listener<ReqT> delegate;
        try {
            delegate = next.startCall(guarded, headers);
        } catch (RuntimeException e) {
            guarded.fail(e);
            return new ServerCall.Listener<>() { };
        }
        return new ExceptionHandlingServerCallListener<>(delegate, guarded::fail);
    }

    /** The call, closed at most once, with failures handed to it by gRPC itself mapped like thrown ones. */
    private static final class GuardedCall<ReqT, RespT> extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

        private final GrpcStatuses statuses;
        private final AtomicBoolean closed = new AtomicBoolean();

        GuardedCall(ServerCall<ReqT, RespT> delegate, GrpcStatuses statuses) {
            super(delegate);
            this.statuses = statuses;
        }

        @Override
        public void close(Status status, Metadata trailers) {
            // a resolver's onError(e) with a plain exception arrives as UNKNOWN carrying it: map it like a throw
            if (status.getCode() == Status.Code.UNKNOWN && status.getCause() != null) {
                fail(status.getCause());
                return;
            }
            if (closed.compareAndSet(false, true)) {
                super.close(status, trailers);
            }
        }

        /** Answers the failure as its problem; another service's status is never passed through (503). */
        void fail(Throwable failure) {
            StatusRuntimeException answer = statuses.toException(statuses.problemOf(failure));
            if (closed.compareAndSet(false, true)) {
                Metadata trailers = answer.getTrailers();
                super.close(answer.getStatus(), trailers == null ? new Metadata() : trailers);
            }
        }
    }
}
