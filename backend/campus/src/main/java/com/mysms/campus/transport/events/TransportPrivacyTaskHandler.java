package com.mysms.campus.transport.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import com.mysms.campus.transport.config.TransportDatabaseConfig;
import com.mysms.campus.transport.config.TransportEventsConfig;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * transport's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1): transport cannot
 * find a data principal's rows, so every task answers not applicable. A student's transport arrangement names the
 * sis.student record, not the person privacy's tasks carry, and transport consumes no event that would tell it the
 * student's person (a projection fed by sis.student events, as attendance.member_record, is the owner's decision).
 * Routes and stops are the school's, not a person's. transport has no retention policy.
 */
@Component
public class TransportPrivacyTaskHandler extends PrivacyTaskHandler {

    public TransportPrivacyTaskHandler(@Qualifier(TransportDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(TransportDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(TransportEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(TransportEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(TransportDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("transport", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    /** No table maps to a person: see the class comment. */
    @Override
    protected List<ErasureStep> erasureSteps() {
        return List.of();
    }
}
