package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.DynamicMessage;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.trace.TraceHolder;
import io.grpc.CallOptions;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.ServerCalls;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/**
 * The trace and error interceptors around a real (in-process) gRPC server: the caller's trace is continued and
 * returned, the call context is bound while the resolver runs and gone afterwards, and every way a resolver can fail
 * is answered with the mapped status.
 */
class GrpcEdgeInterceptorsTest {

    private static final GrpcStatuses STATUSES = new GrpcStatuses(new ConstraintMapper(MessageUtil.ofCatalogue()));
    private static final MethodDescriptor<DynamicMessage, DynamicMessage> GET =
            TestProtos.method("GetWidget", TestProtos.WIDGET, TestProtos.GET_RESPONSE);
    private static final AtomicReference<String> SEEN_TRACE = new AtomicReference<>();
    private static Server server;
    private static ManagedChannel channel;

    @BeforeAll
    static void start() throws Exception {
        String name = "core-edge-" + UUID.randomUUID();
        ServerServiceDefinition service = ServerServiceDefinition.builder("mysms.coretest.v1.WidgetService")
                .addMethod(GET, ServerCalls.asyncUnaryCall((request, out) -> {
                    SEEN_TRACE.set(CallContextHolder.require().traceId());
                    String id = (String) request.getField(TestProtos.WIDGET.findFieldByName("id"));
                    switch (id) {
                        case "missing" -> throw new ResourceNotFoundException("coretest.widget", UUID.randomUUID());
                        case "duplicate" -> throw new DuplicateKeyException("uq_widget");
                        case "on-error" -> out.onError(new IllegalStateException("internal detail"));
                        // a stub call to another service made without IntegrationGrpcClientException.call
                        case "stub" -> throw Status.PERMISSION_DENIED.withDescription("callee's internal detail")
                                .asRuntimeException();
                        case "status" -> out.onError(Status.RESOURCE_EXHAUSTED.withDescription("slow down")
                                .asRuntimeException());
                        default -> {
                            out.onNext(DynamicMessage.getDefaultInstance(TestProtos.GET_RESPONSE));
                            out.onCompleted();
                        }
                    }
                })).build();
        ServerInterceptor startFails = new ServerInterceptor() {
            @Override
            public <Q, R> io.grpc.ServerCall.Listener<Q> interceptCall(io.grpc.ServerCall<Q, R> call, Metadata headers,
                                                                       io.grpc.ServerCallHandler<Q, R> next) {
                if ("fail".equals(headers.get(Metadata.Key.of("x-start", Metadata.ASCII_STRING_MARSHALLER)))) {
                    throw new IllegalStateException("start failed");
                }
                return next.startCall(call, headers);
            }
        };
        server = InProcessServerBuilder.forName(name).addService(ServerInterceptors.interceptForward(service,
                new GrpcTraceInterceptor(), new GlobalGrpcExceptionHandler(STATUSES), startFails)).build().start();
        channel = InProcessChannelBuilder.forName(name).build();
    }

    @AfterAll
    static void stop() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    private static Status.Code fails(String id, Metadata headers) {
        try {
            ClientCalls.blockingUnaryCall(ClientInterceptors.intercept(channel,
                    MetadataUtils.newAttachHeadersInterceptor(headers)), GET, CallOptions.DEFAULT,
                    DynamicMessage.newBuilder(TestProtos.WIDGET).setField(TestProtos.WIDGET.findFieldByName("id"), id)
                            .build());
        } catch (StatusRuntimeException e) {
            assertThat(String.valueOf(e.getStatus().getDescription())).doesNotContain("internal detail")
                    .doesNotContain("start failed").doesNotContain("uq_widget");
            return e.getStatus().getCode();
        }
        return Status.Code.OK;
    }

    @Test
    void continuesTheCallersTraceAndReturnsItsId() {
        Metadata headers = new Metadata();
        headers.put(GrpcEdge.TRACEPARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        AtomicReference<Metadata> answered = new AtomicReference<>();
        ClientCalls.blockingUnaryCall(ClientInterceptors.intercept(channel,
                MetadataUtils.newAttachHeadersInterceptor(headers),
                MetadataUtils.newCaptureMetadataInterceptor(answered, new AtomicReference<>())), GET,
                CallOptions.DEFAULT, DynamicMessage.getDefaultInstance(TestProtos.WIDGET));
        assertThat(SEEN_TRACE.get()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(answered.get().get(GrpcEdge.TRACE_ID)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(fails("ok", new Metadata())).isEqualTo(Status.Code.OK);
        assertThat(SEEN_TRACE.get()).as("a new trace without a traceparent").hasSize(32)
                .isNotEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(CallContextHolder.current()).isEmpty();
        assertThat(TraceHolder.current()).isEmpty();
    }

    @Test
    void answersEveryFailureWithItsMappedStatus() {
        assertThat(fails("missing", new Metadata())).isEqualTo(Status.Code.NOT_FOUND);
        assertThat(fails("duplicate", new Metadata())).isEqualTo(Status.Code.ALREADY_EXISTS);
        assertThat(fails("on-error", new Metadata())).isEqualTo(Status.Code.INTERNAL);
        assertThat(fails("status", new Metadata())).isEqualTo(Status.Code.RESOURCE_EXHAUSTED);
        assertThat(fails("stub", new Metadata())).as("another service's answer is never passed through")
                .isEqualTo(Status.Code.UNAVAILABLE);
        Metadata failing = new Metadata();
        failing.put(Metadata.Key.of("x-start", Metadata.ASCII_STRING_MARSHALLER), "fail");
        assertThat(fails("ok", failing)).isEqualTo(Status.Code.INTERNAL);
    }
}
