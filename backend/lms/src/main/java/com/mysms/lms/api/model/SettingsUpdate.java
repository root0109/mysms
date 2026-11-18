// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Settings that can be updated (partial update; see updateMask).
 */

public class SettingsUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String livecodeUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultCurrencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean applyGst;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showUsdEquivalent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean applyRounding;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String paymentGateway;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean identifyUserCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean personaCaptured;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean lmsIsDefaultHome;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean sendCalendarInviteForEvaluations;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowGuestAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean disableSignup;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean disablePwa;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowJobPosting;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean certifiedMembersPage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean preventSkippingVideos;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer lessonDwellSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceVideoCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceQuizCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceQuizPassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceOpenEndedQuizPassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceAssignmentCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceAssignmentPassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceExerciseCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceExercisePassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showCourses;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showStudents;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showAssessments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showLiveClass;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showDiscussions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showEmails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showDashboard;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showDayView;

  /**
   * Text direction.
   */
  public enum TextDirectionEnum {
    AUTO("auto"),
    
    LTR("ltr"),
    
    RTL("rtl");

    private final String value;

    TextDirectionEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TextDirectionEnum fromValue(String value) {
      for (TextDirectionEnum b : TextDirectionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TextDirectionEnum textDirection;

  /**
   * Channel for new-course announcements.
   */
  public enum NotifyPublishedCoursesEnum {
    EMAIL("email"),
    
    IN_APP("in_app");

    private final String value;

    NotifyPublishedCoursesEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static NotifyPublishedCoursesEnum fromValue(String value) {
      for (NotifyPublishedCoursesEnum b : NotifyPublishedCoursesEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable NotifyPublishedCoursesEnum notifyPublishedCourses;

  /**
   * Channel for new-cohort announcements.
   */
  public enum NotifyPublishedCohortsEnum {
    EMAIL("email"),
    
    IN_APP("in_app");

    private final String value;

    NotifyPublishedCohortsEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static NotifyPublishedCohortsEnum fromValue(String value) {
      for (NotifyPublishedCohortsEnum b : NotifyPublishedCohortsEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable NotifyPublishedCohortsEnum notifyPublishedCohorts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean sendPaymentRemindersCohort;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean sendPaymentRemindersCourse;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mentorRequestTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mentorStatusTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortConfirmationTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID certificateTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID paymentReminderTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String customSignupContent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String metaDescription;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String metaKeywords;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID metaImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contactUsEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contactUsUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String unsplashAccessKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean demoDataPresent;

  public SettingsUpdate livecodeUrl(@Nullable String livecodeUrl) {
    this.livecodeUrl = livecodeUrl;
    return this;
  }

  /**
   * Code-execution service URL.
   * @return livecodeUrl
   */
  @Size(max = 500) 
  @JsonProperty("livecodeUrl")
  public @Nullable String getLivecodeUrl() {
    return livecodeUrl;
  }

  @JsonProperty("livecodeUrl")
  public void setLivecodeUrl(@Nullable String livecodeUrl) {
    this.livecodeUrl = livecodeUrl;
  }

  public SettingsUpdate defaultCurrencyId(@Nullable UUID defaultCurrencyId) {
    this.defaultCurrencyId = defaultCurrencyId;
    return this;
  }

  /**
   * Default currency for prices.
   * @return defaultCurrencyId
   */
  @Valid 
  @JsonProperty("defaultCurrencyId")
  public @Nullable UUID getDefaultCurrencyId() {
    return defaultCurrencyId;
  }

  @JsonProperty("defaultCurrencyId")
  public void setDefaultCurrencyId(@Nullable UUID defaultCurrencyId) {
    this.defaultCurrencyId = defaultCurrencyId;
  }

  public SettingsUpdate applyGst(@Nullable Boolean applyGst) {
    this.applyGst = applyGst;
    return this;
  }

  /**
   * Apply Indian GST on payments.
   * @return applyGst
   */
  
  @JsonProperty("applyGst")
  public @Nullable Boolean getApplyGst() {
    return applyGst;
  }

  @JsonProperty("applyGst")
  public void setApplyGst(@Nullable Boolean applyGst) {
    this.applyGst = applyGst;
  }

  public SettingsUpdate showUsdEquivalent(@Nullable Boolean showUsdEquivalent) {
    this.showUsdEquivalent = showUsdEquivalent;
    return this;
  }

  /**
   * Show USD prices to international visitors.
   * @return showUsdEquivalent
   */
  
  @JsonProperty("showUsdEquivalent")
  public @Nullable Boolean getShowUsdEquivalent() {
    return showUsdEquivalent;
  }

  @JsonProperty("showUsdEquivalent")
  public void setShowUsdEquivalent(@Nullable Boolean showUsdEquivalent) {
    this.showUsdEquivalent = showUsdEquivalent;
  }

  public SettingsUpdate applyRounding(@Nullable Boolean applyRounding) {
    this.applyRounding = applyRounding;
    return this;
  }

  /**
   * Round converted prices.
   * @return applyRounding
   */
  
  @JsonProperty("applyRounding")
  public @Nullable Boolean getApplyRounding() {
    return applyRounding;
  }

  @JsonProperty("applyRounding")
  public void setApplyRounding(@Nullable Boolean applyRounding) {
    this.applyRounding = applyRounding;
  }

  public SettingsUpdate paymentGateway(@Nullable String paymentGateway) {
    this.paymentGateway = paymentGateway;
    return this;
  }

  /**
   * Payment gateway used.
   * @return paymentGateway
   */
  @Size(max = 60) 
  @JsonProperty("paymentGateway")
  public @Nullable String getPaymentGateway() {
    return paymentGateway;
  }

  @JsonProperty("paymentGateway")
  public void setPaymentGateway(@Nullable String paymentGateway) {
    this.paymentGateway = paymentGateway;
  }

  public SettingsUpdate identifyUserCategory(@Nullable Boolean identifyUserCategory) {
    this.identifyUserCategory = identifyUserCategory;
    return this;
  }

  /**
   * Ask students for their persona at signup.
   * @return identifyUserCategory
   */
  
  @JsonProperty("identifyUserCategory")
  public @Nullable Boolean getIdentifyUserCategory() {
    return identifyUserCategory;
  }

  @JsonProperty("identifyUserCategory")
  public void setIdentifyUserCategory(@Nullable Boolean identifyUserCategory) {
    this.identifyUserCategory = identifyUserCategory;
  }

  public SettingsUpdate personaCaptured(@Nullable Boolean personaCaptured) {
    this.personaCaptured = personaCaptured;
    return this;
  }

  /**
   * Persona survey completed.
   * @return personaCaptured
   */
  
  @JsonProperty("personaCaptured")
  public @Nullable Boolean getPersonaCaptured() {
    return personaCaptured;
  }

  @JsonProperty("personaCaptured")
  public void setPersonaCaptured(@Nullable Boolean personaCaptured) {
    this.personaCaptured = personaCaptured;
  }

  public SettingsUpdate lmsIsDefaultHome(@Nullable Boolean lmsIsDefaultHome) {
    this.lmsIsDefaultHome = lmsIsDefaultHome;
    return this;
  }

  /**
   * LMS is the default landing page.
   * @return lmsIsDefaultHome
   */
  
  @JsonProperty("lmsIsDefaultHome")
  public @Nullable Boolean getLmsIsDefaultHome() {
    return lmsIsDefaultHome;
  }

  @JsonProperty("lmsIsDefaultHome")
  public void setLmsIsDefaultHome(@Nullable Boolean lmsIsDefaultHome) {
    this.lmsIsDefaultHome = lmsIsDefaultHome;
  }

  public SettingsUpdate sendCalendarInviteForEvaluations(@Nullable Boolean sendCalendarInviteForEvaluations) {
    this.sendCalendarInviteForEvaluations = sendCalendarInviteForEvaluations;
    return this;
  }

  /**
   * Send calendar invites for evaluations.
   * @return sendCalendarInviteForEvaluations
   */
  
  @JsonProperty("sendCalendarInviteForEvaluations")
  public @Nullable Boolean getSendCalendarInviteForEvaluations() {
    return sendCalendarInviteForEvaluations;
  }

  @JsonProperty("sendCalendarInviteForEvaluations")
  public void setSendCalendarInviteForEvaluations(@Nullable Boolean sendCalendarInviteForEvaluations) {
    this.sendCalendarInviteForEvaluations = sendCalendarInviteForEvaluations;
  }

  public SettingsUpdate allowGuestAccess(@Nullable Boolean allowGuestAccess) {
    this.allowGuestAccess = allowGuestAccess;
    return this;
  }

  /**
   * Visitors can browse without login.
   * @return allowGuestAccess
   */
  
  @JsonProperty("allowGuestAccess")
  public @Nullable Boolean getAllowGuestAccess() {
    return allowGuestAccess;
  }

  @JsonProperty("allowGuestAccess")
  public void setAllowGuestAccess(@Nullable Boolean allowGuestAccess) {
    this.allowGuestAccess = allowGuestAccess;
  }

  public SettingsUpdate disableSignup(@Nullable Boolean disableSignup) {
    this.disableSignup = disableSignup;
    return this;
  }

  /**
   * Self-signup disabled.
   * @return disableSignup
   */
  
  @JsonProperty("disableSignup")
  public @Nullable Boolean getDisableSignup() {
    return disableSignup;
  }

  @JsonProperty("disableSignup")
  public void setDisableSignup(@Nullable Boolean disableSignup) {
    this.disableSignup = disableSignup;
  }

  public SettingsUpdate disablePwa(@Nullable Boolean disablePwa) {
    this.disablePwa = disablePwa;
    return this;
  }

  /**
   * Disable installable web app.
   * @return disablePwa
   */
  
  @JsonProperty("disablePwa")
  public @Nullable Boolean getDisablePwa() {
    return disablePwa;
  }

  @JsonProperty("disablePwa")
  public void setDisablePwa(@Nullable Boolean disablePwa) {
    this.disablePwa = disablePwa;
  }

  public SettingsUpdate allowJobPosting(@Nullable Boolean allowJobPosting) {
    this.allowJobPosting = allowJobPosting;
    return this;
  }

  /**
   * Users may post jobs.
   * @return allowJobPosting
   */
  
  @JsonProperty("allowJobPosting")
  public @Nullable Boolean getAllowJobPosting() {
    return allowJobPosting;
  }

  @JsonProperty("allowJobPosting")
  public void setAllowJobPosting(@Nullable Boolean allowJobPosting) {
    this.allowJobPosting = allowJobPosting;
  }

  public SettingsUpdate certifiedMembersPage(@Nullable Boolean certifiedMembersPage) {
    this.certifiedMembersPage = certifiedMembersPage;
    return this;
  }

  /**
   * Show the certified-members directory.
   * @return certifiedMembersPage
   */
  
  @JsonProperty("certifiedMembersPage")
  public @Nullable Boolean getCertifiedMembersPage() {
    return certifiedMembersPage;
  }

  @JsonProperty("certifiedMembersPage")
  public void setCertifiedMembersPage(@Nullable Boolean certifiedMembersPage) {
    this.certifiedMembersPage = certifiedMembersPage;
  }

  public SettingsUpdate preventSkippingVideos(@Nullable Boolean preventSkippingVideos) {
    this.preventSkippingVideos = preventSkippingVideos;
    return this;
  }

  /**
   * Videos cannot be skipped.
   * @return preventSkippingVideos
   */
  
  @JsonProperty("preventSkippingVideos")
  public @Nullable Boolean getPreventSkippingVideos() {
    return preventSkippingVideos;
  }

  @JsonProperty("preventSkippingVideos")
  public void setPreventSkippingVideos(@Nullable Boolean preventSkippingVideos) {
    this.preventSkippingVideos = preventSkippingVideos;
  }

  public SettingsUpdate lessonDwellSeconds(@Nullable Integer lessonDwellSeconds) {
    this.lessonDwellSeconds = lessonDwellSeconds;
    return this;
  }

  /**
   * Seconds on a lesson before it counts as viewed.
   * @return lessonDwellSeconds
   */
  
  @JsonProperty("lessonDwellSeconds")
  public @Nullable Integer getLessonDwellSeconds() {
    return lessonDwellSeconds;
  }

  @JsonProperty("lessonDwellSeconds")
  public void setLessonDwellSeconds(@Nullable Integer lessonDwellSeconds) {
    this.lessonDwellSeconds = lessonDwellSeconds;
  }

  public SettingsUpdate enforceVideoCompletion(@Nullable Boolean enforceVideoCompletion) {
    this.enforceVideoCompletion = enforceVideoCompletion;
    return this;
  }

  /**
   * Video must be watched to complete a lesson.
   * @return enforceVideoCompletion
   */
  
  @JsonProperty("enforceVideoCompletion")
  public @Nullable Boolean getEnforceVideoCompletion() {
    return enforceVideoCompletion;
  }

  @JsonProperty("enforceVideoCompletion")
  public void setEnforceVideoCompletion(@Nullable Boolean enforceVideoCompletion) {
    this.enforceVideoCompletion = enforceVideoCompletion;
  }

  public SettingsUpdate enforceQuizCompletion(@Nullable Boolean enforceQuizCompletion) {
    this.enforceQuizCompletion = enforceQuizCompletion;
    return this;
  }

  /**
   * Quiz must be attempted.
   * @return enforceQuizCompletion
   */
  
  @JsonProperty("enforceQuizCompletion")
  public @Nullable Boolean getEnforceQuizCompletion() {
    return enforceQuizCompletion;
  }

  @JsonProperty("enforceQuizCompletion")
  public void setEnforceQuizCompletion(@Nullable Boolean enforceQuizCompletion) {
    this.enforceQuizCompletion = enforceQuizCompletion;
  }

  public SettingsUpdate enforceQuizPassing(@Nullable Boolean enforceQuizPassing) {
    this.enforceQuizPassing = enforceQuizPassing;
    return this;
  }

  /**
   * Quiz must be passed.
   * @return enforceQuizPassing
   */
  
  @JsonProperty("enforceQuizPassing")
  public @Nullable Boolean getEnforceQuizPassing() {
    return enforceQuizPassing;
  }

  @JsonProperty("enforceQuizPassing")
  public void setEnforceQuizPassing(@Nullable Boolean enforceQuizPassing) {
    this.enforceQuizPassing = enforceQuizPassing;
  }

  public SettingsUpdate enforceOpenEndedQuizPassing(@Nullable Boolean enforceOpenEndedQuizPassing) {
    this.enforceOpenEndedQuizPassing = enforceOpenEndedQuizPassing;
    return this;
  }

  /**
   * Open-ended quiz must be passed.
   * @return enforceOpenEndedQuizPassing
   */
  
  @JsonProperty("enforceOpenEndedQuizPassing")
  public @Nullable Boolean getEnforceOpenEndedQuizPassing() {
    return enforceOpenEndedQuizPassing;
  }

  @JsonProperty("enforceOpenEndedQuizPassing")
  public void setEnforceOpenEndedQuizPassing(@Nullable Boolean enforceOpenEndedQuizPassing) {
    this.enforceOpenEndedQuizPassing = enforceOpenEndedQuizPassing;
  }

  public SettingsUpdate enforceAssignmentCompletion(@Nullable Boolean enforceAssignmentCompletion) {
    this.enforceAssignmentCompletion = enforceAssignmentCompletion;
    return this;
  }

  /**
   * Assignment must be submitted.
   * @return enforceAssignmentCompletion
   */
  
  @JsonProperty("enforceAssignmentCompletion")
  public @Nullable Boolean getEnforceAssignmentCompletion() {
    return enforceAssignmentCompletion;
  }

  @JsonProperty("enforceAssignmentCompletion")
  public void setEnforceAssignmentCompletion(@Nullable Boolean enforceAssignmentCompletion) {
    this.enforceAssignmentCompletion = enforceAssignmentCompletion;
  }

  public SettingsUpdate enforceAssignmentPassing(@Nullable Boolean enforceAssignmentPassing) {
    this.enforceAssignmentPassing = enforceAssignmentPassing;
    return this;
  }

  /**
   * Assignment must be passed.
   * @return enforceAssignmentPassing
   */
  
  @JsonProperty("enforceAssignmentPassing")
  public @Nullable Boolean getEnforceAssignmentPassing() {
    return enforceAssignmentPassing;
  }

  @JsonProperty("enforceAssignmentPassing")
  public void setEnforceAssignmentPassing(@Nullable Boolean enforceAssignmentPassing) {
    this.enforceAssignmentPassing = enforceAssignmentPassing;
  }

  public SettingsUpdate enforceExerciseCompletion(@Nullable Boolean enforceExerciseCompletion) {
    this.enforceExerciseCompletion = enforceExerciseCompletion;
    return this;
  }

  /**
   * Exercise must be submitted.
   * @return enforceExerciseCompletion
   */
  
  @JsonProperty("enforceExerciseCompletion")
  public @Nullable Boolean getEnforceExerciseCompletion() {
    return enforceExerciseCompletion;
  }

  @JsonProperty("enforceExerciseCompletion")
  public void setEnforceExerciseCompletion(@Nullable Boolean enforceExerciseCompletion) {
    this.enforceExerciseCompletion = enforceExerciseCompletion;
  }

  public SettingsUpdate enforceExercisePassing(@Nullable Boolean enforceExercisePassing) {
    this.enforceExercisePassing = enforceExercisePassing;
    return this;
  }

  /**
   * Exercise must be passed.
   * @return enforceExercisePassing
   */
  
  @JsonProperty("enforceExercisePassing")
  public @Nullable Boolean getEnforceExercisePassing() {
    return enforceExercisePassing;
  }

  @JsonProperty("enforceExercisePassing")
  public void setEnforceExercisePassing(@Nullable Boolean enforceExercisePassing) {
    this.enforceExercisePassing = enforceExercisePassing;
  }

  public SettingsUpdate showCourses(@Nullable Boolean showCourses) {
    this.showCourses = showCourses;
    return this;
  }

  /**
   * Show the courses section.
   * @return showCourses
   */
  
  @JsonProperty("showCourses")
  public @Nullable Boolean getShowCourses() {
    return showCourses;
  }

  @JsonProperty("showCourses")
  public void setShowCourses(@Nullable Boolean showCourses) {
    this.showCourses = showCourses;
  }

  public SettingsUpdate showStudents(@Nullable Boolean showStudents) {
    this.showStudents = showStudents;
    return this;
  }

  /**
   * Show the students section.
   * @return showStudents
   */
  
  @JsonProperty("showStudents")
  public @Nullable Boolean getShowStudents() {
    return showStudents;
  }

  @JsonProperty("showStudents")
  public void setShowStudents(@Nullable Boolean showStudents) {
    this.showStudents = showStudents;
  }

  public SettingsUpdate showAssessments(@Nullable Boolean showAssessments) {
    this.showAssessments = showAssessments;
    return this;
  }

  /**
   * Show the assessments section.
   * @return showAssessments
   */
  
  @JsonProperty("showAssessments")
  public @Nullable Boolean getShowAssessments() {
    return showAssessments;
  }

  @JsonProperty("showAssessments")
  public void setShowAssessments(@Nullable Boolean showAssessments) {
    this.showAssessments = showAssessments;
  }

  public SettingsUpdate showLiveClass(@Nullable Boolean showLiveClass) {
    this.showLiveClass = showLiveClass;
    return this;
  }

  /**
   * Show the live-class section.
   * @return showLiveClass
   */
  
  @JsonProperty("showLiveClass")
  public @Nullable Boolean getShowLiveClass() {
    return showLiveClass;
  }

  @JsonProperty("showLiveClass")
  public void setShowLiveClass(@Nullable Boolean showLiveClass) {
    this.showLiveClass = showLiveClass;
  }

  public SettingsUpdate showDiscussions(@Nullable Boolean showDiscussions) {
    this.showDiscussions = showDiscussions;
    return this;
  }

  /**
   * Show discussions.
   * @return showDiscussions
   */
  
  @JsonProperty("showDiscussions")
  public @Nullable Boolean getShowDiscussions() {
    return showDiscussions;
  }

  @JsonProperty("showDiscussions")
  public void setShowDiscussions(@Nullable Boolean showDiscussions) {
    this.showDiscussions = showDiscussions;
  }

  public SettingsUpdate showEmails(@Nullable Boolean showEmails) {
    this.showEmails = showEmails;
    return this;
  }

  /**
   * Show the e-mails section.
   * @return showEmails
   */
  
  @JsonProperty("showEmails")
  public @Nullable Boolean getShowEmails() {
    return showEmails;
  }

  @JsonProperty("showEmails")
  public void setShowEmails(@Nullable Boolean showEmails) {
    this.showEmails = showEmails;
  }

  public SettingsUpdate showDashboard(@Nullable Boolean showDashboard) {
    this.showDashboard = showDashboard;
    return this;
  }

  /**
   * Show the dashboard.
   * @return showDashboard
   */
  
  @JsonProperty("showDashboard")
  public @Nullable Boolean getShowDashboard() {
    return showDashboard;
  }

  @JsonProperty("showDashboard")
  public void setShowDashboard(@Nullable Boolean showDashboard) {
    this.showDashboard = showDashboard;
  }

  public SettingsUpdate showDayView(@Nullable Boolean showDayView) {
    this.showDayView = showDayView;
    return this;
  }

  /**
   * Day view in timetables.
   * @return showDayView
   */
  
  @JsonProperty("showDayView")
  public @Nullable Boolean getShowDayView() {
    return showDayView;
  }

  @JsonProperty("showDayView")
  public void setShowDayView(@Nullable Boolean showDayView) {
    this.showDayView = showDayView;
  }

  public SettingsUpdate textDirection(@Nullable TextDirectionEnum textDirection) {
    this.textDirection = textDirection;
    return this;
  }

  /**
   * Text direction.
   * @return textDirection
   */
  
  @JsonProperty("textDirection")
  public @Nullable TextDirectionEnum getTextDirection() {
    return textDirection;
  }

  @JsonProperty("textDirection")
  public void setTextDirection(@Nullable TextDirectionEnum textDirection) {
    this.textDirection = textDirection;
  }

  public SettingsUpdate notifyPublishedCourses(@Nullable NotifyPublishedCoursesEnum notifyPublishedCourses) {
    this.notifyPublishedCourses = notifyPublishedCourses;
    return this;
  }

  /**
   * Channel for new-course announcements.
   * @return notifyPublishedCourses
   */
  
  @JsonProperty("notifyPublishedCourses")
  public @Nullable NotifyPublishedCoursesEnum getNotifyPublishedCourses() {
    return notifyPublishedCourses;
  }

  @JsonProperty("notifyPublishedCourses")
  public void setNotifyPublishedCourses(@Nullable NotifyPublishedCoursesEnum notifyPublishedCourses) {
    this.notifyPublishedCourses = notifyPublishedCourses;
  }

  public SettingsUpdate notifyPublishedCohorts(@Nullable NotifyPublishedCohortsEnum notifyPublishedCohorts) {
    this.notifyPublishedCohorts = notifyPublishedCohorts;
    return this;
  }

  /**
   * Channel for new-cohort announcements.
   * @return notifyPublishedCohorts
   */
  
  @JsonProperty("notifyPublishedCohorts")
  public @Nullable NotifyPublishedCohortsEnum getNotifyPublishedCohorts() {
    return notifyPublishedCohorts;
  }

  @JsonProperty("notifyPublishedCohorts")
  public void setNotifyPublishedCohorts(@Nullable NotifyPublishedCohortsEnum notifyPublishedCohorts) {
    this.notifyPublishedCohorts = notifyPublishedCohorts;
  }

  public SettingsUpdate sendPaymentRemindersCohort(@Nullable Boolean sendPaymentRemindersCohort) {
    this.sendPaymentRemindersCohort = sendPaymentRemindersCohort;
    return this;
  }

  /**
   * Send payment reminders for cohorts.
   * @return sendPaymentRemindersCohort
   */
  
  @JsonProperty("sendPaymentRemindersCohort")
  public @Nullable Boolean getSendPaymentRemindersCohort() {
    return sendPaymentRemindersCohort;
  }

  @JsonProperty("sendPaymentRemindersCohort")
  public void setSendPaymentRemindersCohort(@Nullable Boolean sendPaymentRemindersCohort) {
    this.sendPaymentRemindersCohort = sendPaymentRemindersCohort;
  }

  public SettingsUpdate sendPaymentRemindersCourse(@Nullable Boolean sendPaymentRemindersCourse) {
    this.sendPaymentRemindersCourse = sendPaymentRemindersCourse;
    return this;
  }

  /**
   * Send payment reminders for courses.
   * @return sendPaymentRemindersCourse
   */
  
  @JsonProperty("sendPaymentRemindersCourse")
  public @Nullable Boolean getSendPaymentRemindersCourse() {
    return sendPaymentRemindersCourse;
  }

  @JsonProperty("sendPaymentRemindersCourse")
  public void setSendPaymentRemindersCourse(@Nullable Boolean sendPaymentRemindersCourse) {
    this.sendPaymentRemindersCourse = sendPaymentRemindersCourse;
  }

  public SettingsUpdate mentorRequestTemplateId(@Nullable UUID mentorRequestTemplateId) {
    this.mentorRequestTemplateId = mentorRequestTemplateId;
    return this;
  }

  /**
   * Mentor-request created template.
   * @return mentorRequestTemplateId
   */
  @Valid 
  @JsonProperty("mentorRequestTemplateId")
  public @Nullable UUID getMentorRequestTemplateId() {
    return mentorRequestTemplateId;
  }

  @JsonProperty("mentorRequestTemplateId")
  public void setMentorRequestTemplateId(@Nullable UUID mentorRequestTemplateId) {
    this.mentorRequestTemplateId = mentorRequestTemplateId;
  }

  public SettingsUpdate mentorStatusTemplateId(@Nullable UUID mentorStatusTemplateId) {
    this.mentorStatusTemplateId = mentorStatusTemplateId;
    return this;
  }

  /**
   * Mentor-request status template.
   * @return mentorStatusTemplateId
   */
  @Valid 
  @JsonProperty("mentorStatusTemplateId")
  public @Nullable UUID getMentorStatusTemplateId() {
    return mentorStatusTemplateId;
  }

  @JsonProperty("mentorStatusTemplateId")
  public void setMentorStatusTemplateId(@Nullable UUID mentorStatusTemplateId) {
    this.mentorStatusTemplateId = mentorStatusTemplateId;
  }

  public SettingsUpdate cohortConfirmationTemplateId(@Nullable UUID cohortConfirmationTemplateId) {
    this.cohortConfirmationTemplateId = cohortConfirmationTemplateId;
    return this;
  }

  /**
   * Default cohort confirmation template.
   * @return cohortConfirmationTemplateId
   */
  @Valid 
  @JsonProperty("cohortConfirmationTemplateId")
  public @Nullable UUID getCohortConfirmationTemplateId() {
    return cohortConfirmationTemplateId;
  }

  @JsonProperty("cohortConfirmationTemplateId")
  public void setCohortConfirmationTemplateId(@Nullable UUID cohortConfirmationTemplateId) {
    this.cohortConfirmationTemplateId = cohortConfirmationTemplateId;
  }

  public SettingsUpdate certificateTemplateId(@Nullable UUID certificateTemplateId) {
    this.certificateTemplateId = certificateTemplateId;
    return this;
  }

  /**
   * Certificate e-mail template.
   * @return certificateTemplateId
   */
  @Valid 
  @JsonProperty("certificateTemplateId")
  public @Nullable UUID getCertificateTemplateId() {
    return certificateTemplateId;
  }

  @JsonProperty("certificateTemplateId")
  public void setCertificateTemplateId(@Nullable UUID certificateTemplateId) {
    this.certificateTemplateId = certificateTemplateId;
  }

  public SettingsUpdate paymentReminderTemplateId(@Nullable UUID paymentReminderTemplateId) {
    this.paymentReminderTemplateId = paymentReminderTemplateId;
    return this;
  }

  /**
   * Payment-reminder template.
   * @return paymentReminderTemplateId
   */
  @Valid 
  @JsonProperty("paymentReminderTemplateId")
  public @Nullable UUID getPaymentReminderTemplateId() {
    return paymentReminderTemplateId;
  }

  @JsonProperty("paymentReminderTemplateId")
  public void setPaymentReminderTemplateId(@Nullable UUID paymentReminderTemplateId) {
    this.paymentReminderTemplateId = paymentReminderTemplateId;
  }

  public SettingsUpdate customSignupContent(@Nullable String customSignupContent) {
    this.customSignupContent = customSignupContent;
    return this;
  }

  /**
   * Custom HTML on the signup page.
   * @return customSignupContent
   */
  
  @JsonProperty("customSignupContent")
  public @Nullable String getCustomSignupContent() {
    return customSignupContent;
  }

  @JsonProperty("customSignupContent")
  public void setCustomSignupContent(@Nullable String customSignupContent) {
    this.customSignupContent = customSignupContent;
  }

  public SettingsUpdate metaDescription(@Nullable String metaDescription) {
    this.metaDescription = metaDescription;
    return this;
  }

  /**
   * SEO description.
   * @return metaDescription
   */
  
  @JsonProperty("metaDescription")
  public @Nullable String getMetaDescription() {
    return metaDescription;
  }

  @JsonProperty("metaDescription")
  public void setMetaDescription(@Nullable String metaDescription) {
    this.metaDescription = metaDescription;
  }

  public SettingsUpdate metaKeywords(@Nullable String metaKeywords) {
    this.metaKeywords = metaKeywords;
    return this;
  }

  /**
   * SEO keywords.
   * @return metaKeywords
   */
  
  @JsonProperty("metaKeywords")
  public @Nullable String getMetaKeywords() {
    return metaKeywords;
  }

  @JsonProperty("metaKeywords")
  public void setMetaKeywords(@Nullable String metaKeywords) {
    this.metaKeywords = metaKeywords;
  }

  public SettingsUpdate metaImageFileId(@Nullable UUID metaImageFileId) {
    this.metaImageFileId = metaImageFileId;
    return this;
  }

  /**
   * Social-share image.
   * @return metaImageFileId
   */
  @Valid 
  @JsonProperty("metaImageFileId")
  public @Nullable UUID getMetaImageFileId() {
    return metaImageFileId;
  }

  @JsonProperty("metaImageFileId")
  public void setMetaImageFileId(@Nullable UUID metaImageFileId) {
    this.metaImageFileId = metaImageFileId;
  }

  public SettingsUpdate contactUsEmail(@Nullable String contactUsEmail) {
    this.contactUsEmail = contactUsEmail;
    return this;
  }

  /**
   * Contact e-mail.
   * @return contactUsEmail
   */
  @Size(max = 320) 
  @JsonProperty("contactUsEmail")
  public @Nullable String getContactUsEmail() {
    return contactUsEmail;
  }

  @JsonProperty("contactUsEmail")
  public void setContactUsEmail(@Nullable String contactUsEmail) {
    this.contactUsEmail = contactUsEmail;
  }

  public SettingsUpdate contactUsUrl(@Nullable String contactUsUrl) {
    this.contactUsUrl = contactUsUrl;
    return this;
  }

  /**
   * Contact page URL.
   * @return contactUsUrl
   */
  @Size(max = 500) 
  @JsonProperty("contactUsUrl")
  public @Nullable String getContactUsUrl() {
    return contactUsUrl;
  }

  @JsonProperty("contactUsUrl")
  public void setContactUsUrl(@Nullable String contactUsUrl) {
    this.contactUsUrl = contactUsUrl;
  }

  public SettingsUpdate unsplashAccessKey(@Nullable String unsplashAccessKey) {
    this.unsplashAccessKey = unsplashAccessKey;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Image-search API key, encrypted.
   * @return unsplashAccessKey
   */
  
  @JsonProperty("unsplashAccessKey")
  public @Nullable String getUnsplashAccessKey() {
    return unsplashAccessKey;
  }

  @JsonProperty("unsplashAccessKey")
  public void setUnsplashAccessKey(@Nullable String unsplashAccessKey) {
    this.unsplashAccessKey = unsplashAccessKey;
  }

  public SettingsUpdate demoDataPresent(@Nullable Boolean demoDataPresent) {
    this.demoDataPresent = demoDataPresent;
    return this;
  }

  /**
   * Demo data loaded.
   * @return demoDataPresent
   */
  
  @JsonProperty("demoDataPresent")
  public @Nullable Boolean getDemoDataPresent() {
    return demoDataPresent;
  }

  @JsonProperty("demoDataPresent")
  public void setDemoDataPresent(@Nullable Boolean demoDataPresent) {
    this.demoDataPresent = demoDataPresent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SettingsUpdate settingsUpdate = (SettingsUpdate) o;
    return Objects.equals(this.livecodeUrl, settingsUpdate.livecodeUrl) &&
        Objects.equals(this.defaultCurrencyId, settingsUpdate.defaultCurrencyId) &&
        Objects.equals(this.applyGst, settingsUpdate.applyGst) &&
        Objects.equals(this.showUsdEquivalent, settingsUpdate.showUsdEquivalent) &&
        Objects.equals(this.applyRounding, settingsUpdate.applyRounding) &&
        Objects.equals(this.paymentGateway, settingsUpdate.paymentGateway) &&
        Objects.equals(this.identifyUserCategory, settingsUpdate.identifyUserCategory) &&
        Objects.equals(this.personaCaptured, settingsUpdate.personaCaptured) &&
        Objects.equals(this.lmsIsDefaultHome, settingsUpdate.lmsIsDefaultHome) &&
        Objects.equals(this.sendCalendarInviteForEvaluations, settingsUpdate.sendCalendarInviteForEvaluations) &&
        Objects.equals(this.allowGuestAccess, settingsUpdate.allowGuestAccess) &&
        Objects.equals(this.disableSignup, settingsUpdate.disableSignup) &&
        Objects.equals(this.disablePwa, settingsUpdate.disablePwa) &&
        Objects.equals(this.allowJobPosting, settingsUpdate.allowJobPosting) &&
        Objects.equals(this.certifiedMembersPage, settingsUpdate.certifiedMembersPage) &&
        Objects.equals(this.preventSkippingVideos, settingsUpdate.preventSkippingVideos) &&
        Objects.equals(this.lessonDwellSeconds, settingsUpdate.lessonDwellSeconds) &&
        Objects.equals(this.enforceVideoCompletion, settingsUpdate.enforceVideoCompletion) &&
        Objects.equals(this.enforceQuizCompletion, settingsUpdate.enforceQuizCompletion) &&
        Objects.equals(this.enforceQuizPassing, settingsUpdate.enforceQuizPassing) &&
        Objects.equals(this.enforceOpenEndedQuizPassing, settingsUpdate.enforceOpenEndedQuizPassing) &&
        Objects.equals(this.enforceAssignmentCompletion, settingsUpdate.enforceAssignmentCompletion) &&
        Objects.equals(this.enforceAssignmentPassing, settingsUpdate.enforceAssignmentPassing) &&
        Objects.equals(this.enforceExerciseCompletion, settingsUpdate.enforceExerciseCompletion) &&
        Objects.equals(this.enforceExercisePassing, settingsUpdate.enforceExercisePassing) &&
        Objects.equals(this.showCourses, settingsUpdate.showCourses) &&
        Objects.equals(this.showStudents, settingsUpdate.showStudents) &&
        Objects.equals(this.showAssessments, settingsUpdate.showAssessments) &&
        Objects.equals(this.showLiveClass, settingsUpdate.showLiveClass) &&
        Objects.equals(this.showDiscussions, settingsUpdate.showDiscussions) &&
        Objects.equals(this.showEmails, settingsUpdate.showEmails) &&
        Objects.equals(this.showDashboard, settingsUpdate.showDashboard) &&
        Objects.equals(this.showDayView, settingsUpdate.showDayView) &&
        Objects.equals(this.textDirection, settingsUpdate.textDirection) &&
        Objects.equals(this.notifyPublishedCourses, settingsUpdate.notifyPublishedCourses) &&
        Objects.equals(this.notifyPublishedCohorts, settingsUpdate.notifyPublishedCohorts) &&
        Objects.equals(this.sendPaymentRemindersCohort, settingsUpdate.sendPaymentRemindersCohort) &&
        Objects.equals(this.sendPaymentRemindersCourse, settingsUpdate.sendPaymentRemindersCourse) &&
        Objects.equals(this.mentorRequestTemplateId, settingsUpdate.mentorRequestTemplateId) &&
        Objects.equals(this.mentorStatusTemplateId, settingsUpdate.mentorStatusTemplateId) &&
        Objects.equals(this.cohortConfirmationTemplateId, settingsUpdate.cohortConfirmationTemplateId) &&
        Objects.equals(this.certificateTemplateId, settingsUpdate.certificateTemplateId) &&
        Objects.equals(this.paymentReminderTemplateId, settingsUpdate.paymentReminderTemplateId) &&
        Objects.equals(this.customSignupContent, settingsUpdate.customSignupContent) &&
        Objects.equals(this.metaDescription, settingsUpdate.metaDescription) &&
        Objects.equals(this.metaKeywords, settingsUpdate.metaKeywords) &&
        Objects.equals(this.metaImageFileId, settingsUpdate.metaImageFileId) &&
        Objects.equals(this.contactUsEmail, settingsUpdate.contactUsEmail) &&
        Objects.equals(this.contactUsUrl, settingsUpdate.contactUsUrl) &&
        Objects.equals(this.unsplashAccessKey, settingsUpdate.unsplashAccessKey) &&
        Objects.equals(this.demoDataPresent, settingsUpdate.demoDataPresent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(livecodeUrl, defaultCurrencyId, applyGst, showUsdEquivalent, applyRounding, paymentGateway, identifyUserCategory, personaCaptured, lmsIsDefaultHome, sendCalendarInviteForEvaluations, allowGuestAccess, disableSignup, disablePwa, allowJobPosting, certifiedMembersPage, preventSkippingVideos, lessonDwellSeconds, enforceVideoCompletion, enforceQuizCompletion, enforceQuizPassing, enforceOpenEndedQuizPassing, enforceAssignmentCompletion, enforceAssignmentPassing, enforceExerciseCompletion, enforceExercisePassing, showCourses, showStudents, showAssessments, showLiveClass, showDiscussions, showEmails, showDashboard, showDayView, textDirection, notifyPublishedCourses, notifyPublishedCohorts, sendPaymentRemindersCohort, sendPaymentRemindersCourse, mentorRequestTemplateId, mentorStatusTemplateId, cohortConfirmationTemplateId, certificateTemplateId, paymentReminderTemplateId, customSignupContent, metaDescription, metaKeywords, metaImageFileId, contactUsEmail, contactUsUrl, unsplashAccessKey, demoDataPresent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SettingsUpdate {\n");
    sb.append("    livecodeUrl: ").append(toIndentedString(livecodeUrl)).append("\n");
    sb.append("    defaultCurrencyId: ").append(toIndentedString(defaultCurrencyId)).append("\n");
    sb.append("    applyGst: ").append(toIndentedString(applyGst)).append("\n");
    sb.append("    showUsdEquivalent: ").append(toIndentedString(showUsdEquivalent)).append("\n");
    sb.append("    applyRounding: ").append(toIndentedString(applyRounding)).append("\n");
    sb.append("    paymentGateway: ").append(toIndentedString(paymentGateway)).append("\n");
    sb.append("    identifyUserCategory: ").append(toIndentedString(identifyUserCategory)).append("\n");
    sb.append("    personaCaptured: ").append(toIndentedString(personaCaptured)).append("\n");
    sb.append("    lmsIsDefaultHome: ").append(toIndentedString(lmsIsDefaultHome)).append("\n");
    sb.append("    sendCalendarInviteForEvaluations: ").append(toIndentedString(sendCalendarInviteForEvaluations)).append("\n");
    sb.append("    allowGuestAccess: ").append(toIndentedString(allowGuestAccess)).append("\n");
    sb.append("    disableSignup: ").append(toIndentedString(disableSignup)).append("\n");
    sb.append("    disablePwa: ").append(toIndentedString(disablePwa)).append("\n");
    sb.append("    allowJobPosting: ").append(toIndentedString(allowJobPosting)).append("\n");
    sb.append("    certifiedMembersPage: ").append(toIndentedString(certifiedMembersPage)).append("\n");
    sb.append("    preventSkippingVideos: ").append(toIndentedString(preventSkippingVideos)).append("\n");
    sb.append("    lessonDwellSeconds: ").append(toIndentedString(lessonDwellSeconds)).append("\n");
    sb.append("    enforceVideoCompletion: ").append(toIndentedString(enforceVideoCompletion)).append("\n");
    sb.append("    enforceQuizCompletion: ").append(toIndentedString(enforceQuizCompletion)).append("\n");
    sb.append("    enforceQuizPassing: ").append(toIndentedString(enforceQuizPassing)).append("\n");
    sb.append("    enforceOpenEndedQuizPassing: ").append(toIndentedString(enforceOpenEndedQuizPassing)).append("\n");
    sb.append("    enforceAssignmentCompletion: ").append(toIndentedString(enforceAssignmentCompletion)).append("\n");
    sb.append("    enforceAssignmentPassing: ").append(toIndentedString(enforceAssignmentPassing)).append("\n");
    sb.append("    enforceExerciseCompletion: ").append(toIndentedString(enforceExerciseCompletion)).append("\n");
    sb.append("    enforceExercisePassing: ").append(toIndentedString(enforceExercisePassing)).append("\n");
    sb.append("    showCourses: ").append(toIndentedString(showCourses)).append("\n");
    sb.append("    showStudents: ").append(toIndentedString(showStudents)).append("\n");
    sb.append("    showAssessments: ").append(toIndentedString(showAssessments)).append("\n");
    sb.append("    showLiveClass: ").append(toIndentedString(showLiveClass)).append("\n");
    sb.append("    showDiscussions: ").append(toIndentedString(showDiscussions)).append("\n");
    sb.append("    showEmails: ").append(toIndentedString(showEmails)).append("\n");
    sb.append("    showDashboard: ").append(toIndentedString(showDashboard)).append("\n");
    sb.append("    showDayView: ").append(toIndentedString(showDayView)).append("\n");
    sb.append("    textDirection: ").append(toIndentedString(textDirection)).append("\n");
    sb.append("    notifyPublishedCourses: ").append(toIndentedString(notifyPublishedCourses)).append("\n");
    sb.append("    notifyPublishedCohorts: ").append(toIndentedString(notifyPublishedCohorts)).append("\n");
    sb.append("    sendPaymentRemindersCohort: ").append(toIndentedString(sendPaymentRemindersCohort)).append("\n");
    sb.append("    sendPaymentRemindersCourse: ").append(toIndentedString(sendPaymentRemindersCourse)).append("\n");
    sb.append("    mentorRequestTemplateId: ").append(toIndentedString(mentorRequestTemplateId)).append("\n");
    sb.append("    mentorStatusTemplateId: ").append(toIndentedString(mentorStatusTemplateId)).append("\n");
    sb.append("    cohortConfirmationTemplateId: ").append(toIndentedString(cohortConfirmationTemplateId)).append("\n");
    sb.append("    certificateTemplateId: ").append(toIndentedString(certificateTemplateId)).append("\n");
    sb.append("    paymentReminderTemplateId: ").append(toIndentedString(paymentReminderTemplateId)).append("\n");
    sb.append("    customSignupContent: ").append(toIndentedString(customSignupContent)).append("\n");
    sb.append("    metaDescription: ").append(toIndentedString(metaDescription)).append("\n");
    sb.append("    metaKeywords: ").append(toIndentedString(metaKeywords)).append("\n");
    sb.append("    metaImageFileId: ").append(toIndentedString(metaImageFileId)).append("\n");
    sb.append("    contactUsEmail: ").append(toIndentedString(contactUsEmail)).append("\n");
    sb.append("    contactUsUrl: ").append(toIndentedString(contactUsUrl)).append("\n");
    sb.append("    unsplashAccessKey: ").append(toIndentedString(unsplashAccessKey)).append("\n");
    sb.append("    demoDataPresent: ").append(toIndentedString(demoDataPresent)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

