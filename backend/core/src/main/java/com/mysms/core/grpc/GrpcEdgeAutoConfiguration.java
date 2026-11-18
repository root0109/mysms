package com.mysms.core.grpc;

import com.mysms.core.config.CoreAutoConfiguration;
import com.mysms.core.db.IdempotencyLedger;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.validation.DtoValidator;
import io.grpc.protobuf.services.HealthStatusManager;
import net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * What core adds around the starter's gRPC server: the proto ↔ DTO bridge, the error mapping, the edge interceptors in
 * {@link GrpcEdge}'s order and the health service's per-service statuses.
 */
@AutoConfiguration(after = {CoreAutoConfiguration.class, GrpcHealthServiceAutoConfiguration.class})
public class GrpcEdgeAutoConfiguration {

    @Bean
    public ProtoBridge protoBridge(JsonMapper jsonMapper, DtoValidator validator,
                                   ObjectProvider<ProtoEnumValues> irregularEnums) {
        return new ProtoBridge(jsonMapper, validator, irregularEnums.orderedStream().toList());
    }

    @Bean
    public GrpcStatuses grpcStatuses(MessageUtil messages) {
        return new GrpcStatuses(new ConstraintMapper(messages));
    }

    @Bean
    public GrpcTraceInterceptor grpcTraceInterceptor() {
        return new GrpcTraceInterceptor();
    }

    @Bean
    public GlobalGrpcExceptionHandler globalGrpcExceptionHandler(GrpcStatuses statuses) {
        return new GlobalGrpcExceptionHandler(statuses);
    }

    @Bean
    public GrpcIdempotencyInterceptor grpcIdempotencyInterceptor(IdempotencyLedger ledger, GrpcStatuses statuses) {
        return new GrpcIdempotencyInterceptor(ledger, statuses);
    }

    @Bean
    @ConditionalOnBean(HealthStatusManager.class)
    public GrpcServingStatus grpcServingStatus(HealthStatusManager health) {
        return new GrpcServingStatus(health);
    }
}
