package com.mysms.assessment.events;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.repository.PrivacyErasureRepository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.KeptRows;
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
 * assessment's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): the
 * subject's homework submissions are cleared (reply, comments) and soft-deleted, and the answers and feedback of the
 * subject's assignment submissions cleared, their marks kept; the marks of record — exam results and marksheet lines —
 * are kept under assessment.after_leaving (ten years) and counted with the policy code, untouched. Rows a legal hold
 * covers are kept and counted. assessment.after_leaving archives, which is not built: its runs answer failed
 * (privacy-client).
 */
@Component
public class AssessmentPrivacyTaskHandler extends PrivacyTaskHandler {

    static final String RETENTION_POLICY = "assessment.after_leaving";

    /** The tables of a subject's erasure. */
    static final List<ErasureStep> ERASURE = List.of(
            new ErasureStep("assessment.homework_submission", "academic_record",
                    PrivacyErasureRepository.HOMEWORK_SUBMISSION_ROWS,
                    PrivacyErasureRepository.ERASE_HOMEWORK_SUBMISSION, true, List.of()),
            new ErasureStep("assessment.assignment_submission", "academic_record",
                    PrivacyErasureRepository.ASSIGNMENT_SUBMISSION_ROWS,
                    PrivacyErasureRepository.ANONYMISE_ASSIGNMENT_SUBMISSION, false, List.of("content", "feedback")));

    /** The marks of record kept under {@link #RETENTION_POLICY}. */
    static final List<KeptRows> KEPT = List.of(
            new KeptRows("assessment.exam_result", PrivacyErasureRepository.EXAM_RESULT_KEPT, RETENTION_POLICY),
            new KeptRows("assessment.marksheet_line", PrivacyErasureRepository.MARKSHEET_LINE_KEPT,
                    RETENTION_POLICY));

    public AssessmentPrivacyTaskHandler(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(AssessmentDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("assessment", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    /** The marks of record, kept and counted under {@link #RETENTION_POLICY}. */
    @Override
    protected List<KeptRows> keptRows() {
        return KEPT;
    }
}
