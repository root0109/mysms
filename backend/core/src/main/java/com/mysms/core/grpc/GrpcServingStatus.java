package com.mysms.core.grpc;

import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import net.devh.boot.grpc.server.event.GrpcServerShutdownEvent;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.context.event.EventListener;

/**
 * What the gRPC health service answers per service: SERVING once the server has started, NOT_SERVING from the moment
 * it begins to shut down, so a load balancer stops sending calls while the running ones finish.
 */
public final class GrpcServingStatus {

    private final HealthStatusManager health;

    public GrpcServingStatus(HealthStatusManager health) {
        this.health = health;
    }

    @EventListener
    public void started(GrpcServerStartedEvent event) {
        for (ServerServiceDefinition service : event.getServer().getServices()) {
            health.setStatus(service.getServiceDescriptor().getName(), ServingStatus.SERVING);
        }
        health.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.SERVING);
    }

    @EventListener
    public void stopping(GrpcServerShutdownEvent event) {
        health.enterTerminalState();
    }
}
