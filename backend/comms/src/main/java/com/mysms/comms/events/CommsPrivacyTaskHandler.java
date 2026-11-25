package com.mysms.comms.events;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.repository.PrivacyErasureRepository;
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
 * comms' part in privacy's erasure and export requests (design PRIVACY-AND-CONSENT-CENTRE §9.1), in the first wave so
 * nothing more goes out to the subject: messages not yet sent are cancelled and every message to the subject cleared,
 * in-app notifications cleared, called off and archived, notification preferences, group and page memberships,
 * conversation places, receipts, event places, calendar exports and reactions soft-deleted; what the subject wrote —
 * chat and feedback messages, mailbox mail, tasks, posts and comments — is cleared (required text becomes
 * {@code Erased}) and hidden, and feedback threads and content views about the subject's student records closed and
 * soft-deleted. Rows a legal hold covers (a safeguarding hold among them) are kept and counted. The subject is found
 * as a student, through {@code comms.student_record} ({@link StudentRecordSync}): the accounts of guardians and
 * staff, and a guardian's feedback threads, are not tied to a person in comms and are not reached. comms keeps no
 * media of a person (community media are not built), so comms.media_after_withdrawal is not swept: its runs answer
 * failed (privacy-client).
 */
@Component
public class CommsPrivacyTaskHandler extends PrivacyTaskHandler {

    /** The tables of a subject's erasure: what goes out first, a parent table after the rows that belong to it. */
    static final List<ErasureStep> ERASURE = List.of(
            step("comms.outbound_message", "contact", PrivacyErasureRepository.OUTBOUND_MESSAGE_ROWS,
                    PrivacyErasureRepository.ERASE_OUTBOUND_MESSAGE),
            new ErasureStep("comms.in_app_notification", "contact", PrivacyErasureRepository.IN_APP_NOTIFICATION_ROWS,
                    PrivacyErasureRepository.ANONYMISE_IN_APP_NOTIFICATION, false, List.of("title", "body", "smsText",
                            "actionUrl", "entityType", "entityId", "emailStatus", "smsStatus", "popupStatus",
                            "digestStatus", "archivedAt")),
            step("comms.notification_preference", "contact", PrivacyErasureRepository.NOTIFICATION_PREFERENCE_ROWS,
                    PrivacyErasureRepository.ERASE_NOTIFICATION_PREFERENCE),
            step("comms.notification_preference_set", "contact",
                    PrivacyErasureRepository.NOTIFICATION_PREFERENCE_SET_ROWS,
                    PrivacyErasureRepository.ERASE_NOTIFICATION_PREFERENCE_SET),
            step("comms.audience_group_member", "contact", PrivacyErasureRepository.AUDIENCE_GROUP_MEMBER_ROWS,
                    PrivacyErasureRepository.ERASE_AUDIENCE_GROUP_MEMBER),
            step("comms.mailbox_message", "contact", PrivacyErasureRepository.MAILBOX_MESSAGE_ROWS,
                    PrivacyErasureRepository.ERASE_MAILBOX_MESSAGE),
            step("comms.message_receipt", "contact", PrivacyErasureRepository.MESSAGE_RECEIPT_ROWS,
                    PrivacyErasureRepository.ERASE_MESSAGE_RECEIPT),
            step("comms.conversation_message", "contact", PrivacyErasureRepository.CONVERSATION_MESSAGE_ROWS,
                    PrivacyErasureRepository.ERASE_CONVERSATION_MESSAGE),
            step("comms.conversation_participant", "contact", PrivacyErasureRepository.CONVERSATION_PARTICIPANT_ROWS,
                    PrivacyErasureRepository.ERASE_CONVERSATION_PARTICIPANT),
            step("comms.feedback_message", "contact", PrivacyErasureRepository.FEEDBACK_MESSAGE_ROWS,
                    PrivacyErasureRepository.ERASE_FEEDBACK_MESSAGE),
            step("comms.feedback_thread", "contact", PrivacyErasureRepository.FEEDBACK_THREAD_ROWS,
                    PrivacyErasureRepository.ERASE_FEEDBACK_THREAD),
            step("comms.task_assignee", "contact", PrivacyErasureRepository.TASK_ASSIGNEE_ROWS,
                    PrivacyErasureRepository.ERASE_TASK_ASSIGNEE),
            step("comms.task", "contact", PrivacyErasureRepository.TASK_ROWS, PrivacyErasureRepository.ERASE_TASK),
            step("comms.event_occurrence", "contact", PrivacyErasureRepository.EVENT_OCCURRENCE_ROWS,
                    PrivacyErasureRepository.ERASE_EVENT_OCCURRENCE),
            step("comms.event_attendee", "contact", PrivacyErasureRepository.EVENT_ATTENDEE_ROWS,
                    PrivacyErasureRepository.ERASE_EVENT_ATTENDEE),
            step("comms.calendar_export", "contact", PrivacyErasureRepository.CALENDAR_EXPORT_ROWS,
                    PrivacyErasureRepository.ERASE_CALENDAR_EXPORT),
            step("comms.class_page_member", "device_usage", PrivacyErasureRepository.CLASS_PAGE_MEMBER_ROWS,
                    PrivacyErasureRepository.ERASE_CLASS_PAGE_MEMBER),
            step("comms.reaction", "device_usage", PrivacyErasureRepository.REACTION_ROWS,
                    PrivacyErasureRepository.ERASE_REACTION),
            step("comms.content_view", "device_usage", PrivacyErasureRepository.CONTENT_VIEW_ROWS,
                    PrivacyErasureRepository.ERASE_CONTENT_VIEW),
            step("comms.post_comment", "photo_media", PrivacyErasureRepository.POST_COMMENT_ROWS,
                    PrivacyErasureRepository.ERASE_POST_COMMENT),
            step("comms.post", "photo_media", PrivacyErasureRepository.POST_ROWS, PrivacyErasureRepository.ERASE_POST));

    public CommsPrivacyTaskHandler(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(CommsDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("comms", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    @Override
    protected List<ErasureStep> erasureSteps() {
        return ERASURE;
    }

    /** A table whose rows of the subject are cleared and soft-deleted. */
    private static ErasureStep step(String table, String categoryCode, String select, String erase) {
        return new ErasureStep(table, categoryCode, select, erase, true, List.of());
    }
}
