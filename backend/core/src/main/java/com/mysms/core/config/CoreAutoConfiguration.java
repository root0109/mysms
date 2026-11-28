package com.mysms.core.config;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.IdempotencyLedger;
import com.mysms.core.db.InProcessIdempotency;
import com.mysms.core.db.IdempotencyStore;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.PartitionMaintenanceJob;
import com.mysms.core.error.ConstraintMapper;
import com.mysms.core.error.GlobalExceptionHandler;
import com.mysms.core.error.MessageUtil;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.jobs.JobRuntime;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.jobs.ScheduledJob;
import com.mysms.core.json.JsonMappers;
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
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/** Cross-cutting infrastructure every module gets by having `core` on its classpath. */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class CoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * The process's JSON mapper ({@link JsonMappers}). Named as Spring Boot names its own, which therefore backs off,
     * as the reference service does.
     */
    @Bean
    @Primary
    public JsonMapper jacksonJsonMapper() {
        return JsonMappers.create();
    }

    @Bean
    public TraceFilter traceFilter() {
        return new TraceFilter();
    }

    /** The catalogue of error sentences by code and language (messages/mysms*.properties). */
    @Bean
    public MessageUtil messageUtil() {
        return MessageUtil.ofCatalogue();
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler(MessageUtil messages) {
        return new GlobalExceptionHandler(new ConstraintMapper(messages));
    }

    @Bean
    public ProblemResponseWriter problemResponseWriter(JsonMapper jsonMapper) {
        return new ProblemResponseWriter(jsonMapper);
    }

    @Bean
    public DtoValidator dtoValidator(Validator validator) {
        return new DtoValidator(validator);
    }

    /** Encryption of the `*_enc` columns; master keys only from the environment. */
    @Bean
    @ConditionalOnMissingBean
    public FieldCipher fieldCipher(Environment environment) {
        return FieldCipher.fromSettings(environment.getProperty("mysms.secrets.keys"),
                environment.getProperty("mysms.secrets.active-key"));
    }

    /**
     * The idempotency stores of the modules running here (master has none): keyed requests are replayed for 24 h.
     * Stored responses are encrypted, so a module with a store needs the field-encryption master key.
     */
    @Bean
    public IdempotencyLedger idempotencyLedger(ObjectProvider<ModuleDatabase> databases, FieldCipher cipher,
                                               JsonMapper jsonMapper, Clock clock) {
        List<IdempotencyStore> stores = databases.orderedStream().filter(d -> !"master".equals(d.module()))
                .map(d -> new IdempotencyStore(d, clock)).toList();
        if (!stores.isEmpty()) {
            cipher.requireConfigured();
        }
        return new IdempotencyLedger(stores, cipher, jsonMapper, clock);
    }

    /** The request_id rules of a sibling area's operation called in process. */
    @Bean
    public InProcessIdempotency inProcessIdempotency(IdempotencyLedger ledger, JsonMapper jsonMapper) {
        return new InProcessIdempotency(ledger, jsonMapper);
    }

    @Bean
    public IdempotencyFilter idempotencyFilter(IdempotencyLedger ledger, ProblemResponseWriter problems) {
        return new IdempotencyFilter(ledger, problems);
    }

    /**
     * Monthly partitions of every module database, {@code mysms.partitions.months-ahead} months ahead; months older
     * than {@code mysms.partitions.retention-months} are dropped only when that is above 0. The keys come from the
     * modules' application.properties (CLAUDE.md §5); without {@code mysms.partitions.enabled} there is no job.
     */
    @Bean
    @ConditionalOnProperty(name = "mysms.partitions.enabled", havingValue = "true")
    public PartitionMaintenanceJob partitionMaintenanceJob(ObjectProvider<ModuleDatabase> databases,
                                                           Environment environment, Clock clock) {
        return new PartitionMaintenanceJob(databases.orderedStream().toList(), clock,
                environment.getRequiredProperty("mysms.partitions.interval", Duration.class),
                environment.getRequiredProperty("mysms.partitions.months-ahead", Integer.class),
                environment.getRequiredProperty("mysms.partitions.retention-months", Integer.class));
    }

    /**
     * The scheduled jobs of this process's modules ({@link ScheduledJob}, each module's {@link JobSchedule} bean):
     * polled every {@code mysms.jobs.poll-interval}, each due tenant leased for {@code mysms.jobs.lease}, at most
     * {@code mysms.jobs.batch-size} runs per module and pass. The keys come from the modules' application.properties
     * (CLAUDE.md §5); without {@code mysms.jobs.enabled} there is no runtime.
     */
    @Bean
    @ConditionalOnProperty(name = "mysms.jobs.enabled", havingValue = "true")
    public JobRuntime jobRuntime(ObjectProvider<JobSchedule> schedules, ObjectProvider<ScheduledJob> jobs,
                                 Environment environment, Clock clock) {
        return new JobRuntime(schedules.orderedStream().toList(), jobs.orderedStream().toList(), clock,
                environment.getRequiredProperty("mysms.jobs.poll-interval", Duration.class),
                environment.getRequiredProperty("mysms.jobs.lease", Duration.class),
                environment.getRequiredProperty("mysms.jobs.batch-size", Integer.class));
    }
}
