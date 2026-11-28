package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.SettingsUpdate;

/**
 * UpdateSettings over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.SettingsUpdate} message as REST's {@link
 * SettingsUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class SettingsProtoBridge {

    private SettingsProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SettingsUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.SettingsUpdate request) {
        SettingsUpdate changes = new SettingsUpdate();
        changes.setLivecodeUrl(ProtoBridge.text(request.hasLivecodeUrl(), request.getLivecodeUrl()));
        changes.setDefaultCurrencyId(bridge.uuid(
                request.hasDefaultCurrencyId(), request.getDefaultCurrencyId(), "default_currency_id"));
        changes.setApplyGst(ProtoBridge.flag(request.hasApplyGst(), request.getApplyGst()));
        changes.setShowUsdEquivalent(ProtoBridge.flag(request.hasShowUsdEquivalent(), request.getShowUsdEquivalent()));
        changes.setApplyRounding(ProtoBridge.flag(request.hasApplyRounding(), request.getApplyRounding()));
        changes.setPaymentGateway(ProtoBridge.text(request.hasPaymentGateway(), request.getPaymentGateway()));
        changes.setIdentifyUserCategory(ProtoBridge.flag(
                request.hasIdentifyUserCategory(), request.getIdentifyUserCategory()));
        changes.setPersonaCaptured(ProtoBridge.flag(request.hasPersonaCaptured(), request.getPersonaCaptured()));
        changes.setLmsIsDefaultHome(ProtoBridge.flag(request.hasLmsIsDefaultHome(), request.getLmsIsDefaultHome()));
        changes.setSendCalendarInviteForEvaluations(ProtoBridge.flag(
                request.hasSendCalendarInviteForEvaluations(), request.getSendCalendarInviteForEvaluations()));
        changes.setAllowGuestAccess(ProtoBridge.flag(request.hasAllowGuestAccess(), request.getAllowGuestAccess()));
        changes.setDisableSignup(ProtoBridge.flag(request.hasDisableSignup(), request.getDisableSignup()));
        changes.setDisablePwa(ProtoBridge.flag(request.hasDisablePwa(), request.getDisablePwa()));
        changes.setAllowJobPosting(ProtoBridge.flag(request.hasAllowJobPosting(), request.getAllowJobPosting()));
        changes.setCertifiedMembersPage(ProtoBridge.flag(
                request.hasCertifiedMembersPage(), request.getCertifiedMembersPage()));
        changes.setPreventSkippingVideos(ProtoBridge.flag(
                request.hasPreventSkippingVideos(), request.getPreventSkippingVideos()));
        changes.setLessonDwellSeconds(ProtoBridge.number(
                request.hasLessonDwellSeconds(), request.getLessonDwellSeconds()));
        changes.setEnforceVideoCompletion(ProtoBridge.flag(
                request.hasEnforceVideoCompletion(), request.getEnforceVideoCompletion()));
        changes.setEnforceQuizCompletion(ProtoBridge.flag(
                request.hasEnforceQuizCompletion(), request.getEnforceQuizCompletion()));
        changes.setEnforceQuizPassing(ProtoBridge.flag(
                request.hasEnforceQuizPassing(), request.getEnforceQuizPassing()));
        changes.setEnforceOpenEndedQuizPassing(ProtoBridge.flag(
                request.hasEnforceOpenEndedQuizPassing(), request.getEnforceOpenEndedQuizPassing()));
        changes.setEnforceAssignmentCompletion(ProtoBridge.flag(
                request.hasEnforceAssignmentCompletion(), request.getEnforceAssignmentCompletion()));
        changes.setEnforceAssignmentPassing(ProtoBridge.flag(
                request.hasEnforceAssignmentPassing(), request.getEnforceAssignmentPassing()));
        changes.setEnforceExerciseCompletion(ProtoBridge.flag(
                request.hasEnforceExerciseCompletion(), request.getEnforceExerciseCompletion()));
        changes.setEnforceExercisePassing(ProtoBridge.flag(
                request.hasEnforceExercisePassing(), request.getEnforceExercisePassing()));
        changes.setShowCourses(ProtoBridge.flag(request.hasShowCourses(), request.getShowCourses()));
        changes.setShowStudents(ProtoBridge.flag(request.hasShowStudents(), request.getShowStudents()));
        changes.setShowAssessments(ProtoBridge.flag(request.hasShowAssessments(), request.getShowAssessments()));
        changes.setShowLiveClass(ProtoBridge.flag(request.hasShowLiveClass(), request.getShowLiveClass()));
        changes.setShowDiscussions(ProtoBridge.flag(request.hasShowDiscussions(), request.getShowDiscussions()));
        changes.setShowEmails(ProtoBridge.flag(request.hasShowEmails(), request.getShowEmails()));
        changes.setShowDashboard(ProtoBridge.flag(request.hasShowDashboard(), request.getShowDashboard()));
        changes.setShowDayView(ProtoBridge.flag(request.hasShowDayView(), request.getShowDayView()));
        changes.setTextDirection(bridge.choice(
                request.hasTextDirection(), request.getTextDirection(), SettingsUpdate.TextDirectionEnum::fromValue));
        changes.setNotifyPublishedCourses(bridge.choice(
                request.hasNotifyPublishedCourses(), request.getNotifyPublishedCourses(),
                SettingsUpdate.NotifyPublishedCoursesEnum::fromValue));
        changes.setNotifyPublishedCohorts(bridge.choice(
                request.hasNotifyPublishedCohorts(), request.getNotifyPublishedCohorts(),
                SettingsUpdate.NotifyPublishedCohortsEnum::fromValue));
        changes.setSendPaymentRemindersCohort(ProtoBridge.flag(
                request.hasSendPaymentRemindersCohort(), request.getSendPaymentRemindersCohort()));
        changes.setSendPaymentRemindersCourse(ProtoBridge.flag(
                request.hasSendPaymentRemindersCourse(), request.getSendPaymentRemindersCourse()));
        changes.setMentorRequestTemplateId(bridge.uuid(
                request.hasMentorRequestTemplateId(), request.getMentorRequestTemplateId(),
                "mentor_request_template_id"));
        changes.setMentorStatusTemplateId(bridge.uuid(
                request.hasMentorStatusTemplateId(), request.getMentorStatusTemplateId(), "mentor_status_template_id"));
        changes.setCohortConfirmationTemplateId(bridge.uuid(
                request.hasCohortConfirmationTemplateId(), request.getCohortConfirmationTemplateId(),
                "cohort_confirmation_template_id"));
        changes.setCertificateTemplateId(bridge.uuid(
                request.hasCertificateTemplateId(), request.getCertificateTemplateId(), "certificate_template_id"));
        changes.setPaymentReminderTemplateId(bridge.uuid(
                request.hasPaymentReminderTemplateId(), request.getPaymentReminderTemplateId(),
                "payment_reminder_template_id"));
        changes.setCustomSignupContent(ProtoBridge.text(
                request.hasCustomSignupContent(), request.getCustomSignupContent()));
        changes.setMetaDescription(ProtoBridge.text(request.hasMetaDescription(), request.getMetaDescription()));
        changes.setMetaKeywords(ProtoBridge.text(request.hasMetaKeywords(), request.getMetaKeywords()));
        changes.setMetaImageFileId(bridge.uuid(
                request.hasMetaImageFileId(), request.getMetaImageFileId(), "meta_image_file_id"));
        changes.setContactUsEmail(ProtoBridge.text(request.hasContactUsEmail(), request.getContactUsEmail()));
        changes.setContactUsUrl(ProtoBridge.text(request.hasContactUsUrl(), request.getContactUsUrl()));
        changes.setUnsplashAccessKey(ProtoBridge.text(request.hasUnsplashAccessKey(), request.getUnsplashAccessKey()));
        changes.setDemoDataPresent(ProtoBridge.flag(request.hasDemoDataPresent(), request.getDemoDataPresent()));
        return bridge.patch(changes);
    }
}
