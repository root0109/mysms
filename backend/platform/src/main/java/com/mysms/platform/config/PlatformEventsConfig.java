package com.mysms.platform.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's own outbox (platform.outbox_event) and the audit writer on top of it. */
@Configuration(proxyBeanMethods = false)
public class PlatformEventsConfig {

    public static final String OUTBOX = "platformOutboxWriter";
    public static final String AUDIT = "platformAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter platformOutboxWriter(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("platform", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter platformAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
