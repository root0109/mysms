package com.mysms.hrms.timeattendance.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** The area's outbox (time_attendance.outbox_event) and its audit writer. */
@Configuration(proxyBeanMethods = false)
public class TimeAttendanceEventsConfig {

    public static final String OUTBOX = "timeAttendanceOutboxWriter";
    public static final String AUDIT = "timeAttendanceAuditWriter";

    @Bean(name = OUTBOX)
    public OutboxWriter timeAttendanceOutboxWriter(
            @Qualifier(TimeAttendanceDatabaseConfig.DATABASE) ModuleDatabase database, JsonMapper jsonMapper) {
        return new OutboxWriter("time_attendance", database.jdbc(), jsonMapper);
    }

    @Bean(name = AUDIT)
    public AuditWriter timeAttendanceAuditWriter(@Qualifier(OUTBOX) OutboxWriter outbox, Clock clock) {
        return new AuditWriter(outbox, clock);
    }
}
