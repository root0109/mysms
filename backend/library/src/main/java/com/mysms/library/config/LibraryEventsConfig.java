package com.mysms.library.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's own outbox (library.outbox_event) and the audit writer on top of it (JAVA-CODING-RULES §2, §4). */
@Configuration(proxyBeanMethods = false)
public class LibraryEventsConfig {

    public static final String OUTBOX = "libraryOutboxWriter";
    public static final String AUDIT = "libraryAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter libraryOutboxWriter(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("library", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter libraryAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
