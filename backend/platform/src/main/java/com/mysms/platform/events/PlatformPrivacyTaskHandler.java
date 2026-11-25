package com.mysms.platform.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
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
 * platform's part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1). platform finds a
 * data principal where its polymorphic references name the person's record ({@code people.person}): the notes on it
 * (text replaced by {@code Erased}), its attachments (title cleared) and tags are soft-deleted, and the files it owns or
 * has attached unshared get their names cleared and are marked deleted; rows a legal hold covers are kept and counted.
 * Not erased here: the audit trail (ids and field names only, never values, and the evidence of the erasure itself),
 * what platform knows only by user account (uploader, author: iam's events carry no person), and sales enquiries
 * (prospects outside any tenant, not a person of a school). platform.access_log names no table of platform (the access
 * log is lms.access_log): its runs answer failed (privacy-client).
 */
@Component
public class PlatformPrivacyTaskHandler extends PrivacyTaskHandler {

    /** The tables of a subject's erasure, the files last (an attachment names its file). */
    static final List<ErasureStep> ERASURE = List.of(
            delete("platform.entity_note", PrivacyErasureRepository.ENTITY_NOTE_ROWS,
                    PrivacyErasureRepository.ERASE_ENTITY_NOTE),
            delete("platform.entity_attachment", PrivacyErasureRepository.ENTITY_ATTACHMENT_ROWS,
                    PrivacyErasureRepository.ERASE_ENTITY_ATTACHMENT),
            delete("platform.tag_assignment", PrivacyErasureRepository.TAG_ASSIGNMENT_ROWS,
                    PrivacyErasureRepository.ERASE_TAG_ASSIGNMENT),
            delete("platform.file_object", PrivacyErasureRepository.FILE_OBJECT_ROWS,
                    PrivacyErasureRepository.ERASE_FILE_OBJECT));

    public PlatformPrivacyTaskHandler(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(PlatformDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("platform", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected Map<String, ?> stepParams() {
        return Map.of("personType", PrivacyErasureRepository.PERSON_TYPE);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    private static ErasureStep delete(String table, String rows, String erase) {
        return new ErasureStep(table, "identity", rows, erase, true, List.of());
    }
}
