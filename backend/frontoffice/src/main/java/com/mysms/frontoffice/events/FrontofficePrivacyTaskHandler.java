package com.mysms.frontoffice.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.frontoffice.config.FrontofficeEventsConfig;
import com.mysms.frontoffice.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import com.mysms.privacyclient.RetentionSweep;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * frontoffice's part in privacy's rights requests and its retention policy (design PRIVACY-AND-CONSENT-CENTRE §9.1,
 * §10). No register names a person: the visitor log, the call log and the postal register keep a name, a number or an
 * address as typed at the desk, the telephone directory a name and a number, and the ids they hold (the student
 * visited, the staff member met or who logged an entry) cannot be tied to a person here (frontoffice consumes no
 * sis or hr event). An erasure or export therefore answers not applicable. The frontoffice.visitor_log sweep erases
 * each visitor entry older than the cutoff by the entry's own id (a pseudo-subject, {@link PrivacyErasureRepository}):
 * a hold naming the entry keeps it and is counted, whichever action the tenant set (delete or anonymise: both clear
 * the visitor and soft-delete the entry).
 */
@Component
public class FrontofficePrivacyTaskHandler extends PrivacyTaskHandler {

    static final String RETENTION_POLICY = "frontoffice.visitor_log";

    static final Map<String, RetentionSweep> RETENTION = Map.of(RETENTION_POLICY,
            new RetentionSweep(PrivacyErasureRepository.VISITOR_LOG_SUBJECTS, List.of(
                    new ErasureStep("frontoffice.visitor_log", "contact", PrivacyErasureRepository.VISITOR_LOG_DUE_ROWS,
                            PrivacyErasureRepository.ERASE_VISITOR_LOG, true, List.of()))));

    public FrontofficePrivacyTaskHandler(@Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FrontofficeDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FrontofficeEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FrontofficeEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(FrontofficeDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("frontoffice", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    /** No table is found by a person: an erasure or export of one is not applicable. */
    @Override
    protected List<ErasureStep> erasureSteps() {
        return List.of();
    }

    @Override
    protected Map<String, RetentionSweep> retentionSweeps() {
        return RETENTION;
    }
}
