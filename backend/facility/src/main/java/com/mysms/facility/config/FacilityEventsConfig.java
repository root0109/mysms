package com.mysms.facility.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (facility.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class FacilityEventsConfig {

    public static final String OUTBOX = "facilityOutboxWriter";
    public static final String AUDIT = "facilityAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter facilityOutboxWriter(@Qualifier(FacilityDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("facility", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter facilityAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
