package com.mysms.core.config;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.IdempotencyStore;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.PartitionMaintenanceJob;
import com.mysms.core.error.GlobalExceptionHandler;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.sdk.SdkClientFactory;
import com.mysms.core.sdk.TracePropagationInterceptor;
import com.mysms.core.trace.TraceFilter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.IdempotencyFilter;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Cross-cutting infrastructure every module gets by having `core` on its classpath (JAVA-CODING-RULES §2). */
@AutoConfiguration
public class CoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TraceFilter traceFilter() {
        return new TraceFilter();
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public ProblemResponseWriter problemResponseWriter(JsonMapper jsonMapper) {
        return new ProblemResponseWriter(jsonMapper);
    }

    @Bean
    public DtoValidator dtoValidator(Validator validator) {
        return new DtoValidator(validator);
    }

    /** Encryption of the `*_enc` columns; master keys only from the environment (DEPLOYMENT.md "Secrets"). */
    @Bean
    @ConditionalOnMissingBean
    public FieldCipher fieldCipher(Environment environment) {
        return FieldCipher.fromSettings(environment.getProperty("mysms.secrets.keys"),
                environment.getProperty("mysms.secrets.active-key"));
    }

    /**
     * The Idempotency-Key store of every module running here (all but master, which has none): keyed POSTs are
     * kept and replayed for 24 h (api/README "Idempotency"). Stored responses are encrypted, so a module with a store
     * needs the field-encryption master key.
     */
    @Bean
    public IdempotencyFilter idempotencyFilter(ObjectProvider<ModuleDatabase> databases, FieldCipher cipher,
                                               ProblemResponseWriter problems, JsonMapper jsonMapper, Clock clock) {
        List<IdempotencyStore> stores = databases.orderedStream().filter(d -> !"master".equals(d.module()))
                .map(d -> new IdempotencyStore(d, clock)).toList();
        if (!stores.isEmpty()) {
            cipher.requireConfigured();
        }
        return new IdempotencyFilter(stores, cipher, problems, jsonMapper, clock);
    }

    /**
     * Monthly partitions of every module database here, kept {@code mysms.partitions.months-ahead} (default 3) months
     * ahead; months older than {@code mysms.partitions.retention-months} are dropped only when that is set (default
     * 0: never). Off with {@code mysms.partitions.enabled=false} (for example on all but one replica).
     */
    @Bean
    @ConditionalOnProperty(name = "mysms.partitions.enabled", havingValue = "true", matchIfMissing = true)
    public PartitionMaintenanceJob partitionMaintenanceJob(ObjectProvider<ModuleDatabase> databases,
                                                           Environment environment, Clock clock) {
        return new PartitionMaintenanceJob(databases.orderedStream().toList(), clock,
                environment.getProperty("mysms.partitions.interval", Duration.class, Duration.ofHours(24)),
                environment.getProperty("mysms.partitions.months-ahead", Integer.class, 3),
                environment.getProperty("mysms.partitions.retention-months", Integer.class, 0));
    }

    @Bean
    public TracePropagationInterceptor tracePropagationInterceptor() {
        return new TracePropagationInterceptor();
    }

    @Bean
    public SdkClientFactory sdkClientFactory(RestClient.Builder builder,
                                             ObjectProvider<ClientHttpRequestInterceptor> interceptors) {
        return new SdkClientFactory(builder, interceptors.orderedStream().toList());
    }
}
