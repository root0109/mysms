package com.mysms.admission.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (admission.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class AdmissionEventsConfig {

    public static final String OUTBOX = "admissionOutboxWriter";
    public static final String AUDIT = "admissionAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter admissionOutboxWriter(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("admission", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter admissionAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
