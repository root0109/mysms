package com.mysms.students.careers.events;

import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.config.CareersEventsConfig;
import com.mysms.students.careers.repository.PrivacyErasureRepository;
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
 * careers' part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1): the subject's
 * job-search preferences — the profile's every preference, its preferred functions and industries — are cleared and
 * soft-deleted; rows a legal hold covers are kept and counted. Job applications (the résumé file, the status) name
 * only the applicant's login account, which careers cannot tie to a person (iam's account events carry the id alone):
 * they are not touched and an erasure is answered for the preferences only. The employer side — openings and the
 * function and industry catalogues — is not the subject's and stays. careers has no retention policy.
 */
@Component
public class CareersPrivacyTaskHandler extends PrivacyTaskHandler {

    /** The tables of a subject's erasure, the preference profile (which the others belong to) last. */
    static final List<ErasureStep> ERASURE = List.of(
            new ErasureStep("careers.candidate_preferred_function", "identity",
                    PrivacyErasureRepository.PREFERRED_FUNCTION_ROWS,
                    PrivacyErasureRepository.ERASE_PREFERRED_FUNCTION, true, List.of()),
            new ErasureStep("careers.candidate_preferred_industry", "identity",
                    PrivacyErasureRepository.PREFERRED_INDUSTRY_ROWS,
                    PrivacyErasureRepository.ERASE_PREFERRED_INDUSTRY, true, List.of()),
            new ErasureStep("careers.candidate_preference", "identity",
                    PrivacyErasureRepository.CANDIDATE_PREFERENCE_ROWS,
                    PrivacyErasureRepository.ERASE_CANDIDATE_PREFERENCE, true, List.of()));

    public CareersPrivacyTaskHandler(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CareersDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CareersEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CareersEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(CareersDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("careers", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }
}
