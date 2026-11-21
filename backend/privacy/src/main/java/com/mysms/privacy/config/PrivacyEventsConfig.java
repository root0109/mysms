package com.mysms.privacy.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (privacy.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class PrivacyEventsConfig {

    public static final String OUTBOX = "privacyOutboxWriter";
    public static final String AUDIT = "privacyAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter privacyOutboxWriter(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("privacy", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter privacyAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
