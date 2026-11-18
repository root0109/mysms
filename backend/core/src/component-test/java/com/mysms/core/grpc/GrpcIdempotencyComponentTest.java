package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.rpc.ErrorInfo;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.IdempotencyLedger;
import com.mysms.core.db.IdempotencyStore;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import io.grpc.CallOptions;
import io.grpc.ClientInterceptors;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * gRPC {@code request_id} against real PostgreSQL as the application role (row-level security on), through the same
 * store as REST's Idempotency-Key: run once and replay the answer, refuse a reused key, hold a running request, keep a
 * refusal but not a server failure, require the key where declared, keep the answer only encrypted. The auth
 * interceptor is stood in for by one that marks the operation and binds the caller, as auth-client's does.
 */
class GrpcIdempotencyComponentTest {

    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000000b1");
    private static final UUID USER = UUID.fromString("0190b0a0-0000-7000-8000-0000000000ef");
    private static final Metadata.Key<String> DECLARED = Metadata.Key.of("x-idem", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TENANTLESS = Metadata.Key.of("x-no-tenant", Metadata.ASCII_STRING_MARSHALLER);
    private static final FieldCipher CIPHER = FieldCipher.fromSettings(
            "k1:" + Base64.getEncoder().encodeToString(new byte[32]), null);
    private static final Descriptor REQUEST = TestProtos.CREATE_REQUEST;
    private static final Descriptor W = TestProtos.WIDGET;
    private static final MethodDescriptor<DynamicMessage, DynamicMessage> CREATE =
            TestProtos.method("CreateWidget", REQUEST, TestProtos.GET_RESPONSE);
    private static final AtomicInteger RUNS = new AtomicInteger();
    private static final AtomicReference<CountDownLatch> HOLD = new AtomicReference<>();
    private static final CountDownLatch HELD = new CountDownLatch(1);

    private static TestDatabase database;
    private static ModuleDatabase db;
    private static Server server;
    private static ManagedChannel channel;

    @BeforeAll
    static void open() throws Exception {
        database = TestDatabase.create();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.coretest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.coretest.datasource.username", database.username())
                .withProperty("mysms.coretest.datasource.password", database.password())
                .withProperty("mysms.coretest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.coretest.datasource.migrate-password", database.migratePassword());
        db = ModuleDatabase.open("coretest", environment, true);
        GrpcStatuses statuses = new GrpcStatuses(new ConstraintMapper(MessageUtil.ofCatalogue()));
        IdempotencyLedger ledger = new IdempotencyLedger(List.of(new IdempotencyStore(db, Clock.systemUTC())), CIPHER,
                JsonMappers.create(), Clock.systemUTC());
        ServerServiceDefinition service = ServerServiceDefinition.builder("mysms.coretest.v1.WidgetService")
                .addMethod(CREATE, ServerCalls.asyncUnaryCall((request, out) -> {
                    RUNS.incrementAndGet();
                    DynamicMessage widget = (DynamicMessage) request.getField(REQUEST.findFieldByName("widget"));
                    String name = (String) widget.getField(W.findFieldByName("name"));
                    if ("conflict".equals(name)) {
                        throw new ConflictException("ACTIVE_LIMIT_REACHED", "The limit is reached.");
                    }
                    if ("boom".equals(name)) {
                        throw new IllegalStateException("failed on the server");
                    }
                    if ("slow".equals(name)) {
                        HELD.countDown();
                        await(HOLD.get());
                    }
                    out.onNext(DynamicMessage.newBuilder(TestProtos.GET_RESPONSE).setField(
                            TestProtos.GET_RESPONSE.findFieldByName("widget"), DynamicMessage.newBuilder(W)
                                    .setField(W.findFieldByName("id"), UUID.randomUUID().toString()).build()).build());
                    out.onCompleted();
                })).build();
        String name = "idem-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).addService(
                ServerInterceptors.interceptForward(service, new GrpcTraceInterceptor(),
                        new GlobalGrpcExceptionHandler(statuses), new StandInAuth(),
                        new GrpcIdempotencyInterceptor(ledger, statuses))).build().start();
        channel = InProcessChannelBuilder.forName(name).build();
    }

    @AfterAll
    static void close() {
        channel.shutdownNow();
        server.shutdownNow();
        db.close();
        database.close();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** What auth-client's interceptor does for an admitted tenant user: marks the operation, binds the caller. */
    private static final class StandInAuth implements ServerInterceptor {

        @Override
        public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers,
                                                           ServerCallHandler<Q, R> next) {
            Context marked = Context.current().withValue(GrpcEdge.OPERATION_ID, "coretest.CreateWidget");
            if (headers.get(DECLARED) != null) {
                marked = marked.withValue(GrpcEdge.IDEMPOTENCY, headers.get(DECLARED));
            }
            UUID tenant = headers.get(TENANTLESS) == null ? TENANT : null;
            CallBinding binding = () -> {
                Optional<CallContext> before = CallContextHolder.current();
                CallContextHolder.set(before.orElseThrow().withIdentity(tenant, USER));
                return () -> before.ifPresentOrElse(CallContextHolder::set, CallContextHolder::clear);
            };
            try (CallBinding.Scope scope = binding.bind()) {
                return new BoundListener<>(Contexts.interceptCall(marked, call, headers, next), binding);
            }
        }
    }

    private static DynamicMessage request(String key, String name) {
        DynamicMessage.Builder request = DynamicMessage.newBuilder(REQUEST).setField(REQUEST.findFieldByName("widget"),
                DynamicMessage.newBuilder(W).setField(W.findFieldByName("name"), name).build());
        if (key != null) {
            request.setField(REQUEST.findFieldByName("request_id"), key);
        }
        return request.build();
    }

