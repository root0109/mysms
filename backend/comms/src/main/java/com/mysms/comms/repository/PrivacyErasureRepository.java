package com.mysms.comms.repository;

/**
 * The statements of comms' privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code CommsPrivacyTaskHandler} through privacy-client: per table, the ids of a subject's rows (soft-deleted rows
 * included, since soft delete is not erasure) and the statement that erases one row. comms names people by login
 * account and by student record; both are tied to the subject through {@code comms.student_record}, kept from sis's
 * student events, so a subject is found as a student (their own account, their student records).
 */
public final class PrivacyErasureRepository {

    public static final String OUTBOUND_MESSAGE_ROWS = """
            SELECT m.id
              FROM comms.outbound_message m
             WHERE m.tenant_id = :tenantId
               AND m.recipient_user_id IN (SELECT r.user_id FROM comms.student_record r
                                            WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY m.id
            """;
    /** A message not yet sent is cancelled; address and content go (both required: placeholders). */
    public static final String ERASE_OUTBOUND_MESSAGE = """
            UPDATE comms.outbound_message
               SET status = CASE WHEN status IN ('queued', 'processing') THEN 'cancelled' ELSE status END,
                   recipient = 'erased', subject = NULL, body = 'Erased', template_params = NULL, action_url = NULL,
                   provider_response = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String IN_APP_NOTIFICATION_ROWS = """
            SELECT n.id
              FROM comms.in_app_notification n
             WHERE n.tenant_id = :tenantId
               AND n.recipient_user_id IN (SELECT r.user_id FROM comms.student_record r
                                            WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY n.id
            """;
    /**
     * The notification has no soft delete: its text and target are cleared, every delivery still pending is called
     * off and it is archived (hidden from the inbox).
     */
    public static final String ANONYMISE_IN_APP_NOTIFICATION = """
            UPDATE comms.in_app_notification
               SET title = NULL, body = NULL, sms_text = NULL, action_url = NULL, entity_type = NULL, entity_id = NULL,
                   email_status = CASE WHEN email_status = 'pending' THEN 'not_required' ELSE email_status END,
                   sms_status = CASE WHEN sms_status = 'pending' THEN 'not_required' ELSE sms_status END,
                   popup_status = CASE WHEN popup_status = 'pending' THEN 'not_required' ELSE popup_status END,
                   digest_status = CASE WHEN digest_status = 'pending' THEN 'not_required' ELSE digest_status END,
                   archived_at = COALESCE(archived_at, :now), updated_at = :now
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The subject's own mailbox and the mail about their student records. */
    public static final String MAILBOX_MESSAGE_ROWS = """
            SELECT m.id
              FROM comms.mailbox_message m
             WHERE m.tenant_id = :tenantId
               AND (m.owner_user_id IN (SELECT r.user_id FROM comms.student_record r
                                         WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
                    OR m.about_student_id IN (SELECT s.student_id FROM comms.student_record s
                                               WHERE s.tenant_id = :tenantId AND s.person_id = :subjectId))
             ORDER BY m.id
            """;
    public static final String ERASE_MAILBOX_MESSAGE = """
            UPDATE comms.mailbox_message
               SET about_student_id = NULL, from_address = NULL, from_display = NULL, to_addresses = NULL,
                   subject = NULL, body = NULL, provider_message_id = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String CONVERSATION_MESSAGE_ROWS = """
            SELECT m.id
              FROM comms.conversation_message m
             WHERE m.tenant_id = :tenantId
               AND m.sender_user_id IN (SELECT r.user_id FROM comms.student_record r
                                         WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY m.id
            """;
    /** The subject's chat message: its text (required) becomes the placeholder. */
    public static final String ERASE_CONVERSATION_MESSAGE = """
            UPDATE comms.conversation_message
               SET body = 'Erased', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String MESSAGE_RECEIPT_ROWS = """
            SELECT m.id
              FROM comms.message_receipt m
             WHERE m.tenant_id = :tenantId
               AND m.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY m.id
            """;
    public static final String ERASE_MESSAGE_RECEIPT = """
            UPDATE comms.message_receipt
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String CONVERSATION_PARTICIPANT_ROWS = """
            SELECT p.id
              FROM comms.conversation_participant p
             WHERE p.tenant_id = :tenantId
               AND p.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY p.id
            """;
    public static final String ERASE_CONVERSATION_PARTICIPANT = """
            UPDATE comms.conversation_participant
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The subject's own feedback messages and every message of a thread about their student records. */
    public static final String FEEDBACK_MESSAGE_ROWS = """
            SELECT m.id
              FROM comms.feedback_message m
             WHERE m.tenant_id = :tenantId
               AND (m.sender_user_id IN (SELECT r.user_id FROM comms.student_record r
                                          WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
                    OR m.thread_id IN (SELECT t.id FROM comms.feedback_thread t
                                        WHERE t.tenant_id = :tenantId
                                          AND t.student_id IN (SELECT s.student_id FROM comms.student_record s
                                                                WHERE s.tenant_id = :tenantId
                                                                  AND s.person_id = :subjectId)))
             ORDER BY m.id
            """;
    /** The message (required) becomes the placeholder, the attachment goes. */
    public static final String ERASE_FEEDBACK_MESSAGE = """
            UPDATE comms.feedback_message
               SET message = 'Erased', attachment_file_id = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String FEEDBACK_THREAD_ROWS = """
            SELECT t.id
              FROM comms.feedback_thread t
             WHERE t.tenant_id = :tenantId
               AND t.student_id IN (SELECT s.student_id FROM comms.student_record s
                                     WHERE s.tenant_id = :tenantId AND s.person_id = :subjectId)
             ORDER BY t.id
            """;
    /** A thread holds ids only: it is closed and soft-deleted. */
    public static final String ERASE_FEEDBACK_THREAD = """
            UPDATE comms.feedback_thread
               SET status = 'closed', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String AUDIENCE_GROUP_MEMBER_ROWS = """
            SELECT m.id
              FROM comms.audience_group_member m
             WHERE m.tenant_id = :tenantId
               AND m.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY m.id
            """;
    public static final String ERASE_AUDIENCE_GROUP_MEMBER = """
            UPDATE comms.audience_group_member
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String TASK_ASSIGNEE_ROWS = """
            SELECT a.id
              FROM comms.task_assignee a
             WHERE a.tenant_id = :tenantId
               AND a.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY a.id
            """;
    public static final String ERASE_TASK_ASSIGNEE = """
            UPDATE comms.task_assignee
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String TASK_ROWS = """
            SELECT t.id
              FROM comms.task t
             WHERE t.tenant_id = :tenantId
               AND t.owner_user_id IN (SELECT r.user_id FROM comms.student_record r
                                        WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY t.id
            """;
    /** The subject's own task: title and checklist (both required) become the placeholder. */
    public static final String ERASE_TASK = """
            UPDATE comms.task
               SET title = 'Erased', checklist = 'Erased', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String EVENT_OCCURRENCE_ROWS = """
            SELECT o.id
              FROM comms.event_occurrence o
             WHERE o.tenant_id = :tenantId
               AND o.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY o.id
            """;
    /** The subject's personal copy of an event: its own title and description go. */
    public static final String ERASE_EVENT_OCCURRENCE = """
            UPDATE comms.event_occurrence
               SET title = NULL, description = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String EVENT_ATTENDEE_ROWS = """
            SELECT a.id
              FROM comms.event_attendee a
             WHERE a.tenant_id = :tenantId
               AND a.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY a.id
            """;
    public static final String ERASE_EVENT_ATTENDEE = """
            UPDATE comms.event_attendee
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String CALENDAR_EXPORT_ROWS = """
            SELECT c.id
              FROM comms.calendar_export c
             WHERE c.tenant_id = :tenantId
               AND c.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY c.id
            """;
    public static final String ERASE_CALENDAR_EXPORT = """
            UPDATE comms.calendar_export
               SET property_value = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String CLASS_PAGE_MEMBER_ROWS = """
            SELECT m.id
              FROM comms.class_page_member m
             WHERE m.tenant_id = :tenantId
               AND m.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY m.id
            """;
    /** Membership, follow and reaction of a class page go. */
    public static final String ERASE_CLASS_PAGE_MEMBER = """
            UPDATE comms.class_page_member
               SET is_following = false, reaction = NULL, is_active = false, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String REACTION_ROWS = """
            SELECT x.id
              FROM comms.reaction x
             WHERE x.tenant_id = :tenantId
               AND x.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY x.id
            """;
    public static final String ERASE_REACTION = """
            UPDATE comms.reaction
               SET reaction = NULL, is_active = false, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String POST_COMMENT_ROWS = """
            SELECT c.id
              FROM comms.post_comment c
             WHERE c.tenant_id = :tenantId
               AND c.author_user_id IN (SELECT r.user_id FROM comms.student_record r
                                         WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY c.id
            """;
    /** The subject's comment: text and attachment go, the comment is hidden (replies to it stay). */
    public static final String ERASE_POST_COMMENT = """
            UPDATE comms.post_comment
               SET body = NULL, attachment_file_id = NULL, is_active = false, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String POST_ROWS = """
            SELECT p.id
              FROM comms.post p
             WHERE p.tenant_id = :tenantId
               AND p.author_user_id IN (SELECT r.user_id FROM comms.student_record r
                                         WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY p.id
            """;
    /** The subject's post: its text (required) becomes the placeholder, one not yet published is cancelled. */
    public static final String ERASE_POST = """
            UPDATE comms.post
               SET body = 'Erased', status = CASE WHEN status IN ('draft', 'pending') THEN 'cancelled' ELSE status END,
                   deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The views of the subject's student records' content, and the subject's own views. */
    public static final String CONTENT_VIEW_ROWS = """
            SELECT v.id
              FROM comms.content_view v
             WHERE v.tenant_id = :tenantId
               AND (v.student_id IN (SELECT s.student_id FROM comms.student_record s
                                      WHERE s.tenant_id = :tenantId AND s.person_id = :subjectId)
                    OR v.viewer_user_id IN (SELECT r.user_id FROM comms.student_record r
                                             WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId))
             ORDER BY v.id
            """;
    public static final String ERASE_CONTENT_VIEW = """
            UPDATE comms.content_view
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String NOTIFICATION_PREFERENCE_ROWS = """
            SELECT p.id
              FROM comms.notification_preference p
             WHERE p.tenant_id = :tenantId
               AND p.preference_set_id IN (SELECT s.id FROM comms.notification_preference_set s
                                            WHERE s.tenant_id = :tenantId
                                              AND s.user_id IN (SELECT r.user_id FROM comms.student_record r
                                                                 WHERE r.tenant_id = :tenantId
                                                                   AND r.person_id = :subjectId))
             ORDER BY p.id
            """;
    public static final String ERASE_NOTIFICATION_PREFERENCE = """
            UPDATE comms.notification_preference
               SET property_value = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String NOTIFICATION_PREFERENCE_SET_ROWS = """
            SELECT s.id
              FROM comms.notification_preference_set s
             WHERE s.tenant_id = :tenantId
               AND s.user_id IN (SELECT r.user_id FROM comms.student_record r
                                  WHERE r.tenant_id = :tenantId AND r.person_id = :subjectId)
             ORDER BY s.id
            """;
    public static final String ERASE_NOTIFICATION_PREFERENCE_SET = """
            UPDATE comms.notification_preference_set
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
