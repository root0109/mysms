package com.mysms.core.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.error.MessageUtil;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.validation.DtoValidator;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.UUID;
import net.devh.boot.grpc.server.event.GrpcServerShutdownEvent;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import net.devh.boot.grpc.server.serverfactory.GrpcServerLifecycle;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * The gRPC edge core contributes to the starter's server: its beans, the order the starter applies the interceptors in
 * (lowest first, outermost), and the health service's per-service statuses.
 */
class GrpcEdgeAutoConfigurationTest {

    @Test
    void contributesTheEdgeInTheOrderOfGrpcEdge() {
        GrpcEdgeAutoConfiguration config = new GrpcEdgeAutoConfiguration();
        GrpcStatuses statuses = config.grpcStatuses(MessageUtil.ofCatalogue());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(config.protoBridge(JsonMappers.create(), new DtoValidator(factory.getValidator()),
                    new StaticListableBeanFactory().getBeanProvider(ProtoEnumValues.class))).isNotNull();
        }
        Object trace = config.grpcTraceInterceptor();
        Object errors = config.globalGrpcExceptionHandler(statuses);
        Object idempotency = config.grpcIdempotencyInterceptor(null, statuses);
        List<Object> sorted = new java.util.ArrayList<>(List.of(idempotency, errors, trace));
        sorted.sort(AnnotationAwareOrderComparator.INSTANCE);
        assertThat(sorted).containsExactly(trace, errors, idempotency);
        assertThat(GrpcEdge.TRACE_ORDER).isLessThan(GrpcEdge.ERROR_ORDER);
        assertThat(GrpcEdge.ERROR_ORDER).isLessThan(GrpcEdge.AUTH_ORDER);
        assertThat(GrpcEdge.AUTH_ORDER).isLessThan(GrpcEdge.IDEMPOTENCY_ORDER);
    }

    @Test
    void everyServiceIsServingOnceStartedAndNotServingFromShutdown() throws Exception {
        HealthStatusManager health = new HealthStatusManager();
        GrpcServingStatus serving = new GrpcEdgeAutoConfiguration().grpcServingStatus(health);
        String name = "health-" + UUID.randomUUID();
        ServerServiceDefinition widgets = ServerServiceDefinition.builder(new ServiceDescriptor(
                "mysms.coretest.v1.WidgetService")).build();
        Server server = InProcessServerBuilder.forName(name).addService(health.getHealthService())
                .addService(widgets).build().start();
        var channel = InProcessChannelBuilder.forName(name).build();
        try {
            HealthGrpc.HealthBlockingStub stub = HealthGrpc.newBlockingStub(channel);
            serving.started(new GrpcServerStartedEvent(Mockito.mock(GrpcServerLifecycle.class), server, "in-process", -1));
            assertThat(status(stub, "mysms.coretest.v1.WidgetService")).isEqualTo(ServingStatus.SERVING);
            assertThat(status(stub, "")).isEqualTo(ServingStatus.SERVING);

            serving.stopping(new GrpcServerShutdownEvent(Mockito.mock(GrpcServerLifecycle.class), server));
            assertThat(status(stub, "mysms.coretest.v1.WidgetService")).isEqualTo(ServingStatus.NOT_SERVING);
            assertThat(status(stub, "")).isEqualTo(ServingStatus.NOT_SERVING);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private static ServingStatus status(HealthGrpc.HealthBlockingStub stub, String service) {
        HealthCheckResponse response = stub.check(HealthCheckRequest.newBuilder().setService(service).build());
        return response.getStatus();
    }
}
