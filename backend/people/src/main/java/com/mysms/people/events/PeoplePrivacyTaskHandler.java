package com.mysms.people.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.config.PeopleEventsConfig;
import com.mysms.people.repository.PrivacyErasureRepository;
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
 * people's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1): a subject's
 * addresses, emergency contacts, education, work experience, certifications, skills, documents, public profile,
 * social links and the greetings asked for them are cleared and soft-deleted, and the person anonymised ("Erased
 * person", the national id's envelope cleared) and kept, since other modules refer to it; rows a legal hold covers
 * are kept and counted. An export answers the rows per table (no export part is written yet). people has no
 * retention policy of its own.
 */
@Component
public class PeoplePrivacyTaskHandler extends PrivacyTaskHandler {

    /** The tables of a subject's erasure, the person's own row (anonymised and kept) last. */
    static final List<ErasureStep> ERASURE = List.of(
            delete("people.person_address", "location", PrivacyErasureRepository.PERSON_ADDRESS_ROWS,
                    PrivacyErasureRepository.ERASE_PERSON_ADDRESS),
            delete("people.emergency_contact", "contact", PrivacyErasureRepository.EMERGENCY_CONTACT_ROWS,
                    PrivacyErasureRepository.ERASE_EMERGENCY_CONTACT),
            delete("people.education_history", "academic_record", PrivacyErasureRepository.EDUCATION_HISTORY_ROWS,
                    PrivacyErasureRepository.ERASE_EDUCATION_HISTORY),
            delete("people.work_experience", "identity", PrivacyErasureRepository.WORK_EXPERIENCE_ROWS,
                    PrivacyErasureRepository.ERASE_WORK_EXPERIENCE),
            delete("people.certification", "academic_record", PrivacyErasureRepository.CERTIFICATION_ROWS,
                    PrivacyErasureRepository.ERASE_CERTIFICATION),
            delete("people.person_skill", "identity", PrivacyErasureRepository.PERSON_SKILL_ROWS,
                    PrivacyErasureRepository.ERASE_PERSON_SKILL),
            delete("people.person_document", "government_id", PrivacyErasureRepository.PERSON_DOCUMENT_ROWS,
                    PrivacyErasureRepository.ERASE_PERSON_DOCUMENT),
            delete("people.public_profile", "identity", PrivacyErasureRepository.PUBLIC_PROFILE_ROWS,
                    PrivacyErasureRepository.ERASE_PUBLIC_PROFILE),
            delete("people.person_social_link", "contact", PrivacyErasureRepository.PERSON_SOCIAL_LINK_ROWS,
                    PrivacyErasureRepository.ERASE_PERSON_SOCIAL_LINK),
            delete("people.greeting", "contact", PrivacyErasureRepository.GREETING_ROWS,
                    PrivacyErasureRepository.ERASE_GREETING),
            new ErasureStep("people.person", "identity", PrivacyErasureRepository.PERSON_ROWS,
                    PrivacyErasureRepository.ANONYMISE_PERSON, false,
                    List.of("honorificId", "firstName", "middleName", "lastName", "displayName", "dateOfBirth",
                            "gender", "bloodGroup", "maritalStatus", "birthPlace", "nativePlace",
                            "nationalityCountryId", "motherTongue", "religion", "socialCategory",
                            "socialSubCategory", "nationalIdType", "nationalId", "visaInfo", "primaryEmail",
                            "primaryPhone", "alternatePhone", "photoFileId", "remarks", "workPhone", "websiteUrl",
                            "aboutMe")));

    public PeoplePrivacyTaskHandler(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PeopleDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PeopleEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PeopleEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(PeopleDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("people", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    private static ErasureStep delete(String table, String categoryCode, String rows, String erase) {
        return new ErasureStep(table, categoryCode, rows, erase, true, List.of());
    }
}
