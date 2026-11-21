package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.StringValue;
import com.google.rpc.ErrorInfo;
import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestRoleData;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.grpc.GrpcEdge;
import com.mysms.core.grpc.GlobalGrpcExceptionHandler;
import com.mysms.core.grpc.GrpcStatuses;
import com.mysms.core.grpc.GrpcTraceInterceptor;
import com.mysms.core.error.ConstraintMapper;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * The audience of an operation, enforced identically by both edges over the same decision: a
 * service token is admitted only on an internal operation (with the permission its client was granted, in the tenant
 * its call names), a person's token never on one; and the gRPC edge's own answers (unknown operation, mapped
 * failures, trace id). The REST edge is the real {@link AuthFilter}, the gRPC edge an in-process server behind the
 * real interceptors; tokens are signed for real.
 */
class EdgeAudienceTest {

    private static final TestTokens TOKENS = new TestTokens();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final OperationTable OPERATIONS = OperationTable.load(List.of("authtest"));
    private static final EdgeAuthorizer AUTHORIZER = new EdgeAuthorizer(
            JwtTokenVerifier.from(new AuthClientProperties(TestTokens.ISSUER, null, TOKENS.publicKeyPem(), null,
                    List.of("authtest"), null, false)), new ClaimsPermissionChecker(), null, TestRoleData.resolver());
    private static final GrpcStatuses STATUSES = new GrpcStatuses(new ConstraintMapper(MessageUtil.ofCatalogue()));
    private static final AtomicReference<AuthContext> SEEN = new AtomicReference<>();
    private static Server server;
    private static ManagedChannel channel;

    private final AuthFilter filter = new AuthFilter(OPERATIONS, AUTHORIZER,
            new ProblemResponseWriter(JsonMapper.builder().build()), List.of());

    @BeforeAll
    static void startServer() throws IOException {
        String name = "edge-" + UUID.randomUUID();
        ServerServiceDefinition widgets = ServerServiceDefinition.builder("mysms.authtest.v1.WidgetService")
                .addMethod(method("GetWidget"), ServerCalls.asyncUnaryCall((request, out) -> {
                    if ("missing".equals(request.getValue())) {
                        throw new ResourceNotFoundException("authtest.widget", UUID.randomUUID());
                    }
                    if ("boom".equals(request.getValue())) {
                        throw new IllegalStateException("secret internals");
                    }
                    SEEN.set(AuthContext.current().orElse(null));
                    out.onNext(StringValue.of("ok"));
                    out.onCompleted();
                }))
                .addMethod(method("SyncWidgets"), ServerCalls.asyncUnaryCall((request, out) -> {
                    SEEN.set(AuthContext.current().orElse(null));
                    out.onNext(StringValue.of(CallContextHolder.current().flatMap(CallContext::tenantId)
                            .map(UUID::toString).orElse("")));
                    out.onCompleted();
                }))
                .addMethod(method("Undeclared"), ServerCalls.asyncUnaryCall((request, out) -> out.onCompleted()))
                .build();
        server = InProcessServerBuilder.forName(name).addService(ServerInterceptors.interceptForward(widgets,
                new GrpcTraceInterceptor(), new GlobalGrpcExceptionHandler(STATUSES),
                new GrpcAuthInterceptor(OPERATIONS, AUTHORIZER, STATUSES))).build().start();
        channel = InProcessChannelBuilder.forName(name).build();
    }

    @AfterAll
    static void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @AfterEach
    void clear() {
        SEEN.set(null);
        CallContextHolder.clear();
    }

