package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of lms's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * lms.access_log.user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_ACCESS_LOG_USER_ID = """
            UPDATE lms.access_log
               SET user_id = NULL
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.annotation.file_id, on platform.file_object deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_ANNOTATION_FILE_ID = """
            UPDATE lms.annotation
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND file_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.annotation.media_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_ANNOTATION_MEDIA_FILE_ID = """
            UPDATE lms.annotation
               SET media_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND media_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.annotation_share.shared_with_user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_ANNOTATION_SHARE_SHARED_WITH_USER_ID = """
            UPDATE lms.annotation_share
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND shared_with_user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.assignment.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_ASSIGNMENT_INSTITUTION_ID = """
            UPDATE lms.assignment
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.assignment_group_member.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_ASSIGNMENT_GROUP_MEMBER_USER_ID = """
            UPDATE lms.assignment_group_member
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.assignment_submission.attachment_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_ASSIGNMENT_SUBMISSION_ATTACHMENT_FILE_ID = """
            UPDATE lms.assignment_submission
               SET attachment_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND attachment_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.bookmark.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_BOOKMARK_USER_ID = """
            UPDATE lms.bookmark
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.catalog_banner.image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_CATALOG_BANNER_IMAGE_FILE_ID = """
            UPDATE lms.catalog_banner
               SET image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.chapter.scorm_package_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_CHAPTER_SCORM_PACKAGE_FILE_ID = """
            UPDATE lms.chapter
               SET scorm_package_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND scorm_package_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort.academic_term_id, on org.academic_term deletion: null the column.
     */
    public static final String CLEAR_COHORT_ACADEMIC_TERM_ID = """
            UPDATE lms.cohort
               SET academic_term_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND academic_term_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort.confirmation_template_id, on comms.message_template deletion: null the column.
     */
    public static final String CLEAR_COHORT_CONFIRMATION_TEMPLATE_ID = """
            UPDATE lms.cohort
               SET confirmation_template_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND confirmation_template_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort.grading_scheme_id, on assessment.grading_scheme deletion: null the column.
     */
    public static final String CLEAR_COHORT_GRADING_SCHEME_ID = """
            UPDATE lms.cohort
               SET grading_scheme_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND grading_scheme_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort.intro_video_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_COHORT_INTRO_VIDEO_FILE_ID = """
            UPDATE lms.cohort
               SET intro_video_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND intro_video_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort.meta_image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_COHORT_META_IMAGE_FILE_ID = """
            UPDATE lms.cohort
               SET meta_image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND meta_image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort.program_id, on academics.program deletion: null the column.
     */
    public static final String CLEAR_COHORT_PROGRAM_ID = """
            UPDATE lms.cohort
               SET program_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND program_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort_enrollment.acquisition_source_id, on finance.acquisition_source deletion: null the column.
     */
    public static final String CLEAR_COHORT_ENROLLMENT_ACQUISITION_SOURCE_ID = """
            UPDATE lms.cohort_enrollment
               SET acquisition_source_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND acquisition_source_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort_enrollment.graded_by, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_COHORT_ENROLLMENT_GRADED_BY = """
            UPDATE lms.cohort_enrollment
               SET graded_by = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND graded_by = :ownerId
            RETURNING id
            """;

    /**
     * lms.cohort_holiday.holiday_id, on org.holiday deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_COHORT_HOLIDAY_HOLIDAY_ID = """
            UPDATE lms.cohort_holiday
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND holiday_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.cohort_teacher.role_id, on iam.role deletion: null the column.
     */
    public static final String CLEAR_COHORT_TEACHER_ROLE_ID = """
            UPDATE lms.cohort_teacher
               SET role_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND role_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.content_item_rating.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_CONTENT_ITEM_RATING_USER_ID = """
            UPDATE lms.content_item_rating
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.content_view_session.file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_CONTENT_VIEW_SESSION_FILE_ID = """
            UPDATE lms.content_view_session
               SET file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.content_view_session.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_CONTENT_VIEW_SESSION_USER_ID = """
            UPDATE lms.content_view_session
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.course.grading_scheme_id, on assessment.grading_scheme deletion: null the column.
     */
    public static final String CLEAR_COURSE_GRADING_SCHEME_ID = """
            UPDATE lms.course
               SET grading_scheme_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND grading_scheme_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.course.image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_COURSE_IMAGE_FILE_ID = """
            UPDATE lms.course
               SET image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.course_author_profile.photo_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_COURSE_AUTHOR_PROFILE_PHOTO_FILE_ID = """
            UPDATE lms.course_author_profile
               SET photo_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND photo_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.course_testimonial.photo_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_COURSE_TESTIMONIAL_PHOTO_FILE_ID = """
            UPDATE lms.course_testimonial
               SET photo_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND photo_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.discussion_prompt.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_DISCUSSION_PROMPT_INSTITUTION_ID = """
            UPDATE lms.discussion_prompt
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.drive_item.file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_DRIVE_ITEM_FILE_ID = """
            UPDATE lms.drive_item
               SET file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.drive_item.owner_user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_DRIVE_ITEM_OWNER_USER_ID = """
            UPDATE lms.drive_item
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND owner_user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.drive_item_share.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_DRIVE_ITEM_SHARE_USER_ID = """
            UPDATE lms.drive_item_share
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.ebook.file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_EBOOK_FILE_ID = """
            UPDATE lms.ebook
               SET file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.ebook.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_EBOOK_INSTITUTION_ID = """
            UPDATE lms.ebook
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.ebook.source_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_EBOOK_SOURCE_FILE_ID = """
            UPDATE lms.ebook
               SET source_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND source_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.enrollment_request.decided_by, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_ENROLLMENT_REQUEST_DECIDED_BY = """
            UPDATE lms.enrollment_request
               SET decided_by = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND decided_by = :ownerId
            RETURNING id
            """;

    /**
     * lms.enrollment_request.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_ENROLLMENT_REQUEST_USER_ID = """
            UPDATE lms.enrollment_request
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.external_tool_institution.institution_id, on org.institution deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_EXTERNAL_TOOL_INSTITUTION_INSTITUTION_ID = """
            UPDATE lms.external_tool_institution
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.external_tool_resource.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_EXTERNAL_TOOL_RESOURCE_INSTITUTION_ID = """
            UPDATE lms.external_tool_resource
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.external_tool_role.role_id, on iam.role deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_EXTERNAL_TOOL_ROLE_ROLE_ID = """
            UPDATE lms.external_tool_role
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND role_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.forum_post.abuse_confirmed_by, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_FORUM_POST_ABUSE_CONFIRMED_BY = """
            UPDATE lms.forum_post
               SET abuse_confirmed_by = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND abuse_confirmed_by = :ownerId
            RETURNING id
            """;

    /**
     * lms.forum_post_vote.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_FORUM_POST_VOTE_USER_ID = """
            UPDATE lms.forum_post_vote
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.forum_read_state.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_FORUM_READ_STATE_USER_ID = """
            UPDATE lms.forum_read_state
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.forum_topic.individual_user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_FORUM_TOPIC_INDIVIDUAL_USER_ID = """
            UPDATE lms.forum_topic
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND individual_user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.grade_change_log.student_user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_GRADE_CHANGE_LOG_STUDENT_USER_ID = """
            UPDATE lms.grade_change_log
               SET student_user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND student_user_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.gradebook_entry.graded_by, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_GRADEBOOK_ENTRY_GRADED_BY = """
            UPDATE lms.gradebook_entry
               SET graded_by = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND graded_by = :ownerId
            RETURNING id
            """;

    /**
     * lms.import_export_job.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_IMPORT_EXPORT_JOB_INSTITUTION_ID = """
            UPDATE lms.import_export_job
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.import_export_job.package_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_IMPORT_EXPORT_JOB_PACKAGE_FILE_ID = """
            UPDATE lms.import_export_job
               SET package_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND package_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.live_class.calendar_event_id, on comms.event deletion: null the column.
     */
    public static final String CLEAR_LIVE_CLASS_CALENDAR_EVENT_ID = """
            UPDATE lms.live_class
               SET calendar_event_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND calendar_event_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.mentorship.mentee_user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_MENTORSHIP_MENTEE_USER_ID = """
            UPDATE lms.mentorship
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND mentee_user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.mentorship.mentor_user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_MENTORSHIP_MENTOR_USER_ID = """
            UPDATE lms.mentorship
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND mentor_user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.participation_activity.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_PARTICIPATION_ACTIVITY_INSTITUTION_ID = """
            UPDATE lms.participation_activity
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.plagiarism_account.default_teacher_user_id, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_PLAGIARISM_ACCOUNT_DEFAULT_TEACHER_USER_ID = """
            UPDATE lms.plagiarism_account
               SET default_teacher_user_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND default_teacher_user_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.plagiarism_report.entity_attachment_id, on platform.entity_attachment deletion: null the column.
     */
    public static final String CLEAR_PLAGIARISM_REPORT_ENTITY_ATTACHMENT_ID = """
            UPDATE lms.plagiarism_report
               SET entity_attachment_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND entity_attachment_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.plagiarism_user_link.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_PLAGIARISM_USER_LINK_USER_ID = """
            UPDATE lms.plagiarism_user_link
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.question_hotspot.image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_QUESTION_HOTSPOT_IMAGE_FILE_ID = """
            UPDATE lms.question_hotspot
               SET image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.quiz.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_QUIZ_INSTITUTION_ID = """
            UPDATE lms.quiz
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.quiz_answer.graded_by, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_QUIZ_ANSWER_GRADED_BY = """
            UPDATE lms.quiz_answer
               SET graded_by = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND graded_by = :ownerId
            RETURNING id
            """;

    /**
     * lms.related_material_rank.file_id, on platform.file_object deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_RELATED_MATERIAL_RANK_FILE_ID = """
            UPDATE lms.related_material_rank
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND file_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.related_material_rank.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_RELATED_MATERIAL_RANK_USER_ID = """
            UPDATE lms.related_material_rank
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.release_grant.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_RELEASE_GRANT_USER_ID = """
            UPDATE lms.release_grant
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.rubric.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_RUBRIC_INSTITUTION_ID = """
            UPDATE lms.rubric
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.settings.certificate_template_id, on comms.message_template deletion: null the column.
     */
    public static final String CLEAR_SETTINGS_CERTIFICATE_TEMPLATE_ID = """
            UPDATE lms.settings
               SET certificate_template_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND certificate_template_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.settings.cohort_confirmation_template_id, on comms.message_template deletion: null the column.
     */
    public static final String CLEAR_SETTINGS_COHORT_CONFIRMATION_TEMPLATE_ID = """
            UPDATE lms.settings
               SET cohort_confirmation_template_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND cohort_confirmation_template_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.settings.mentor_request_template_id, on comms.message_template deletion: null the column.
     */
    public static final String CLEAR_SETTINGS_MENTOR_REQUEST_TEMPLATE_ID = """
            UPDATE lms.settings
               SET mentor_request_template_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND mentor_request_template_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.settings.mentor_status_template_id, on comms.message_template deletion: null the column.
     */
    public static final String CLEAR_SETTINGS_MENTOR_STATUS_TEMPLATE_ID = """
            UPDATE lms.settings
               SET mentor_status_template_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND mentor_status_template_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.settings.meta_image_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_SETTINGS_META_IMAGE_FILE_ID = """
            UPDATE lms.settings
               SET meta_image_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND meta_image_file_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.settings.payment_reminder_template_id, on comms.message_template deletion: null the column.
     */
    public static final String CLEAR_SETTINGS_PAYMENT_REMINDER_TEMPLATE_ID = """
            UPDATE lms.settings
               SET payment_reminder_template_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND payment_reminder_template_id = :ownerId
            RETURNING id
            """;

    /**
     * lms.student_alert.sent_by, on iam.user_account deletion: null the column.
     */
    public static final String CLEAR_STUDENT_ALERT_SENT_BY = """
            UPDATE lms.student_alert
               SET sent_by = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND sent_by = :ownerId
            RETURNING id
            """;

    /**
     * lms.student_alert.user_id, on iam.user_account deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_STUDENT_ALERT_USER_ID = """
            UPDATE lms.student_alert
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * lms.web_resource.institution_id, on org.institution deletion: null the column.
     */
    public static final String CLEAR_WEB_RESOURCE_INSTITUTION_ID = """
            UPDATE lms.web_resource
               SET institution_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id = :ownerId
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
