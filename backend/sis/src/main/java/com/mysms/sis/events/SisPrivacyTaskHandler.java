package com.mysms.sis.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import com.mysms.privacyclient.RetentionSweep;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.repository.PrivacyErasureRepository;
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
 * sis's part in privacy's erasure and export requests and its retention policy (design PRIVACY-AND-CONSENT-CENTRE
 * §9.1, §10). An erasure clears and soft-deletes the subject's health records, activities and leave requests as a
 * student, clears why the student left, and anonymises the subject as a guardian (occupation, income, contact); the
 * admission register (student number and dates, enrolments, promotions) and discipline incidents are kept for the
 * owner's decision (design §9.1). Rows a legal hold covers are kept and counted. sis.health_after_leaving clears and
 * soft-deletes the health records of students who left before the cutoff.
 */
@Component
public class SisPrivacyTaskHandler extends PrivacyTaskHandler {

    static final String HEALTH_RECORD = "sis.student_health_record";

    static final List<ErasureStep> ERASURE = List.of(
            delete(HEALTH_RECORD, "health", PrivacyErasureRepository.HEALTH_RECORD_ROWS,
                    PrivacyErasureRepository.ERASE_HEALTH_RECORD),
            delete("sis.student_activity", "academic_record", PrivacyErasureRepository.ACTIVITY_ROWS,
                    PrivacyErasureRepository.ERASE_ACTIVITY),
            delete("sis.student_leave_request", "attendance", PrivacyErasureRepository.LEAVE_REQUEST_ROWS,
                    PrivacyErasureRepository.ERASE_LEAVE_REQUEST),
            new ErasureStep("sis.guardian", "contact", PrivacyErasureRepository.GUARDIAN_ROWS,
                    PrivacyErasureRepository.ANONYMISE_GUARDIAN, false, List.of("profession", "subOccupation",
                            "designation", "organizationName", "annualIncome", "qualificationId", "mobile", "email")),
            new ErasureStep("sis.student", "academic_record", PrivacyErasureRepository.STUDENT_ROWS,
                    PrivacyErasureRepository.ANONYMISE_STUDENT, false, List.of("exitReason")));

    static final Map<String, RetentionSweep> RETENTION = Map.of("sis.health_after_leaving",
            new RetentionSweep(PrivacyErasureRepository.HEALTH_AFTER_LEAVING_SUBJECTS,
                    List.of(delete(HEALTH_RECORD, "health", PrivacyErasureRepository.HEALTH_AFTER_LEAVING_ROWS,
                            PrivacyErasureRepository.ERASE_HEALTH_RECORD))));

    public SisPrivacyTaskHandler(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(SisDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("sis", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    @Override
    protected Map<String, RetentionSweep> retentionSweeps() {
        return RETENTION;
    }

    private static ErasureStep delete(String table, String categoryCode, String rows, String erase) {
        return new ErasureStep(table, categoryCode, rows, erase, true, List.of());
    }
}