    private record Answer(String id, Status.Code code, String reason, boolean replayed) {
    }

    private static Answer send(DynamicMessage request, String declared, boolean withTenant) {
        Metadata headers = new Metadata();
        if (declared != null) {
            headers.put(DECLARED, declared);
        }
        if (!withTenant) {
            headers.put(TENANTLESS, "1");
        }
        AtomicReference<Metadata> answeredHeaders = new AtomicReference<>();
        AtomicReference<Metadata> trailers = new AtomicReference<>();
        try {
            DynamicMessage response = ClientCalls.blockingUnaryCall(ClientInterceptors.intercept(channel,
                    MetadataUtils.newAttachHeadersInterceptor(headers),
                    MetadataUtils.newCaptureMetadataInterceptor(answeredHeaders, trailers)), CREATE,
                    CallOptions.DEFAULT, request);
            DynamicMessage widget = (DynamicMessage) response.getField(TestProtos.GET_RESPONSE.findFieldByName("widget"));
            return new Answer((String) widget.getField(W.findFieldByName("id")), Status.Code.OK, null,
                    "true".equals(answeredHeaders.get().get(GrpcIdempotencyInterceptor.REPLAYED)));
        } catch (StatusRuntimeException e) {
            String reason = null;
            com.google.rpc.Status rpc = StatusProto.fromThrowable(e);
            if (rpc != null) {
                for (Any detail : rpc.getDetailsList()) {
                    if (detail.is(ErrorInfo.class)) {
                        try {
                            reason = detail.unpack(ErrorInfo.class).getReason();
                        } catch (com.google.protobuf.InvalidProtocolBufferException invalid) {
                            throw new AssertionError(invalid);
                        }
                    }
                }
            }
            return new Answer(null, e.getStatus().getCode(), reason,
                    e.getTrailers() != null && "true".equals(e.getTrailers().get(GrpcIdempotencyInterceptor.REPLAYED)));
        }
    }

    @Test
    void theFirstRequestRunsAndTheSameRequestIsAnsweredFromTheStore() {
        int before = RUNS.get();
        String key = UUID.randomUUID().toString();
        Answer first = send(request(key, "Pump"), "optional", true);
        assertThat(first.code()).isEqualTo(Status.Code.OK);
        assertThat(first.replayed()).isFalse();
        Answer again = send(request(key, "Pump"), "optional", true);
        assertThat(again.id()).as("the kept answer").isEqualTo(first.id());
        assertThat(again.replayed()).isTrue();
        assertThat(RUNS.get() - before).as("ran once").isEqualTo(1);
        String stored = CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736")
                .withIdentity(TENANT, USER), () -> TestTransactions.of(db).read(() -> db.jdbc().sql(
                        "SELECT response_body FROM coretest.idempotency_key WHERE tenant_id = :tenantId AND "
                                + "idempotency_key = :key").param("key", UUID.fromString(key)).query(String.class)
                        .single()));
        assertThat(stored).as("kept only encrypted").startsWith("v1.k1.").doesNotContain(first.id());

        Answer reused = send(request(key, "Valve"), "optional", true);
        assertThat(reused.code()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(reused.reason()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        // without a key, or without a tenant (public and platform calls), every request runs
        send(request(null, "Pump"), "optional", true);
        send(request(key, "Pump"), "optional", false);
        send(request(key, "Pump"), "optional", false);
        assertThat(RUNS.get() - before).isEqualTo(4);
        // an operation that declares no idempotency ignores the field
        send(request(key, "Other"), null, true);
        assertThat(RUNS.get() - before).isEqualTo(5);
    }

    @Test
    void aRefusalIsKeptButAServerFailureIsNot() {
        int before = RUNS.get();
        String conflict = UUID.randomUUID().toString();
        Answer refused = send(request(conflict, "conflict"), "optional", true);
        assertThat(refused.code()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(refused.reason()).isEqualTo("ACTIVE_LIMIT_REACHED");
        Answer replayed = send(request(conflict, "conflict"), "optional", true);
        assertThat(replayed.code()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(replayed.reason()).isEqualTo("ACTIVE_LIMIT_REACHED");
        assertThat(replayed.replayed()).isTrue();
        assertThat(RUNS.get() - before).isEqualTo(1);

        String failing = UUID.randomUUID().toString();
        assertThat(send(request(failing, "boom"), "optional", true).code()).isEqualTo(Status.Code.INTERNAL);
        assertThat(send(request(failing, "boom"), "optional", true).code()).isEqualTo(Status.Code.INTERNAL);
        assertThat(RUNS.get() - before).as("a server failure is not kept: the retry runs").isEqualTo(3);
    }

    @Test
    void aRunningRequestHoldsItsKey() throws Exception {
        String key = UUID.randomUUID().toString();
        HOLD.set(new CountDownLatch(1));
        Metadata declared = new Metadata();
        declared.put(DECLARED, "optional");
        ListenableFuture<DynamicMessage> running = ClientCalls.futureUnaryCall(ClientInterceptors.intercept(channel,
                MetadataUtils.newAttachHeadersInterceptor(declared)).newCall(CREATE, CallOptions.DEFAULT),
                request(key, "slow"));
        await(HELD);
        Answer meanwhile = send(request(key, "slow"), "optional", true);
        assertThat(meanwhile.code()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(meanwhile.reason()).isEqualTo("REQUEST_IN_PROGRESS");
        HOLD.get().countDown();
        assertThat(running.get(10, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void aRequiredKeyMustBeAUuidAndPresent() {
        assertThat(send(request(null, "Pump"), "required", true).code()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(send(request("not-a-uuid", "Pump"), "required", true).code()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(send(request(UUID.randomUUID().toString(), "Pump"), "required", true).code())
                .isEqualTo(Status.Code.OK);
    }
}
