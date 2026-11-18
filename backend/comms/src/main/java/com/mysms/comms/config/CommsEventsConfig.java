package com.mysms.comms.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (comms.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class CommsEventsConfig {

    public static final String OUTBOX = "commsOutboxWriter";
    public static final String AUDIT = "commsAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter commsOutboxWriter(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("comms", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter commsAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
