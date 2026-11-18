package com.mysms.careers.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (careers.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class CareersEventsConfig {

    public static final String OUTBOX = "careersOutboxWriter";
    public static final String AUDIT = "careersAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter careersOutboxWriter(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("careers", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter careersAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
