package com.mysms.people.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's own outbox (people.outbox_event) and the audit writer on top of it (JAVA-CODING-RULES §2, §4). */
@Configuration(proxyBeanMethods = false)
public class PeopleEventsConfig {

    public static final String OUTBOX = "peopleOutboxWriter";
    public static final String AUDIT = "peopleAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter peopleOutboxWriter(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
                                           JsonMapper jsonMapper) {
        return new OutboxWriter("people", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter peopleAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
