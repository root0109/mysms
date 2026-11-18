package com.mysms.core.grpc;

import io.grpc.Context;
import io.grpc.Metadata;
import net.devh.boot.grpc.common.util.InterceptorOrder;

/**
 * The gRPC edge's metadata and context keys, and the order of its interceptors (lowest outermost): trace, error
 * mapping, authentication and authorization, idempotency, then the resolver.
 */
public final class GrpcEdge {

    /** The outermost: continues the caller's trace and binds the call context every other step logs with. */
    public static final int TRACE_ORDER = InterceptorOrder.ORDER_GLOBAL_EXCEPTION_HANDLING - 100;
    /** Maps every failure inside it to a google.rpc.Status, in the starter's exception slot. */
    public static final int ERROR_ORDER = InterceptorOrder.ORDER_GLOBAL_EXCEPTION_HANDLING;
    /** auth-client: token, tenant, operation, permission; binds AuthContext. */
    public static final int AUTH_ORDER = ERROR_ORDER + 100;
    /** request_id of the operations that declare idempotency. */
    public static final int IDEMPOTENCY_ORDER = ERROR_ORDER + 200;

    /** The contract operation id ({@code <module>.<RpcMethod>}) the call addresses, set by the auth interceptor. */
    public static final Context.Key<String> OPERATION_ID = Context.key("mysms.operationId");
    /**
     * "required" or "optional" when the operation declares idempotency (its request carries {@code request_id}), set
     * by the auth interceptor; absent otherwise.
     */
    public static final Context.Key<String> IDEMPOTENCY = Context.key("mysms.idempotency");

    public static final Metadata.Key<String> TRACEPARENT =
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER);
    /** The trace id returned to the caller (REST: the X-Trace-Id header), so it can be quoted. */
    public static final Metadata.Key<String> TRACE_ID = Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    /** The refusal as a packed {@code mysms.common.v1.ErrorMessage}, in the trailers every refused call carries. */
    public static final Metadata.Key<byte[]> ERROR_MESSAGE =
            Metadata.Key.of("error-message-bin", Metadata.BINARY_BYTE_MARSHALLER);
    /** The refusal as text for logs and tools ({@code <code> <status>: <detail>}, ASCII only), next to it. */
    public static final Metadata.Key<String> ERROR_DETAILS =
            Metadata.Key.of("error-details", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcEdge() {
    }
}
