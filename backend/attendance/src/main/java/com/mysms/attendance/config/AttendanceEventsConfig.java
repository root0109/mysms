package com.mysms.attendance.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The module's own outbox (attendance.outbox_event) and the audit writer on top of it (JAVA-CODING-RULES §2, §4). */
@Configuration(proxyBeanMethods = false)
public class AttendanceEventsConfig {

    public static final String OUTBOX = "attendanceOutboxWriter";
    public static final String AUDIT = "attendanceAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter attendanceOutboxWriter(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper jsonMapper) {
        return new OutboxWriter("attendance", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter attendanceAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
