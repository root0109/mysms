package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.GrpcEdge;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;

/**
 * Where a call to iam.UserAccountService comes from, for the operations that record it (a sign-in's session, a mailed
 * secret, an API token, an impersonation): the gRPC rendering of what REST's controller reads from the servlet
 * request — the peer's IP address ({@code getRemoteAddr()}: the connecting socket, so a proxy's address behind one, as
 * on REST) and the {@code user-agent} the client sent. The starter's server applies every global interceptor to every
 * service, so this one binds the two values for that service's calls only and passes every other call through untouched; it runs
 * innermost, after the edge (trace, errors, authentication, idempotency), so a refused or replayed call never reaches
 * it.
 */
@GrpcGlobalServerInterceptor
@Order(GrpcEdge.IDEMPOTENCY_ORDER + 100)
public class ClientOrigin implements ServerInterceptor {

    private static final Context.Key<String> ADDRESS = Context.key("mysms.iam.clientAddress");
    private static final Context.Key<String> USER_AGENT = Context.key("mysms.iam.userAgent");
    private static final Metadata.Key<String> USER_AGENT_HEADER =
            Metadata.Key.of("user-agent", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        if (!UserAccountServiceGrpc.SERVICE_NAME.equals(call.getMethodDescriptor().getServiceName())) {
            return next.startCall(call, headers);
        }
        Context origin = Context.current()
                .withValue(ADDRESS, address(call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)))
                .withValue(USER_AGENT, headers.get(USER_AGENT_HEADER));
        return Contexts.interceptCall(origin, call, headers, next);
    }

    /** The calling peer's IP address as text (REST's {@code getRemoteAddr()}), or null when not an IP socket. */
    static String address() {
        return ADDRESS.get();
    }

    /** The client's {@code user-agent}, or null when it sent none. */
    static String userAgent() {
        return USER_AGENT.get();
    }

    private static String address(SocketAddress peer) {
        return peer instanceof InetSocketAddress socket && socket.getAddress() != null
                ? socket.getAddress().getHostAddress() : null;
    }
}
