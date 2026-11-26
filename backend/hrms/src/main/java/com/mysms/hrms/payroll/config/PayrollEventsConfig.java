package com.mysms.hrms.payroll.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's outbox (payroll.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class PayrollEventsConfig {

    public static final String OUTBOX = "payrollOutboxWriter";
    public static final String AUDIT = "payrollAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter payrollOutboxWriter(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("payroll", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter payrollAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
