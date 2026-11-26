package com.mysms.students.admission.events;

import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import com.mysms.students.admission.config.AdmissionEventsConfig;
import com.mysms.students.admission.repository.PrivacyErasureRepository;
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
 * admission's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1): each application
 * whose applicant is the subject is erased with its guardians and emergency contacts — cleared (placeholders where the
 * schema requires a value) and soft-deleted; the admission register stays in sis. Rows a legal hold covers are kept and
 * counted. An application knows its applicant's person only once one is linked (applicantPersonId); the guardians of
 * an application are not linked to a person, so a guardian's own request finds nothing here.
 * admission.applicant_not_admitted is not swept: most applications that were never admitted have no person, and a
 * sweep is keyed by person (privacy-client), so its runs answer failed.
 */
@Component
public class AdmissionPrivacyTaskHandler extends PrivacyTaskHandler {

    /** The tables of a subject's erasure, the application itself last. */
    static final List<ErasureStep> ERASURE = List.of(
            delete("admission.application_guardian", "contact", PrivacyErasureRepository.GUARDIAN_ROWS,
                    PrivacyErasureRepository.ERASE_GUARDIAN),
            delete("admission.application_emergency_contact", "contact",
                    PrivacyErasureRepository.EMERGENCY_CONTACT_ROWS, PrivacyErasureRepository.ERASE_EMERGENCY_CONTACT),
            delete("admission.application", "identity", PrivacyErasureRepository.APPLICATION_ROWS,
                    PrivacyErasureRepository.ERASE_APPLICATION));

    public AdmissionPrivacyTaskHandler(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AdmissionDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AdmissionEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AdmissionEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(AdmissionDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("admission", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    private static ErasureStep delete(String table, String categoryCode, String rows, String erase) {
        return new ErasureStep(table, categoryCode, rows, erase, true, List.of());
    }
}
