package com.mysms.identity.iam.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (iam.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class IamEventsConfig {

    public static final String OUTBOX = "iamOutboxWriter";
    public static final String AUDIT = "iamAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter iamOutboxWriter(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
                                        JsonMapper jsonMapper) {
        return new OutboxWriter("iam", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter iamAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
