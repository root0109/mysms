package com.mysms.campus.transport.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (transport.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class TransportEventsConfig {

    public static final String OUTBOX = "transportOutboxWriter";
    public static final String AUDIT = "transportAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter transportOutboxWriter(@Qualifier(TransportDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("transport", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter transportAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
