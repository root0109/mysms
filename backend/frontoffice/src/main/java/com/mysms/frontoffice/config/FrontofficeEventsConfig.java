package com.mysms.frontoffice.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (frontoffice.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class FrontofficeEventsConfig {

    public static final String OUTBOX = "frontofficeOutboxWriter";
    public static final String AUDIT = "frontofficeAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter frontofficeOutboxWriter(@Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("frontoffice", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter frontofficeAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
