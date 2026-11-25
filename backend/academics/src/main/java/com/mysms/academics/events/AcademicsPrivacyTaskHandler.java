package com.mysms.academics.events;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.repository.PrivacyErasureRepository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
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
 * academics' part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1), the person known
 * from academics.staff_record ({@link StaffRecordSync}): the subject's lesson plans are anonymised and kept (the
 * author's notes cleared; the plan is the school's teaching record); rows a legal hold covers are kept and counted.
 * Teaching assignments and class-teacher designations are kept untouched: operational records naming the hr.staff
 * record, which hr anonymises and keeps. Approval comments name their author by user account only (not mapped).
 * academics has no retention policy.
 */
@Component
public class AcademicsPrivacyTaskHandler extends PrivacyTaskHandler {

    static final List<ErasureStep> ERASURE = List.of(new ErasureStep("academics.lesson_plan", "identity",
            PrivacyErasureRepository.LESSON_PLAN_ROWS, PrivacyErasureRepository.ANONYMISE_LESSON_PLAN, false,
            List.of("notes")));

    public AcademicsPrivacyTaskHandler(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(AcademicsDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("academics", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }
}
