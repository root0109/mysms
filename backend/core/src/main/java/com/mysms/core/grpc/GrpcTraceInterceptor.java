package com.mysms.core.grpc;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.trace.TraceContext;
import com.mysms.core.trace.TraceFilter;
import com.mysms.core.trace.TraceHolder;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.util.Optional;
import org.slf4j.MDC;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;

/**
 * The gRPC twin of {@link TraceFilter}, outermost on every call: continues or starts the W3C trace, binds it around
 * every step, and returns the trace id as {@code x-trace-id}.
 */
@GrpcGlobalServerInterceptor
@Order(GrpcEdge.TRACE_ORDER)
public final class GrpcTraceInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        TraceContext trace = TraceContext.parse(headers.get(GrpcEdge.TRACEPARENT))
                .map(TraceContext::newChild)
                .orElseGet(TraceContext::newRoot);
        CallBinding binding = () -> bind(trace);
        ServerCall<ReqT, RespT> answering = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            private volatile boolean headersSent;

            @Override
            public void sendHeaders(Metadata responseHeaders) {
                responseHeaders.put(GrpcEdge.TRACE_ID, trace.traceId());
                headersSent = true;
                super.sendHeaders(responseHeaders);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                if (!headersSent) {
                    trailers.put(GrpcEdge.TRACE_ID, trace.traceId());
                }
                super.close(status, trailers);
            }
        };
        try (CallBinding.Scope scope = binding.bind()) {
            return new BoundListener<>(next.startCall(answering, headers), binding);
        }
    }

    private static CallBinding.Scope bind(TraceContext trace) {
        Optional<TraceContext> previousTrace = TraceHolder.current();
        Optional<CallContext> previousContext = CallContextHolder.current();
        String previousMdc = MDC.get(TraceFilter.MDC_TRACE_ID);
        TraceHolder.set(trace);
        CallContextHolder.set(CallContext.anonymous(trace.traceId()));
        MDC.put(TraceFilter.MDC_TRACE_ID, trace.traceId());
        return () -> {
            previousTrace.ifPresentOrElse(TraceHolder::set, TraceHolder::clear);
            previousContext.ifPresentOrElse(CallContextHolder::set, CallContextHolder::clear);
            if (previousMdc == null) {
                MDC.remove(TraceFilter.MDC_TRACE_ID);
            } else {
                MDC.put(TraceFilter.MDC_TRACE_ID, previousMdc);
            }
        };
    }
}
