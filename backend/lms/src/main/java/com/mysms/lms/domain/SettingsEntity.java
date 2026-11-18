package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.settings, the tenant's one LMS settings record. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SettingsEntity extends BaseEntity {

    public static final String TABLE = "lms.settings";

    private UUID tenantId;
    private String livecodeUrl;
    private UUID defaultCurrencyId;
    private Boolean applyGst;
    private Boolean showUsdEquivalent;
    private Boolean applyRounding;
    private String paymentGateway;
    private Boolean identifyUserCategory;
    private Boolean personaCaptured;
    private Boolean lmsIsDefaultHome;
    private Boolean sendCalendarInviteForEvaluations;
    private Boolean allowGuestAccess;
    private Boolean disableSignup;
    private Boolean disablePwa;
    private Boolean allowJobPosting;
    private Boolean certifiedMembersPage;
    private Boolean preventSkippingVideos;
    private Integer lessonDwellSeconds;
    private Boolean enforceVideoCompletion;
    private Boolean enforceQuizCompletion;
    private Boolean enforceQuizPassing;
    private Boolean enforceOpenEndedQuizPassing;
    private Boolean enforceAssignmentCompletion;
    private Boolean enforceAssignmentPassing;
    private Boolean enforceExerciseCompletion;
    private Boolean enforceExercisePassing;
    private Boolean showCourses;
    private Boolean showStudents;
    private Boolean showAssessments;
    private Boolean showLiveClass;
    private Boolean showDiscussions;
    private Boolean showEmails;
    private Boolean showDashboard;
    private Boolean showDayView;
    private String textDirection;
    private String notifyPublishedCourses;
    private String notifyPublishedCohorts;
    private Boolean sendPaymentRemindersCohort;
    private Boolean sendPaymentRemindersCourse;
    private UUID mentorRequestTemplateId;
    private UUID mentorStatusTemplateId;
    private UUID cohortConfirmationTemplateId;
    private UUID certificateTemplateId;
    private UUID paymentReminderTemplateId;
    private String customSignupContent;
    private String metaDescription;
    private String metaKeywords;
    private UUID metaImageFileId;
    private String contactUsEmail;
    private String contactUsUrl;
    private String unsplashAccessKeyEnc;
    private Boolean demoDataPresent;
}
