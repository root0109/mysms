package com.mysms.authclient;

import com.mysms.core.error.Problem;
import com.mysms.core.grpc.BoundListener;
import com.mysms.core.grpc.CallBinding;
import com.mysms.core.grpc.GrpcEdge;
import com.mysms.core.grpc.GrpcStatuses;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;

/**
 * The gRPC edge: admits or refuses a call through the same {@link EdgeAuthorizer} as {@link AuthFilter}, binds the
 * caller around every step, and passes the operation and its idempotency to the interceptors inside
 * ({@link GrpcEdge#OPERATION_ID}, {@link GrpcEdge#IDEMPOTENCY}).
 */
@GrpcGlobalServerInterceptor
@Order(GrpcEdge.AUTH_ORDER)
public final class GrpcAuthInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> INSTITUTION =
            Metadata.Key.of("x-institution-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> TENANT = Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    /** The school a guest reads ({@link AuthFilter#TENANT_CODE_HEADER}). */
    public static final Metadata.Key<String> TENANT_CODE =
            Metadata.Key.of("x-tenant-code", Metadata.ASCII_STRING_MARSHALLER);
    /** The package of every MySMS gRPC service ({@code mysms.<module>.v1.<Service>}); every other is infrastructure. */
    private static final String MYSMS_SERVICES = "mysms.";
    private static final Metadata.Key<String> CHALLENGE =
            Metadata.Key.of("www-authenticate", Metadata.ASCII_STRING_MARSHALLER);

    private final OperationTable operations;
    private final EdgeAuthorizer authorizer;
    private final GrpcStatuses statuses;

    public GrpcAuthInterceptor(OperationTable operations, EdgeAuthorizer authorizer, GrpcStatuses statuses) {
        this.operations = operations;
        this.authorizer = authorizer;
        this.statuses = statuses;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String service = call.getMethodDescriptor().getServiceName();
        if (service != null && !service.startsWith(MYSMS_SERVICES)) {
            // the starter's health and reflection services carry no MySMS data and need no token
            return next.startCall(call, headers);
        }
        Optional<OperationRule> rule = operations.forGrpcMethod(call.getMethodDescriptor().getFullMethodName());
        if (rule.isEmpty()) {
            return refuse(call, new Problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such operation.", List.of()), false);
        }
        EdgeDecision decision = authorizer.decide(rule.get(), headers.get(AUTHORIZATION), headers.get(INSTITUTION),
                headers.get(TENANT), headers.get(TENANT_CODE));
        if (decision instanceof EdgeDecision.Refused refused) {
            return refuse(call, new Problem(refused.status(), refused.code(), refused.detail(), List.of()),
                    refused.challenge());
        }
        EdgeDecision.Admitted admitted = (EdgeDecision.Admitted) decision;
        Context marked = Context.current().withValue(GrpcEdge.OPERATION_ID, rule.get().operationId());
        if (rule.get().idempotency() != null) {
            marked = marked.withValue(GrpcEdge.IDEMPOTENCY, rule.get().idempotency());
        }
        CallBinding binding = () -> EdgeIdentity.bind(admitted);
        try (CallBinding.Scope scope = binding.bind()) {
            return new BoundListener<>(Contexts.interceptCall(marked, call, headers, next), binding);
        }
    }

    private <ReqT, RespT> ServerCall.Listener<ReqT> refuse(ServerCall<ReqT, RespT> call, Problem problem,
                                                           boolean challenge) {
        StatusRuntimeException answer = statuses.toException(problem);
        Metadata trailers = answer.getTrailers() == null ? new Metadata() : answer.getTrailers();
        if (challenge) {
            trailers.put(CHALLENGE, "Bearer error=\"invalid_token\"");
        }
        call.close(answer.getStatus(), trailers);
        return new ServerCall.Listener<>() { };
    }
}
