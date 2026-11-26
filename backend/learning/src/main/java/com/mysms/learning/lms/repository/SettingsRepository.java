package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.SettingsEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC access to lms.settings: the tenant's one live row, inserted once and updated under its row_version. */
@Repository
public class SettingsRepository {

    private static final String FIND = """
            SELECT r.id, r.tenant_id, r.livecode_url, r.default_currency_id, r.apply_gst,
                   r.show_usd_equivalent, r.apply_rounding, r.payment_gateway, r.identify_user_category,
                   r.persona_captured, r.lms_is_default_home, r.send_calendar_invite_for_evaluations,
                   r.allow_guest_access, r.disable_signup, r.disable_pwa, r.allow_job_posting,
                   r.certified_members_page, r.prevent_skipping_videos, r.lesson_dwell_seconds,
                   r.enforce_video_completion, r.enforce_quiz_completion, r.enforce_quiz_passing,
                   r.enforce_open_ended_quiz_passing, r.enforce_assignment_completion,
                   r.enforce_assignment_passing, r.enforce_exercise_completion, r.enforce_exercise_passing,
                   r.show_courses, r.show_students, r.show_assessments, r.show_live_class, r.show_discussions,
                   r.show_emails, r.show_dashboard, r.show_day_view, r.text_direction,
                   r.notify_published_courses, r.notify_published_cohorts, r.send_payment_reminders_cohort,
                   r.send_payment_reminders_course, r.mentor_request_template_id, r.mentor_status_template_id,
                   r.cohort_confirmation_template_id, r.certificate_template_id,
                   r.payment_reminder_template_id, r.custom_signup_content, r.meta_description,
                   r.meta_keywords, r.meta_image_file_id, r.contact_us_email, r.contact_us_url,
                   r.unsplash_access_key_enc, r.demo_data_present, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM lms.settings r
             WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL
            """;
    private static final String INSERT = """
            INSERT INTO lms.settings
            (id, tenant_id, livecode_url, default_currency_id, apply_gst, show_usd_equivalent, apply_rounding,
             payment_gateway, identify_user_category, persona_captured, lms_is_default_home,
             send_calendar_invite_for_evaluations, allow_guest_access, disable_signup, disable_pwa,
             allow_job_posting, certified_members_page, prevent_skipping_videos, lesson_dwell_seconds,
             enforce_video_completion, enforce_quiz_completion, enforce_quiz_passing,
             enforce_open_ended_quiz_passing, enforce_assignment_completion, enforce_assignment_passing,
             enforce_exercise_completion, enforce_exercise_passing, show_courses, show_students,
             show_assessments, show_live_class, show_discussions, show_emails, show_dashboard, show_day_view,
             text_direction, notify_published_courses, notify_published_cohorts,
             send_payment_reminders_cohort, send_payment_reminders_course, mentor_request_template_id,
             mentor_status_template_id, cohort_confirmation_template_id, certificate_template_id,
             payment_reminder_template_id, custom_signup_content, meta_description, meta_keywords,
             meta_image_file_id, contact_us_email, contact_us_url, unsplash_access_key_enc, demo_data_present,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :livecodeUrl, :defaultCurrencyId, COALESCE(:applyGst, false),
                    COALESCE(:showUsdEquivalent, false), COALESCE(:applyRounding, false), :paymentGateway,
                    COALESCE(:identifyUserCategory, false), COALESCE(:personaCaptured, false),
                    COALESCE(:lmsIsDefaultHome, false), COALESCE(:sendCalendarInviteForEvaluations, false),
                    COALESCE(:allowGuestAccess, true), COALESCE(:disableSignup, true),
                    COALESCE(:disablePwa, false), COALESCE(:allowJobPosting, true),
                    COALESCE(:certifiedMembersPage, false), COALESCE(:preventSkippingVideos, false),
                    COALESCE(:lessonDwellSeconds, 30), COALESCE(:enforceVideoCompletion, true),
                    COALESCE(:enforceQuizCompletion, true), COALESCE(:enforceQuizPassing, true),
                    COALESCE(:enforceOpenEndedQuizPassing, true),
                    COALESCE(:enforceAssignmentCompletion, true), COALESCE(:enforceAssignmentPassing, false),
                    COALESCE(:enforceExerciseCompletion, false), COALESCE(:enforceExercisePassing, false),
                    COALESCE(:showCourses, true), COALESCE(:showStudents, true),
                    COALESCE(:showAssessments, true), COALESCE(:showLiveClass, true),
                    COALESCE(:showDiscussions, true), COALESCE(:showEmails, true),
                    COALESCE(:showDashboard, true), COALESCE(:showDayView, false),
                    COALESCE(:textDirection, 'auto'::character varying), :notifyPublishedCourses,
                    :notifyPublishedCohorts, COALESCE(:sendPaymentRemindersCohort, false),
                    COALESCE(:sendPaymentRemindersCourse, false), :mentorRequestTemplateId,
                    :mentorStatusTemplateId, :cohortConfirmationTemplateId, :certificateTemplateId,
                    :paymentReminderTemplateId, :customSignupContent, :metaDescription, :metaKeywords,
                    :metaImageFileId, :contactUsEmail, :contactUsUrl, :unsplashAccessKeyEnc,
                    COALESCE(:demoDataPresent, false), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, livecode_url, default_currency_id, apply_gst, show_usd_equivalent,
                      apply_rounding, payment_gateway, identify_user_category, persona_captured,
                      lms_is_default_home, send_calendar_invite_for_evaluations, allow_guest_access,
                      disable_signup, disable_pwa, allow_job_posting, certified_members_page,
                      prevent_skipping_videos, lesson_dwell_seconds, enforce_video_completion,
                      enforce_quiz_completion, enforce_quiz_passing, enforce_open_ended_quiz_passing,
                      enforce_assignment_completion, enforce_assignment_passing, enforce_exercise_completion,
                      enforce_exercise_passing, show_courses, show_students, show_assessments,
                      show_live_class, show_discussions, show_emails, show_dashboard, show_day_view,
                      text_direction, notify_published_courses, notify_published_cohorts,
                      send_payment_reminders_cohort, send_payment_reminders_course,
                      mentor_request_template_id, mentor_status_template_id, cohort_confirmation_template_id,
                      certificate_template_id, payment_reminder_template_id, custom_signup_content,
                      meta_description, meta_keywords, meta_image_file_id, contact_us_email, contact_us_url,
                      unsplash_access_key_enc, demo_data_present, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE lms.settings
               SET livecode_url = :livecodeUrl,
                   default_currency_id = :defaultCurrencyId,
                   apply_gst = :applyGst,
                   show_usd_equivalent = :showUsdEquivalent,
                   apply_rounding = :applyRounding,
                   payment_gateway = :paymentGateway,
                   identify_user_category = :identifyUserCategory,
                   persona_captured = :personaCaptured,
                   lms_is_default_home = :lmsIsDefaultHome,
                   send_calendar_invite_for_evaluations = :sendCalendarInviteForEvaluations,
                   allow_guest_access = :allowGuestAccess,
                   disable_signup = :disableSignup,
                   disable_pwa = :disablePwa,
                   allow_job_posting = :allowJobPosting,
                   certified_members_page = :certifiedMembersPage,
                   prevent_skipping_videos = :preventSkippingVideos,
                   lesson_dwell_seconds = :lessonDwellSeconds,
                   enforce_video_completion = :enforceVideoCompletion,
                   enforce_quiz_completion = :enforceQuizCompletion,
                   enforce_quiz_passing = :enforceQuizPassing,
                   enforce_open_ended_quiz_passing = :enforceOpenEndedQuizPassing,
                   enforce_assignment_completion = :enforceAssignmentCompletion,
                   enforce_assignment_passing = :enforceAssignmentPassing,
                   enforce_exercise_completion = :enforceExerciseCompletion,
                   enforce_exercise_passing = :enforceExercisePassing,
                   show_courses = :showCourses,
                   show_students = :showStudents,
                   show_assessments = :showAssessments,
                   show_live_class = :showLiveClass,
                   show_discussions = :showDiscussions,
                   show_emails = :showEmails,
                   show_dashboard = :showDashboard,
                   show_day_view = :showDayView,
                   text_direction = :textDirection,
                   notify_published_courses = :notifyPublishedCourses,
                   notify_published_cohorts = :notifyPublishedCohorts,
                   send_payment_reminders_cohort = :sendPaymentRemindersCohort,
                   send_payment_reminders_course = :sendPaymentRemindersCourse,
                   mentor_request_template_id = :mentorRequestTemplateId,
                   mentor_status_template_id = :mentorStatusTemplateId,
                   cohort_confirmation_template_id = :cohortConfirmationTemplateId,
                   certificate_template_id = :certificateTemplateId,
                   payment_reminder_template_id = :paymentReminderTemplateId,
                   custom_signup_content = :customSignupContent,
                   meta_description = :metaDescription,
                   meta_keywords = :metaKeywords,
                   meta_image_file_id = :metaImageFileId,
                   contact_us_email = :contactUsEmail,
                   contact_us_url = :contactUsUrl,
                   unsplash_access_key_enc = :unsplashAccessKeyEnc,
                   demo_data_present = :demoDataPresent,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public SettingsRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The tenant's settings row, if it has one. */
    public Optional<SettingsEntity> find() {
        return jdbc.sql(FIND).query(SettingsRepository::map).optional();
    }

    /** Inserts the tenant's row (omitted values take the column defaults) and returns it as stored. */
    public SettingsEntity insert(SettingsEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("livecodeUrl", e.getLivecodeUrl())
                .param("defaultCurrencyId", e.getDefaultCurrencyId())
                .param("applyGst", e.getApplyGst())
                .param("showUsdEquivalent", e.getShowUsdEquivalent())
                .param("applyRounding", e.getApplyRounding())
                .param("paymentGateway", e.getPaymentGateway())
                .param("identifyUserCategory", e.getIdentifyUserCategory())
                .param("personaCaptured", e.getPersonaCaptured())
                .param("lmsIsDefaultHome", e.getLmsIsDefaultHome())
                .param("sendCalendarInviteForEvaluations", e.getSendCalendarInviteForEvaluations())
                .param("allowGuestAccess", e.getAllowGuestAccess())
                .param("disableSignup", e.getDisableSignup())
                .param("disablePwa", e.getDisablePwa())
                .param("allowJobPosting", e.getAllowJobPosting())
                .param("certifiedMembersPage", e.getCertifiedMembersPage())
                .param("preventSkippingVideos", e.getPreventSkippingVideos())
                .param("lessonDwellSeconds", e.getLessonDwellSeconds())
                .param("enforceVideoCompletion", e.getEnforceVideoCompletion())
                .param("enforceQuizCompletion", e.getEnforceQuizCompletion())
                .param("enforceQuizPassing", e.getEnforceQuizPassing())
                .param("enforceOpenEndedQuizPassing", e.getEnforceOpenEndedQuizPassing())
                .param("enforceAssignmentCompletion", e.getEnforceAssignmentCompletion())
                .param("enforceAssignmentPassing", e.getEnforceAssignmentPassing())
                .param("enforceExerciseCompletion", e.getEnforceExerciseCompletion())
                .param("enforceExercisePassing", e.getEnforceExercisePassing())
                .param("showCourses", e.getShowCourses())
                .param("showStudents", e.getShowStudents())
                .param("showAssessments", e.getShowAssessments())
                .param("showLiveClass", e.getShowLiveClass())
                .param("showDiscussions", e.getShowDiscussions())
                .param("showEmails", e.getShowEmails())
                .param("showDashboard", e.getShowDashboard())
                .param("showDayView", e.getShowDayView())
                .param("textDirection", e.getTextDirection())
                .param("notifyPublishedCourses", e.getNotifyPublishedCourses())
                .param("notifyPublishedCohorts", e.getNotifyPublishedCohorts())
                .param("sendPaymentRemindersCohort", e.getSendPaymentRemindersCohort())
                .param("sendPaymentRemindersCourse", e.getSendPaymentRemindersCourse())
                .param("mentorRequestTemplateId", e.getMentorRequestTemplateId())
                .param("mentorStatusTemplateId", e.getMentorStatusTemplateId())
                .param("cohortConfirmationTemplateId", e.getCohortConfirmationTemplateId())
                .param("certificateTemplateId", e.getCertificateTemplateId())
                .param("paymentReminderTemplateId", e.getPaymentReminderTemplateId())
                .param("customSignupContent", e.getCustomSignupContent())
                .param("metaDescription", e.getMetaDescription())
                .param("metaKeywords", e.getMetaKeywords())
                .param("metaImageFileId", e.getMetaImageFileId())
                .param("contactUsEmail", e.getContactUsEmail())
                .param("contactUsUrl", e.getContactUsUrl())
                .param("unsplashAccessKeyEnc", e.getUnsplashAccessKeyEnc())
                .param("demoDataPresent", e.getDemoDataPresent())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SettingsRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed since it was read. */
    public int update(SettingsEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("livecodeUrl", e.getLivecodeUrl())
                .param("defaultCurrencyId", e.getDefaultCurrencyId())
                .param("applyGst", e.getApplyGst())
                .param("showUsdEquivalent", e.getShowUsdEquivalent())
                .param("applyRounding", e.getApplyRounding())
                .param("paymentGateway", e.getPaymentGateway())
                .param("identifyUserCategory", e.getIdentifyUserCategory())
                .param("personaCaptured", e.getPersonaCaptured())
                .param("lmsIsDefaultHome", e.getLmsIsDefaultHome())
                .param("sendCalendarInviteForEvaluations", e.getSendCalendarInviteForEvaluations())
                .param("allowGuestAccess", e.getAllowGuestAccess())
                .param("disableSignup", e.getDisableSignup())
                .param("disablePwa", e.getDisablePwa())
                .param("allowJobPosting", e.getAllowJobPosting())
                .param("certifiedMembersPage", e.getCertifiedMembersPage())
                .param("preventSkippingVideos", e.getPreventSkippingVideos())
                .param("lessonDwellSeconds", e.getLessonDwellSeconds())
                .param("enforceVideoCompletion", e.getEnforceVideoCompletion())
                .param("enforceQuizCompletion", e.getEnforceQuizCompletion())
                .param("enforceQuizPassing", e.getEnforceQuizPassing())
                .param("enforceOpenEndedQuizPassing", e.getEnforceOpenEndedQuizPassing())
                .param("enforceAssignmentCompletion", e.getEnforceAssignmentCompletion())
                .param("enforceAssignmentPassing", e.getEnforceAssignmentPassing())
                .param("enforceExerciseCompletion", e.getEnforceExerciseCompletion())
                .param("enforceExercisePassing", e.getEnforceExercisePassing())
                .param("showCourses", e.getShowCourses())
                .param("showStudents", e.getShowStudents())
                .param("showAssessments", e.getShowAssessments())
                .param("showLiveClass", e.getShowLiveClass())
                .param("showDiscussions", e.getShowDiscussions())
                .param("showEmails", e.getShowEmails())
                .param("showDashboard", e.getShowDashboard())
                .param("showDayView", e.getShowDayView())
                .param("textDirection", e.getTextDirection())
                .param("notifyPublishedCourses", e.getNotifyPublishedCourses())
                .param("notifyPublishedCohorts", e.getNotifyPublishedCohorts())
                .param("sendPaymentRemindersCohort", e.getSendPaymentRemindersCohort())
                .param("sendPaymentRemindersCourse", e.getSendPaymentRemindersCourse())
                .param("mentorRequestTemplateId", e.getMentorRequestTemplateId())
                .param("mentorStatusTemplateId", e.getMentorStatusTemplateId())
                .param("cohortConfirmationTemplateId", e.getCohortConfirmationTemplateId())
                .param("certificateTemplateId", e.getCertificateTemplateId())
                .param("paymentReminderTemplateId", e.getPaymentReminderTemplateId())
                .param("customSignupContent", e.getCustomSignupContent())
                .param("metaDescription", e.getMetaDescription())
                .param("metaKeywords", e.getMetaKeywords())
                .param("metaImageFileId", e.getMetaImageFileId())
                .param("contactUsEmail", e.getContactUsEmail())
                .param("contactUsUrl", e.getContactUsUrl())
                .param("unsplashAccessKeyEnc", e.getUnsplashAccessKeyEnc())
                .param("demoDataPresent", e.getDemoDataPresent())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static SettingsEntity map(ResultSet rs, int row) throws SQLException {
        return SettingsEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .livecodeUrl(rs.getString("livecode_url"))
                .defaultCurrencyId(rs.getObject("default_currency_id", UUID.class))
                .applyGst(rs.getObject("apply_gst", Boolean.class))
                .showUsdEquivalent(rs.getObject("show_usd_equivalent", Boolean.class))
                .applyRounding(rs.getObject("apply_rounding", Boolean.class))
                .paymentGateway(rs.getString("payment_gateway"))
                .identifyUserCategory(rs.getObject("identify_user_category", Boolean.class))
                .personaCaptured(rs.getObject("persona_captured", Boolean.class))
                .lmsIsDefaultHome(rs.getObject("lms_is_default_home", Boolean.class))
                .sendCalendarInviteForEvaluations(rs.getObject("send_calendar_invite_for_evaluations", Boolean.class))
                .allowGuestAccess(rs.getObject("allow_guest_access", Boolean.class))
                .disableSignup(rs.getObject("disable_signup", Boolean.class))
                .disablePwa(rs.getObject("disable_pwa", Boolean.class))
                .allowJobPosting(rs.getObject("allow_job_posting", Boolean.class))
                .certifiedMembersPage(rs.getObject("certified_members_page", Boolean.class))
                .preventSkippingVideos(rs.getObject("prevent_skipping_videos", Boolean.class))
                .lessonDwellSeconds(rs.getObject("lesson_dwell_seconds", Integer.class))
                .enforceVideoCompletion(rs.getObject("enforce_video_completion", Boolean.class))
                .enforceQuizCompletion(rs.getObject("enforce_quiz_completion", Boolean.class))
                .enforceQuizPassing(rs.getObject("enforce_quiz_passing", Boolean.class))
                .enforceOpenEndedQuizPassing(rs.getObject("enforce_open_ended_quiz_passing", Boolean.class))
                .enforceAssignmentCompletion(rs.getObject("enforce_assignment_completion", Boolean.class))
                .enforceAssignmentPassing(rs.getObject("enforce_assignment_passing", Boolean.class))
                .enforceExerciseCompletion(rs.getObject("enforce_exercise_completion", Boolean.class))
                .enforceExercisePassing(rs.getObject("enforce_exercise_passing", Boolean.class))
                .showCourses(rs.getObject("show_courses", Boolean.class))
                .showStudents(rs.getObject("show_students", Boolean.class))
                .showAssessments(rs.getObject("show_assessments", Boolean.class))
                .showLiveClass(rs.getObject("show_live_class", Boolean.class))
                .showDiscussions(rs.getObject("show_discussions", Boolean.class))
                .showEmails(rs.getObject("show_emails", Boolean.class))
                .showDashboard(rs.getObject("show_dashboard", Boolean.class))
                .showDayView(rs.getObject("show_day_view", Boolean.class))
                .textDirection(rs.getString("text_direction"))
                .notifyPublishedCourses(rs.getString("notify_published_courses"))
                .notifyPublishedCohorts(rs.getString("notify_published_cohorts"))
                .sendPaymentRemindersCohort(rs.getObject("send_payment_reminders_cohort", Boolean.class))
                .sendPaymentRemindersCourse(rs.getObject("send_payment_reminders_course", Boolean.class))
                .mentorRequestTemplateId(rs.getObject("mentor_request_template_id", UUID.class))
                .mentorStatusTemplateId(rs.getObject("mentor_status_template_id", UUID.class))
                .cohortConfirmationTemplateId(rs.getObject("cohort_confirmation_template_id", UUID.class))
                .certificateTemplateId(rs.getObject("certificate_template_id", UUID.class))
                .paymentReminderTemplateId(rs.getObject("payment_reminder_template_id", UUID.class))
                .customSignupContent(rs.getString("custom_signup_content"))
                .metaDescription(rs.getString("meta_description"))
                .metaKeywords(rs.getString("meta_keywords"))
                .metaImageFileId(rs.getObject("meta_image_file_id", UUID.class))
                .contactUsEmail(rs.getString("contact_us_email"))
                .contactUsUrl(rs.getString("contact_us_url"))
                .unsplashAccessKeyEnc(rs.getString("unsplash_access_key_enc"))
                .demoDataPresent(rs.getObject("demo_data_present", Boolean.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
