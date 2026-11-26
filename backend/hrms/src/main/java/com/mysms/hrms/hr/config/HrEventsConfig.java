package com.mysms.hrms.hr.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (hr.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class HrEventsConfig {

    public static final String OUTBOX = "hrOutboxWriter";
    public static final String AUDIT = "hrAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter hrOutboxWriter(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("hr", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter hrAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
