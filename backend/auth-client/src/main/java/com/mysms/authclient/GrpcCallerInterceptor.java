package com.mysms.authclient;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.grpc.GrpcEdge;
import com.mysms.core.trace.TraceContext;
import com.mysms.core.trace.RequestCost;
import com.mysms.core.trace.TraceHolder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adds to every outgoing gRPC call the caller's own token, institution and tenant, the trace and a default deadline;
 * a call made with {@link ServiceTokenProvider#callCredentials} keeps its own identity. A call inside a database
 * transaction is refused with {@link IllegalStateException}: a transaction never waits on another service. Every call
 * counts towards the request's {@link RequestCost}.
 */
@GrpcGlobalClientInterceptor
public final class GrpcCallerInterceptor implements ClientInterceptor {

    private final Duration deadline;

    /** @param deadline how long a call may take when its stub sets no deadline of its own */
    public GrpcCallerInterceptor(Duration deadline) {
        this.deadline = deadline;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A call to " + method.getFullMethodName() + " was made inside a database "
                    + "transaction; call other modules before or after the transaction, never inside it.");
        }
        RequestCost.remoteCall();
        CallOptions options = callOptions.getDeadline() == null
                ? callOptions.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS) : callOptions;
        boolean ownCredentials = options.getCredentials() != null;
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                TraceContext trace = TraceHolder.current().map(TraceContext::newChild).orElseGet(TraceContext::newRoot);
                headers.put(GrpcEdge.TRACEPARENT, trace.toHeader());
                if (!ownCredentials) {
                    AuthContextHolder.token().ifPresent(token ->
                            headers.put(GrpcAuthInterceptor.AUTHORIZATION, EdgeAuthorizer.BEARER_PREFIX + token));
                    CallContextHolder.current().flatMap(CallContext::tenantId).ifPresent(tenant ->
                            headers.put(GrpcAuthInterceptor.TENANT, tenant.toString()));
                }
                AuthContextHolder.current().flatMap(AuthContext::activeInstitutionId).ifPresent(institution ->
                        headers.put(GrpcAuthInterceptor.INSTITUTION, institution.toString()));
                super.start(responseListener, headers);
            }
        };
    }
}
