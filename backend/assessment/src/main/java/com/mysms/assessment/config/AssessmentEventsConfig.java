package com.mysms.assessment.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (assessment.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class AssessmentEventsConfig {

    public static final String OUTBOX = "assessmentOutboxWriter";
    public static final String AUDIT = "assessmentAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter assessmentOutboxWriter(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("assessment", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter assessmentAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