    private static MethodDescriptor<StringValue, StringValue> method(String name) {
        return MethodDescriptor.<StringValue, StringValue>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName("mysms.authtest.v1.WidgetService", name))
                .setRequestMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
                .setResponseMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
                .build();
    }

    private static String call(String method, Metadata headers, String value) {
        return ClientCalls.blockingUnaryCall(io.grpc.ClientInterceptors.intercept(channel,
                MetadataUtils.newAttachHeadersInterceptor(headers)), method(method), CallOptions.DEFAULT,
                StringValue.of(value)).getValue();
    }

    private static Status.Code grpcFails(String method, String token, String tenant) {
        Metadata headers = new Metadata();
        if (token != null) {
            headers.put(GrpcAuthInterceptor.AUTHORIZATION, "Bearer " + token);
        }
        if (tenant != null) {
            headers.put(GrpcAuthInterceptor.TENANT, tenant);
        }
        try {
            call(method, headers, "");
        } catch (StatusRuntimeException e) {
            return e.getStatus().getCode();
        }
        throw new AssertionError(method + " was admitted");
    }

    private MockHttpServletResponse rest(String method, String path, String token, String tenant) throws Exception {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        if (tenant != null) {
            request.addHeader(AuthFilter.TENANT_HEADER, tenant);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                SEEN.set(AuthContext.current().orElse(null));
            }
        });
        return response;
    }

    @Test
    void aServiceTokenCallsAnInternalOperationInTheTenantItsCallNames() throws Exception {
        String service = TOKENS.service("org", "authtest.widget.sync");
        assertThat(rest("POST", "/v1/authtest/widgets:sync", service, TENANT.toString()).getStatus()).isEqualTo(200);
        assertThat(SEEN.get().service()).contains("org");
        assertThat(SEEN.get().tenantId()).contains(TENANT);
        assertThat(SEEN.get().userId()).isEqualTo(AuthContext.serviceUserId("org"));
        assertThat(SEEN.get().allInstitutions()).isTrue();

        assertThat(call("SyncWidgets", headers(service, TENANT.toString()), "")).isEqualTo(TENANT.toString());
        assertThat(SEEN.get().service()).contains("org");
        // without a tenant the call acts in none
        assertThat(call("SyncWidgets", headers(service, null), "")).isEmpty();
        assertThat(rest("POST", "/v1/authtest/widgets:sync", service, null).getStatus()).isEqualTo(200);
        assertThat(SEEN.get().tenantId()).isEmpty();
    }

    @Test
    void aServiceTokenNeedsThePermissionAndAWellFormedTenant() throws Exception {
        String ungranted = TOKENS.service("org");
        assertThat(rest("POST", "/v1/authtest/widgets:sync", ungranted, null).getStatus()).isEqualTo(403);
        assertThat(grpcFails("SyncWidgets", ungranted, null)).isEqualTo(Status.Code.PERMISSION_DENIED);
        String service = TOKENS.service("org", "authtest.widget.sync");
        assertThat(rest("POST", "/v1/authtest/widgets:sync", service, "not-a-uuid").getStatus()).isEqualTo(400);
        assertThat(grpcFails("SyncWidgets", service, "not-a-uuid")).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void aServiceTokenIsRefusedOnEveryOtherOperation() throws Exception {
        String service = TOKENS.service("org", "authtest.widget.read", "authtest.widget.create");
        assertThat(rest("GET", "/v1/authtest/widgets/" + UUID.randomUUID(), service, null).getStatus()).isEqualTo(403);
        assertThat(rest("POST", "/v1/authtest/widgets", service, null).getStatus()).isEqualTo(403);
        // even where no token is needed: a service calls those without one
        assertThat(rest("GET", "/v1/authtest/widgets", service, null).getStatus()).isEqualTo(403);
        assertThat(grpcFails("GetWidget", service, null)).isEqualTo(Status.Code.PERMISSION_DENIED);
    }

    @Test
    void aPersonsTokenIsRefusedOnAnInternalOperation() throws Exception {
        String user = TOKENS.tenantUser(TENANT, USER, "authtest.widget.sync", "authtest.widget.read");
        String operator = TOKENS.platformOperator(USER, "*");
        assertThat(rest("POST", "/v1/authtest/widgets:sync", user, null).getStatus()).isEqualTo(403);
        assertThat(rest("POST", "/v1/authtest/widgets:sync", operator, null).getStatus()).isEqualTo(403);
        assertThat(grpcFails("SyncWidgets", user, null)).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(rest("POST", "/v1/authtest/widgets:sync", null, null).getStatus()).isEqualTo(401);
        assertThat(grpcFails("SyncWidgets", null, null)).isEqualTo(Status.Code.UNAUTHENTICATED);
        // a person's tenant is the token's: a tenant header is ignored for people
        UUID other = UUID.randomUUID();
        assertThat(call("GetWidget", headers(user, other.toString()), "")).isEqualTo("ok");
        assertThat(SEEN.get().tenantId()).contains(TENANT);
        assertThat(rest("GET", "/v1/authtest/widgets/" + UUID.randomUUID(), user, other.toString()).getStatus())
                .isEqualTo(200);
        assertThat(SEEN.get().tenantId()).contains(TENANT);
        assertThat(AuthContext.current()).as("cleared after the call").isEmpty();
    }

    @Test
    void theGrpcEdgeAnswersLikeTheRestEdge() {
        String user = TOKENS.tenantUser(TENANT, USER, "authtest.widget.read");
        assertThat(grpcFails("GetWidget", null, null)).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(grpcFails("GetWidget", "garbage", null)).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(grpcFails("GetWidget", TOKENS.tenantUser(TENANT, USER), null)).isEqualTo(Status.Code.PERMISSION_DENIED);
        // a method of a served service that is no contract operation is refused, like an undeclared route
        assertThat(grpcFails("Undeclared", user, null)).isEqualTo(Status.Code.NOT_FOUND);

        StatusRuntimeException missing = catchStatus(() -> call("GetWidget", headers(user, null), "missing"));
        assertThat(missing.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        ErrorInfo info = errorInfo(missing);
        assertThat(info.getReason()).isEqualTo("NOT_FOUND");
        assertThat(info.getMetadataMap()).containsEntry("status", "404").containsKey("traceId");
        assertThat(missing.getTrailers().get(GrpcEdge.TRACE_ID)).isEqualTo(info.getMetadataMap().get("traceId"));

        StatusRuntimeException boom = catchStatus(() -> call("GetWidget", headers(user, null), "boom"));
        assertThat(boom.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(boom.getStatus().getDescription()).isNullOrEmpty();
        assertThat(errorInfo(boom).getReason()).isEqualTo("INTERNAL");
    }

    private static Metadata headers(String token, String tenant) {
        Metadata headers = new Metadata();
        headers.put(GrpcAuthInterceptor.AUTHORIZATION, "Bearer " + token);
        if (tenant != null) {
            headers.put(GrpcAuthInterceptor.TENANT, tenant);
        }
        return headers;
    }

    private static StatusRuntimeException catchStatus(Runnable call) {
        try {
            call.run();
        } catch (StatusRuntimeException e) {
            return e;
        }
        throw new AssertionError("the call succeeded");
    }

    private static ErrorInfo errorInfo(StatusRuntimeException failure) {
        try {
            for (com.google.protobuf.Any detail : StatusProto.fromThrowable(failure).getDetailsList()) {
                if (detail.is(ErrorInfo.class)) {
                    return detail.unpack(ErrorInfo.class);
                }
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError("no ErrorInfo");
    }
}
