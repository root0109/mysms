package com.mysms.comms.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.repository.ReferenceCleanupRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Keeps comms's logical references consistent when their owners are deleted (references are not checked when written).
 * For each owner's {@code .deleted} event it clears the referencing column or soft-deletes the referencing row, audited
 * and announced on the outbox, inside the event's tenant; the inbox delivers each event exactly once.
 */
@Component
public class CommsReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("academics.class_section.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_POST_AUDIENCE_CLASS_CLASS_SECTION_ID,
                    "comms.post_audience_class", null))),
        Map.entry("iam.permission.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTIFICATION_RULE_PERMISSION_ID,
                    "comms.notification_rule", null))),
        Map.entry("iam.role.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTICE_AUDIENCE_ROLE_ID,
                    "comms.notice_audience", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTIFICATION_PREFERENCE_SET_ROLE_ID,
                    "comms.notification_preference_set", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTIFICATION_RULE_ROLE_ID,
                    "comms.notification_rule", null))),
        Map.entry("iam.user_account.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_CALENDAR_EXPORT_USER_ID,
                    "comms.calendar_export", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_EVENT_OWNER_USER_ID,
                    "comms.event", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_EVENT_ATTENDEE_USER_ID,
                    "comms.event_attendee", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_EVENT_OCCURRENCE_USER_ID,
                    "comms.event_occurrence", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTIFICATION_PREFERENCE_SET_USER_ID,
                    "comms.notification_preference_set", null))),
        Map.entry("lms.cohort.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTICE_COHORT_ID,
                    "comms.notice", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_EVENT_COHORT_ID,
                    "comms.event", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_EVENT_OCCURRENCE_COHORT_ID,
                    "comms.event_occurrence", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_CALENDAR_EXPORT_COHORT_ID,
                    "comms.calendar_export", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTIFICATION_COHORT_SETTING_COHORT_ID,
                    "comms.notification_cohort_setting", null))),
        Map.entry("lms.forum_topic.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_NOTICE_DISCUSSION_TOPIC_ID,
                    "comms.notice", "discussionTopicId"),
            new Step(ReferenceCleanupRepository.CLEAR_NOTICE_VERSION_DISCUSSION_TOPIC_ID,
                    "comms.notice_version", "discussionTopicId"))),
        Map.entry("org.institution.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_EVENT_INSTITUTION_INSTITUTION_ID,
                    "comms.event_institution", null),
            new Step(ReferenceCleanupRepository.ARCHIVE_NOTICE_AUDIENCE_INSTITUTION_ID,
                    "comms.notice_audience", null))),
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_CLASS_PAGE_COVER_IMAGE_FILE_ID,
                    "comms.class_page", "coverImageFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_EVENT_IMAGE_FILE_ID,
                    "comms.event", "imageFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_FEEDBACK_MESSAGE_ATTACHMENT_FILE_ID,
                    "comms.feedback_message", "attachmentFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_MAGAZINE_COVER_IMAGE_FILE_ID,
                    "comms.magazine", "coverImageFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_MAGAZINE_DOCUMENT_FILE_ID,
                    "comms.magazine", "documentFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_NOTICE_ATTACHMENT_FILE_ID,
                    "comms.notice", "attachmentFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_NOTICE_BACKGROUND_IMAGE_FILE_ID,
                    "comms.notice_background", "imageFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_POST_COMMENT_ATTACHMENT_FILE_ID,
                    "comms.post_comment", "attachmentFileId"))),
        Map.entry("sis.student.deleted", List.of(
            new Step(ReferenceCleanupRepository.ARCHIVE_CONTENT_VIEW_STUDENT_ID,
                    "comms.content_view", null),
            new Step(ReferenceCleanupRepository.CLEAR_MAILBOX_MESSAGE_ABOUT_STUDENT_ID,
                    "comms.mailbox_message", "aboutStudentId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public CommsReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "comms";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return STEPS.keySet().stream().collect(Collectors.toMap(type -> type, type -> (EventHandler) this::handle));
    }

    /** Applies every step of the deleted owner; returns how many rows changed. */
    public int handle(EventEnvelope event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        int changed = 0;
        for (Step step : STEPS.getOrDefault(event.type(), List.of())) {
            for (UUID id : repository.apply(step.statement(), event.aggregateId(), now)) {
                if (step.field() == null) {
                    audit.recordDelete(step.entity(), id);
                    outbox.publish(step.entity() + CommsEvents.DELETED, step.entity(), id, CommsEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + CommsEvents.UPDATED, step.entity(), id, CommsEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
