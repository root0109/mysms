package com.mysms.master.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's own outbox (master.outbox_event) and the audit writer on top of it. */
@Configuration(proxyBeanMethods = false)
public class MasterEventsConfig {

    public static final String OUTBOX = "masterOutboxWriter";
    public static final String AUDIT = "masterAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter masterOutboxWriter(@Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database,
                                           JsonMapper jsonMapper) {
        return new OutboxWriter("master", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter masterAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
