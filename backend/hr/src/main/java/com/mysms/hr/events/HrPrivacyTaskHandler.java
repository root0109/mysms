package com.mysms.hr.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.hr.config.HrDatabaseConfig;
import com.mysms.hr.config.HrEventsConfig;
import com.mysms.hr.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * hr's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1): the free text of the
 * subject's leave applications (the applicant's remarks, the decision's comments) is cleared, and each staff record of
 * the subject anonymised (qualification, specialisation, interests) and kept, since payroll, attendance and the leave
 * ledger refer to it; rows a legal hold covers are kept and counted. The person's name, contacts and documents live in
 * people, the bank accounts in payroll. hr has no retention policy of its own.
 */
@Component
public class HrPrivacyTaskHandler extends PrivacyTaskHandler {

    /** The tables of a subject's erasure, the staff record (anonymised and kept) last. */
    static final List<ErasureStep> ERASURE = List.of(
            new ErasureStep("hr.leave_application", "attendance", PrivacyErasureRepository.LEAVE_APPLICATION_ROWS,
                    PrivacyErasureRepository.ANONYMISE_LEAVE_APPLICATION, false,
                    List.of("remarks", "decisionComments")),
            new ErasureStep("hr.staff", "identity", PrivacyErasureRepository.STAFF_ROWS,
                    PrivacyErasureRepository.ANONYMISE_STAFF, false, List.of("subDesignation",
                            "highestQualificationId", "qualificationDetail", "specialization", "interests")));

    public HrPrivacyTaskHandler(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(HrDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(HrEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(HrEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(HrDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("hr", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }
}
