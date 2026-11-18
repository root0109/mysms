package com.mysms.lms.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (lms.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class LmsEventsConfig {

    public static final String OUTBOX = "lmsOutboxWriter";
    public static final String AUDIT = "lmsAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter lmsOutboxWriter(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("lms", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter lmsAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
