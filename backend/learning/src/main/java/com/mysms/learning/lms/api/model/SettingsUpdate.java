// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of Settings that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SettingsUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> livecodeUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> defaultCurrencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> applyGst;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showUsdEquivalent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> applyRounding;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> paymentGateway;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> identifyUserCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> personaCaptured;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> lmsIsDefaultHome;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> sendCalendarInviteForEvaluations;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowGuestAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> disableSignup;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> disablePwa;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowJobPosting;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> certifiedMembersPage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> preventSkippingVideos;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> lessonDwellSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceVideoCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceQuizCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceQuizPassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceOpenEndedQuizPassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceAssignmentCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceAssignmentPassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceExerciseCompletion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceExercisePassing;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showCourses;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showStudents;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showAssessments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showLiveClass;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showDiscussions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showEmails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showDashboard;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showDayView;

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
  private @Nullable Optional<@NotNull TextDirectionEnum> textDirection;

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
  private @Nullable Optional<NotifyPublishedCoursesEnum> notifyPublishedCourses;

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
  private @Nullable Optional<NotifyPublishedCohortsEnum> notifyPublishedCohorts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> sendPaymentRemindersCohort;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> sendPaymentRemindersCourse;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> mentorRequestTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> mentorStatusTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortConfirmationTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> certificateTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> paymentReminderTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> customSignupContent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> metaDescription;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> metaKeywords;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> metaImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 320) String> contactUsEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> contactUsUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> unsplashAccessKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> demoDataPresent;

  public SettingsUpdate livecodeUrl(@Nullable Optional<String> livecodeUrl) {
    this.livecodeUrl = livecodeUrl;
    return this;
  }

  /**
   * Code-execution service URL.
   * @return livecodeUrl
   */
  
  @JsonProperty("livecodeUrl")
  public @Nullable Optional<String> getLivecodeUrl() {
    return livecodeUrl;
  }

  @JsonProperty("livecodeUrl")
  public void setLivecodeUrl(@Nullable Optional<String> livecodeUrl) {
    this.livecodeUrl = livecodeUrl;
  }

  public SettingsUpdate defaultCurrencyId(@Nullable Optional<UUID> defaultCurrencyId) {
    this.defaultCurrencyId = defaultCurrencyId;
    return this;
  }

  /**
   * Default currency for prices.
   * @return defaultCurrencyId
   */
  
  @JsonProperty("defaultCurrencyId")
  public @Nullable Optional<UUID> getDefaultCurrencyId() {
    return defaultCurrencyId;
  }

  @JsonProperty("defaultCurrencyId")
  public void setDefaultCurrencyId(@Nullable Optional<UUID> defaultCurrencyId) {
    this.defaultCurrencyId = defaultCurrencyId;
  }

  public SettingsUpdate applyGst(@Nullable Optional<Boolean> applyGst) {
    this.applyGst = applyGst;
    return this;
  }

  /**
   * Apply Indian GST on payments.
   * @return applyGst
   */
  
  @JsonProperty("applyGst")
  public @Nullable Optional<Boolean> getApplyGst() {
    return applyGst;
  }

  @JsonProperty("applyGst")
  public void setApplyGst(@Nullable Optional<Boolean> applyGst) {
    this.applyGst = applyGst;
  }

  public SettingsUpdate showUsdEquivalent(@Nullable Optional<Boolean> showUsdEquivalent) {
    this.showUsdEquivalent = showUsdEquivalent;
    return this;
  }

  /**
   * Show USD prices to international visitors.
   * @return showUsdEquivalent
   */
  
  @JsonProperty("showUsdEquivalent")
  public @Nullable Optional<Boolean> getShowUsdEquivalent() {
    return showUsdEquivalent;
  }

  @JsonProperty("showUsdEquivalent")
  public void setShowUsdEquivalent(@Nullable Optional<Boolean> showUsdEquivalent) {
    this.showUsdEquivalent = showUsdEquivalent;
  }

  public SettingsUpdate applyRounding(@Nullable Optional<Boolean> applyRounding) {
    this.applyRounding = applyRounding;
    return this;
  }

  /**
   * Round converted prices.
   * @return applyRounding
   */
  
  @JsonProperty("applyRounding")
  public @Nullable Optional<Boolean> getApplyRounding() {
    return applyRounding;
  }

  @JsonProperty("applyRounding")
  public void setApplyRounding(@Nullable Optional<Boolean> applyRounding) {
    this.applyRounding = applyRounding;
  }

  public SettingsUpdate paymentGateway(@Nullable Optional<String> paymentGateway) {
    this.paymentGateway = paymentGateway;
    return this;
  }

  /**
   * Payment gateway used.
   * @return paymentGateway
   */
  
  @JsonProperty("paymentGateway")
  public @Nullable Optional<String> getPaymentGateway() {
    return paymentGateway;
  }

  @JsonProperty("paymentGateway")
  public void setPaymentGateway(@Nullable Optional<String> paymentGateway) {
    this.paymentGateway = paymentGateway;
  }

  public SettingsUpdate identifyUserCategory(@Nullable Optional<Boolean> identifyUserCategory) {
    this.identifyUserCategory = identifyUserCategory;
    return this;
  }

  /**
   * Ask students for their persona at signup.
   * @return identifyUserCategory
   */
  
  @JsonProperty("identifyUserCategory")
  public @Nullable Optional<Boolean> getIdentifyUserCategory() {
    return identifyUserCategory;
  }

  @JsonProperty("identifyUserCategory")
  public void setIdentifyUserCategory(@Nullable Optional<Boolean> identifyUserCategory) {
    this.identifyUserCategory = identifyUserCategory;
  }

  public SettingsUpdate personaCaptured(@Nullable Optional<Boolean> personaCaptured) {
    this.personaCaptured = personaCaptured;
    return this;
  }

  /**
   * Persona survey completed.
   * @return personaCaptured
   */
  
  @JsonProperty("personaCaptured")
  public @Nullable Optional<Boolean> getPersonaCaptured() {
    return personaCaptured;
  }

  @JsonProperty("personaCaptured")
  public void setPersonaCaptured(@Nullable Optional<Boolean> personaCaptured) {
    this.personaCaptured = personaCaptured;
  }

  public SettingsUpdate lmsIsDefaultHome(@Nullable Optional<Boolean> lmsIsDefaultHome) {
    this.lmsIsDefaultHome = lmsIsDefaultHome;
    return this;
  }

  /**
   * LMS is the default landing page.
   * @return lmsIsDefaultHome
   */
  
  @JsonProperty("lmsIsDefaultHome")
  public @Nullable Optional<Boolean> getLmsIsDefaultHome() {
    return lmsIsDefaultHome;
  }

  @JsonProperty("lmsIsDefaultHome")
  public void setLmsIsDefaultHome(@Nullable Optional<Boolean> lmsIsDefaultHome) {
    this.lmsIsDefaultHome = lmsIsDefaultHome;
  }

  public SettingsUpdate sendCalendarInviteForEvaluations(@Nullable Optional<Boolean> sendCalendarInviteForEvaluations) {
    this.sendCalendarInviteForEvaluations = sendCalendarInviteForEvaluations;
    return this;
  }

  /**
   * Send calendar invites for evaluations.
   * @return sendCalendarInviteForEvaluations
   */
  
  @JsonProperty("sendCalendarInviteForEvaluations")
  public @Nullable Optional<Boolean> getSendCalendarInviteForEvaluations() {
    return sendCalendarInviteForEvaluations;
  }

  @JsonProperty("sendCalendarInviteForEvaluations")
  public void setSendCalendarInviteForEvaluations(@Nullable Optional<Boolean> sendCalendarInviteForEvaluations) {
    this.sendCalendarInviteForEvaluations = sendCalendarInviteForEvaluations;
  }

  public SettingsUpdate allowGuestAccess(@Nullable Optional<Boolean> allowGuestAccess) {
    this.allowGuestAccess = allowGuestAccess;
    return this;
  }

  /**
   * Visitors can browse without login.
   * @return allowGuestAccess
   */
  
  @JsonProperty("allowGuestAccess")
  public @Nullable Optional<Boolean> getAllowGuestAccess() {
    return allowGuestAccess;
  }

  @JsonProperty("allowGuestAccess")
  public void setAllowGuestAccess(@Nullable Optional<Boolean> allowGuestAccess) {
    this.allowGuestAccess = allowGuestAccess;
  }

  public SettingsUpdate disableSignup(@Nullable Optional<Boolean> disableSignup) {
    this.disableSignup = disableSignup;
    return this;
  }

  /**
   * Self-signup disabled.
   * @return disableSignup
   */
  
  @JsonProperty("disableSignup")
  public @Nullable Optional<Boolean> getDisableSignup() {
    return disableSignup;
  }

  @JsonProperty("disableSignup")
  public void setDisableSignup(@Nullable Optional<Boolean> disableSignup) {
    this.disableSignup = disableSignup;
  }

  public SettingsUpdate disablePwa(@Nullable Optional<Boolean> disablePwa) {
    this.disablePwa = disablePwa;
    return this;
  }

  /**
   * Disable installable web app.
   * @return disablePwa
   */
  
  @JsonProperty("disablePwa")
  public @Nullable Optional<Boolean> getDisablePwa() {
    return disablePwa;
  }

  @JsonProperty("disablePwa")
  public void setDisablePwa(@Nullable Optional<Boolean> disablePwa) {
    this.disablePwa = disablePwa;
  }

  public SettingsUpdate allowJobPosting(@Nullable Optional<Boolean> allowJobPosting) {
    this.allowJobPosting = allowJobPosting;
    return this;
  }

  /**
   * Users may post jobs.
   * @return allowJobPosting
   */
  
  @JsonProperty("allowJobPosting")
  public @Nullable Optional<Boolean> getAllowJobPosting() {
    return allowJobPosting;
  }

  @JsonProperty("allowJobPosting")
  public void setAllowJobPosting(@Nullable Optional<Boolean> allowJobPosting) {
    this.allowJobPosting = allowJobPosting;
  }

  public SettingsUpdate certifiedMembersPage(@Nullable Optional<Boolean> certifiedMembersPage) {
    this.certifiedMembersPage = certifiedMembersPage;
    return this;
  }

  /**
   * Show the certified-members directory.
   * @return certifiedMembersPage
   */
  
  @JsonProperty("certifiedMembersPage")
  public @Nullable Optional<Boolean> getCertifiedMembersPage() {
    return certifiedMembersPage;
  }

  @JsonProperty("certifiedMembersPage")
  public void setCertifiedMembersPage(@Nullable Optional<Boolean> certifiedMembersPage) {
    this.certifiedMembersPage = certifiedMembersPage;
  }

  public SettingsUpdate preventSkippingVideos(@Nullable Optional<Boolean> preventSkippingVideos) {
    this.preventSkippingVideos = preventSkippingVideos;
    return this;
  }

  /**
   * Videos cannot be skipped.
   * @return preventSkippingVideos
   */
  
  @JsonProperty("preventSkippingVideos")
  public @Nullable Optional<Boolean> getPreventSkippingVideos() {
    return preventSkippingVideos;
  }

  @JsonProperty("preventSkippingVideos")
  public void setPreventSkippingVideos(@Nullable Optional<Boolean> preventSkippingVideos) {
    this.preventSkippingVideos = preventSkippingVideos;
  }

  public SettingsUpdate lessonDwellSeconds(@Nullable Optional<Integer> lessonDwellSeconds) {
    this.lessonDwellSeconds = lessonDwellSeconds;
    return this;
  }

  /**
   * Seconds on a lesson before it counts as viewed.
   * @return lessonDwellSeconds
   */
  
  @JsonProperty("lessonDwellSeconds")
  public @Nullable Optional<Integer> getLessonDwellSeconds() {
    return lessonDwellSeconds;
  }

  @JsonProperty("lessonDwellSeconds")
  public void setLessonDwellSeconds(@Nullable Optional<Integer> lessonDwellSeconds) {
    this.lessonDwellSeconds = lessonDwellSeconds;
  }

  public SettingsUpdate enforceVideoCompletion(@Nullable Optional<Boolean> enforceVideoCompletion) {
    this.enforceVideoCompletion = enforceVideoCompletion;
    return this;
  }

  /**
   * Video must be watched to complete a lesson.
   * @return enforceVideoCompletion
   */
  
  @JsonProperty("enforceVideoCompletion")
  public @Nullable Optional<Boolean> getEnforceVideoCompletion() {
    return enforceVideoCompletion;
  }

  @JsonProperty("enforceVideoCompletion")
  public void setEnforceVideoCompletion(@Nullable Optional<Boolean> enforceVideoCompletion) {
    this.enforceVideoCompletion = enforceVideoCompletion;
  }

  public SettingsUpdate enforceQuizCompletion(@Nullable Optional<Boolean> enforceQuizCompletion) {
    this.enforceQuizCompletion = enforceQuizCompletion;
    return this;
  }

  /**
   * Quiz must be attempted.
   * @return enforceQuizCompletion
   */
  
  @JsonProperty("enforceQuizCompletion")
  public @Nullable Optional<Boolean> getEnforceQuizCompletion() {
    return enforceQuizCompletion;
  }

  @JsonProperty("enforceQuizCompletion")
  public void setEnforceQuizCompletion(@Nullable Optional<Boolean> enforceQuizCompletion) {
    this.enforceQuizCompletion = enforceQuizCompletion;
  }

  public SettingsUpdate enforceQuizPassing(@Nullable Optional<Boolean> enforceQuizPassing) {
    this.enforceQuizPassing = enforceQuizPassing;
    return this;
  }

  /**
   * Quiz must be passed.
   * @return enforceQuizPassing
   */
  
  @JsonProperty("enforceQuizPassing")
  public @Nullable Optional<Boolean> getEnforceQuizPassing() {
    return enforceQuizPassing;
  }

  @JsonProperty("enforceQuizPassing")
  public void setEnforceQuizPassing(@Nullable Optional<Boolean> enforceQuizPassing) {
    this.enforceQuizPassing = enforceQuizPassing;
  }

  public SettingsUpdate enforceOpenEndedQuizPassing(@Nullable Optional<Boolean> enforceOpenEndedQuizPassing) {
    this.enforceOpenEndedQuizPassing = enforceOpenEndedQuizPassing;
    return this;
  }

  /**
   * Open-ended quiz must be passed.
   * @return enforceOpenEndedQuizPassing
   */
  
  @JsonProperty("enforceOpenEndedQuizPassing")
  public @Nullable Optional<Boolean> getEnforceOpenEndedQuizPassing() {
    return enforceOpenEndedQuizPassing;
  }

  @JsonProperty("enforceOpenEndedQuizPassing")
  public void setEnforceOpenEndedQuizPassing(@Nullable Optional<Boolean> enforceOpenEndedQuizPassing) {
    this.enforceOpenEndedQuizPassing = enforceOpenEndedQuizPassing;
  }

  public SettingsUpdate enforceAssignmentCompletion(@Nullable Optional<Boolean> enforceAssignmentCompletion) {
    this.enforceAssignmentCompletion = enforceAssignmentCompletion;
    return this;
  }

  /**
   * Assignment must be submitted.
   * @return enforceAssignmentCompletion
   */
  
  @JsonProperty("enforceAssignmentCompletion")
  public @Nullable Optional<Boolean> getEnforceAssignmentCompletion() {
    return enforceAssignmentCompletion;
  }

  @JsonProperty("enforceAssignmentCompletion")
  public void setEnforceAssignmentCompletion(@Nullable Optional<Boolean> enforceAssignmentCompletion) {
    this.enforceAssignmentCompletion = enforceAssignmentCompletion;
  }

  public SettingsUpdate enforceAssignmentPassing(@Nullable Optional<Boolean> enforceAssignmentPassing) {
    this.enforceAssignmentPassing = enforceAssignmentPassing;
    return this;
  }

  /**
   * Assignment must be passed.
   * @return enforceAssignmentPassing
   */
  
  @JsonProperty("enforceAssignmentPassing")
  public @Nullable Optional<Boolean> getEnforceAssignmentPassing() {
    return enforceAssignmentPassing;
  }

  @JsonProperty("enforceAssignmentPassing")
  public void setEnforceAssignmentPassing(@Nullable Optional<Boolean> enforceAssignmentPassing) {
    this.enforceAssignmentPassing = enforceAssignmentPassing;
  }

  public SettingsUpdate enforceExerciseCompletion(@Nullable Optional<Boolean> enforceExerciseCompletion) {
    this.enforceExerciseCompletion = enforceExerciseCompletion;
    return this;
  }

  /**
   * Exercise must be submitted.
   * @return enforceExerciseCompletion
   */
  
  @JsonProperty("enforceExerciseCompletion")
  public @Nullable Optional<Boolean> getEnforceExerciseCompletion() {
    return enforceExerciseCompletion;
  }

  @JsonProperty("enforceExerciseCompletion")
  public void setEnforceExerciseCompletion(@Nullable Optional<Boolean> enforceExerciseCompletion) {
    this.enforceExerciseCompletion = enforceExerciseCompletion;
  }

  public SettingsUpdate enforceExercisePassing(@Nullable Optional<Boolean> enforceExercisePassing) {
    this.enforceExercisePassing = enforceExercisePassing;
    return this;
  }

  /**
   * Exercise must be passed.
   * @return enforceExercisePassing
   */
  
  @JsonProperty("enforceExercisePassing")
  public @Nullable Optional<Boolean> getEnforceExercisePassing() {
    return enforceExercisePassing;
  }

  @JsonProperty("enforceExercisePassing")
  public void setEnforceExercisePassing(@Nullable Optional<Boolean> enforceExercisePassing) {
    this.enforceExercisePassing = enforceExercisePassing;
  }

  public SettingsUpdate showCourses(@Nullable Optional<Boolean> showCourses) {
    this.showCourses = showCourses;
    return this;
  }

  /**
   * Show the courses section.
   * @return showCourses
   */
  
  @JsonProperty("showCourses")
  public @Nullable Optional<Boolean> getShowCourses() {
    return showCourses;
  }

  @JsonProperty("showCourses")
  public void setShowCourses(@Nullable Optional<Boolean> showCourses) {
    this.showCourses = showCourses;
  }

  public SettingsUpdate showStudents(@Nullable Optional<Boolean> showStudents) {
    this.showStudents = showStudents;
    return this;
  }

  /**
   * Show the students section.
   * @return showStudents
   */
  
  @JsonProperty("showStudents")
  public @Nullable Optional<Boolean> getShowStudents() {
    return showStudents;
  }

  @JsonProperty("showStudents")
  public void setShowStudents(@Nullable Optional<Boolean> showStudents) {
    this.showStudents = showStudents;
  }

  public SettingsUpdate showAssessments(@Nullable Optional<Boolean> showAssessments) {
    this.showAssessments = showAssessments;
    return this;
  }

  /**
   * Show the assessments section.
   * @return showAssessments
   */
  
  @JsonProperty("showAssessments")
  public @Nullable Optional<Boolean> getShowAssessments() {
    return showAssessments;
  }

  @JsonProperty("showAssessments")
  public void setShowAssessments(@Nullable Optional<Boolean> showAssessments) {
    this.showAssessments = showAssessments;
  }

  public SettingsUpdate showLiveClass(@Nullable Optional<Boolean> showLiveClass) {
    this.showLiveClass = showLiveClass;
    return this;
  }

  /**
   * Show the live-class section.
   * @return showLiveClass
   */
  
  @JsonProperty("showLiveClass")
  public @Nullable Optional<Boolean> getShowLiveClass() {
    return showLiveClass;
  }

  @JsonProperty("showLiveClass")
  public void setShowLiveClass(@Nullable Optional<Boolean> showLiveClass) {
    this.showLiveClass = showLiveClass;
  }

  public SettingsUpdate showDiscussions(@Nullable Optional<Boolean> showDiscussions) {
    this.showDiscussions = showDiscussions;
    return this;
  }

  /**
   * Show discussions.
   * @return showDiscussions
   */
  
  @JsonProperty("showDiscussions")
  public @Nullable Optional<Boolean> getShowDiscussions() {
    return showDiscussions;
  }

  @JsonProperty("showDiscussions")
  public void setShowDiscussions(@Nullable Optional<Boolean> showDiscussions) {
    this.showDiscussions = showDiscussions;
  }

  public SettingsUpdate showEmails(@Nullable Optional<Boolean> showEmails) {
    this.showEmails = showEmails;
    return this;
  }

  /**
   * Show the e-mails section.
   * @return showEmails
   */
  
  @JsonProperty("showEmails")
  public @Nullable Optional<Boolean> getShowEmails() {
    return showEmails;
  }

  @JsonProperty("showEmails")
  public void setShowEmails(@Nullable Optional<Boolean> showEmails) {
    this.showEmails = showEmails;
  }

  public SettingsUpdate showDashboard(@Nullable Optional<Boolean> showDashboard) {
    this.showDashboard = showDashboard;
    return this;
  }

  /**
   * Show the dashboard.
   * @return showDashboard
   */
  
  @JsonProperty("showDashboard")
  public @Nullable Optional<Boolean> getShowDashboard() {
    return showDashboard;
  }

  @JsonProperty("showDashboard")
  public void setShowDashboard(@Nullable Optional<Boolean> showDashboard) {
    this.showDashboard = showDashboard;
  }

  public SettingsUpdate showDayView(@Nullable Optional<Boolean> showDayView) {
    this.showDayView = showDayView;
    return this;
  }

  /**
   * Day view in timetables.
   * @return showDayView
   */
  
  @JsonProperty("showDayView")
  public @Nullable Optional<Boolean> getShowDayView() {
    return showDayView;
  }

  @JsonProperty("showDayView")
  public void setShowDayView(@Nullable Optional<Boolean> showDayView) {
    this.showDayView = showDayView;
  }

  public SettingsUpdate textDirection(@Nullable Optional<TextDirectionEnum> textDirection) {
    this.textDirection = textDirection;
    return this;
  }

  /**
   * Text direction.
   * @return textDirection
   */
  
  @JsonProperty("textDirection")
  public @Nullable Optional<TextDirectionEnum> getTextDirection() {
    return textDirection;
  }

  @JsonProperty("textDirection")
  public void setTextDirection(@Nullable Optional<TextDirectionEnum> textDirection) {
    this.textDirection = textDirection;
  }

  public SettingsUpdate notifyPublishedCourses(@Nullable Optional<NotifyPublishedCoursesEnum> notifyPublishedCourses) {
    this.notifyPublishedCourses = notifyPublishedCourses;
    return this;
  }

  /**
   * Channel for new-course announcements.
   * @return notifyPublishedCourses
   */
  
  @JsonProperty("notifyPublishedCourses")
  public @Nullable Optional<NotifyPublishedCoursesEnum> getNotifyPublishedCourses() {
    return notifyPublishedCourses;
  }

  @JsonProperty("notifyPublishedCourses")
  public void setNotifyPublishedCourses(@Nullable Optional<NotifyPublishedCoursesEnum> notifyPublishedCourses) {
    this.notifyPublishedCourses = notifyPublishedCourses;
  }

  public SettingsUpdate notifyPublishedCohorts(@Nullable Optional<NotifyPublishedCohortsEnum> notifyPublishedCohorts) {
    this.notifyPublishedCohorts = notifyPublishedCohorts;
    return this;
  }

  /**
   * Channel for new-cohort announcements.
   * @return notifyPublishedCohorts
   */
  
  @JsonProperty("notifyPublishedCohorts")
  public @Nullable Optional<NotifyPublishedCohortsEnum> getNotifyPublishedCohorts() {
    return notifyPublishedCohorts;
  }

  @JsonProperty("notifyPublishedCohorts")
  public void setNotifyPublishedCohorts(@Nullable Optional<NotifyPublishedCohortsEnum> notifyPublishedCohorts) {
    this.notifyPublishedCohorts = notifyPublishedCohorts;
  }

  public SettingsUpdate sendPaymentRemindersCohort(@Nullable Optional<Boolean> sendPaymentRemindersCohort) {
    this.sendPaymentRemindersCohort = sendPaymentRemindersCohort;
    return this;
  }

  /**
   * Send payment reminders for cohorts.
   * @return sendPaymentRemindersCohort
   */
  
  @JsonProperty("sendPaymentRemindersCohort")
  public @Nullable Optional<Boolean> getSendPaymentRemindersCohort() {
    return sendPaymentRemindersCohort;
  }

  @JsonProperty("sendPaymentRemindersCohort")
  public void setSendPaymentRemindersCohort(@Nullable Optional<Boolean> sendPaymentRemindersCohort) {
    this.sendPaymentRemindersCohort = sendPaymentRemindersCohort;
  }

  public SettingsUpdate sendPaymentRemindersCourse(@Nullable Optional<Boolean> sendPaymentRemindersCourse) {
    this.sendPaymentRemindersCourse = sendPaymentRemindersCourse;
    return this;
  }

  /**
   * Send payment reminders for courses.
   * @return sendPaymentRemindersCourse
   */
  
  @JsonProperty("sendPaymentRemindersCourse")
  public @Nullable Optional<Boolean> getSendPaymentRemindersCourse() {
    return sendPaymentRemindersCourse;
  }

  @JsonProperty("sendPaymentRemindersCourse")
  public void setSendPaymentRemindersCourse(@Nullable Optional<Boolean> sendPaymentRemindersCourse) {
    this.sendPaymentRemindersCourse = sendPaymentRemindersCourse;
  }

  public SettingsUpdate mentorRequestTemplateId(@Nullable Optional<UUID> mentorRequestTemplateId) {
    this.mentorRequestTemplateId = mentorRequestTemplateId;
    return this;
  }

  /**
   * Mentor-request created template.
   * @return mentorRequestTemplateId
   */
  
  @JsonProperty("mentorRequestTemplateId")
  public @Nullable Optional<UUID> getMentorRequestTemplateId() {
    return mentorRequestTemplateId;
  }

  @JsonProperty("mentorRequestTemplateId")
  public void setMentorRequestTemplateId(@Nullable Optional<UUID> mentorRequestTemplateId) {
    this.mentorRequestTemplateId = mentorRequestTemplateId;
  }

  public SettingsUpdate mentorStatusTemplateId(@Nullable Optional<UUID> mentorStatusTemplateId) {
    this.mentorStatusTemplateId = mentorStatusTemplateId;
    return this;
  }

  /**
   * Mentor-request status template.
   * @return mentorStatusTemplateId
   */
  
  @JsonProperty("mentorStatusTemplateId")
  public @Nullable Optional<UUID> getMentorStatusTemplateId() {
    return mentorStatusTemplateId;
  }

  @JsonProperty("mentorStatusTemplateId")
  public void setMentorStatusTemplateId(@Nullable Optional<UUID> mentorStatusTemplateId) {
    this.mentorStatusTemplateId = mentorStatusTemplateId;
  }

  public SettingsUpdate cohortConfirmationTemplateId(@Nullable Optional<UUID> cohortConfirmationTemplateId) {
    this.cohortConfirmationTemplateId = cohortConfirmationTemplateId;
    return this;
  }

  /**
   * Default cohort confirmation template.
   * @return cohortConfirmationTemplateId
   */
  
  @JsonProperty("cohortConfirmationTemplateId")
  public @Nullable Optional<UUID> getCohortConfirmationTemplateId() {
    return cohortConfirmationTemplateId;
  }

  @JsonProperty("cohortConfirmationTemplateId")
  public void setCohortConfirmationTemplateId(@Nullable Optional<UUID> cohortConfirmationTemplateId) {
    this.cohortConfirmationTemplateId = cohortConfirmationTemplateId;
  }

  public SettingsUpdate certificateTemplateId(@Nullable Optional<UUID> certificateTemplateId) {
    this.certificateTemplateId = certificateTemplateId;
    return this;
  }

  /**
   * Certificate e-mail template.
   * @return certificateTemplateId
   */
  
  @JsonProperty("certificateTemplateId")
  public @Nullable Optional<UUID> getCertificateTemplateId() {
    return certificateTemplateId;
  }

  @JsonProperty("certificateTemplateId")
  public void setCertificateTemplateId(@Nullable Optional<UUID> certificateTemplateId) {
    this.certificateTemplateId = certificateTemplateId;
  }

  public SettingsUpdate paymentReminderTemplateId(@Nullable Optional<UUID> paymentReminderTemplateId) {
    this.paymentReminderTemplateId = paymentReminderTemplateId;
    return this;
  }

  /**
   * Payment-reminder template.
   * @return paymentReminderTemplateId
   */
  
  @JsonProperty("paymentReminderTemplateId")
  public @Nullable Optional<UUID> getPaymentReminderTemplateId() {
    return paymentReminderTemplateId;
  }

  @JsonProperty("paymentReminderTemplateId")
  public void setPaymentReminderTemplateId(@Nullable Optional<UUID> paymentReminderTemplateId) {
    this.paymentReminderTemplateId = paymentReminderTemplateId;
  }

  public SettingsUpdate customSignupContent(@Nullable Optional<String> customSignupContent) {
    this.customSignupContent = customSignupContent;
    return this;
  }

  /**
   * Custom HTML on the signup page.
   * @return customSignupContent
   */
  
  @JsonProperty("customSignupContent")
  public @Nullable Optional<String> getCustomSignupContent() {
    return customSignupContent;
  }

  @JsonProperty("customSignupContent")
  public void setCustomSignupContent(@Nullable Optional<String> customSignupContent) {
    this.customSignupContent = customSignupContent;
  }

  public SettingsUpdate metaDescription(@Nullable Optional<String> metaDescription) {
    this.metaDescription = metaDescription;
    return this;
  }

  /**
   * SEO description.
   * @return metaDescription
   */
  
  @JsonProperty("metaDescription")
  public @Nullable Optional<String> getMetaDescription() {
    return metaDescription;
  }

  @JsonProperty("metaDescription")
  public void setMetaDescription(@Nullable Optional<String> metaDescription) {
    this.metaDescription = metaDescription;
  }

  public SettingsUpdate metaKeywords(@Nullable Optional<String> metaKeywords) {
    this.metaKeywords = metaKeywords;
    return this;
  }

  /**
   * SEO keywords.
   * @return metaKeywords
   */
  
  @JsonProperty("metaKeywords")
  public @Nullable Optional<String> getMetaKeywords() {
    return metaKeywords;
  }

  @JsonProperty("metaKeywords")
  public void setMetaKeywords(@Nullable Optional<String> metaKeywords) {
    this.metaKeywords = metaKeywords;
  }

  public SettingsUpdate metaImageFileId(@Nullable Optional<UUID> metaImageFileId) {
    this.metaImageFileId = metaImageFileId;
    return this;
  }

  /**
   * Social-share image.
   * @return metaImageFileId
   */
  
  @JsonProperty("metaImageFileId")
  public @Nullable Optional<UUID> getMetaImageFileId() {
    return metaImageFileId;
  }

  @JsonProperty("metaImageFileId")
  public void setMetaImageFileId(@Nullable Optional<UUID> metaImageFileId) {
    this.metaImageFileId = metaImageFileId;
  }

  public SettingsUpdate contactUsEmail(@Nullable Optional<String> contactUsEmail) {
    this.contactUsEmail = contactUsEmail;
    return this;
  }

  /**
   * Contact e-mail.
   * @return contactUsEmail
   */
  
  @JsonProperty("contactUsEmail")
  public @Nullable Optional<String> getContactUsEmail() {
    return contactUsEmail;
  }

  @JsonProperty("contactUsEmail")
  public void setContactUsEmail(@Nullable Optional<String> contactUsEmail) {
    this.contactUsEmail = contactUsEmail;
  }

  public SettingsUpdate contactUsUrl(@Nullable Optional<String> contactUsUrl) {
    this.contactUsUrl = contactUsUrl;
    return this;
  }

  /**
   * Contact page URL.
   * @return contactUsUrl
   */
  
  @JsonProperty("contactUsUrl")
  public @Nullable Optional<String> getContactUsUrl() {
    return contactUsUrl;
  }

  @JsonProperty("contactUsUrl")
  public void setContactUsUrl(@Nullable Optional<String> contactUsUrl) {
    this.contactUsUrl = contactUsUrl;
  }

  public SettingsUpdate unsplashAccessKey(@Nullable Optional<String> unsplashAccessKey) {
    this.unsplashAccessKey = unsplashAccessKey;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Image-search API key, encrypted.
   * @return unsplashAccessKey
   */
  
  @JsonProperty("unsplashAccessKey")
  public @Nullable Optional<String> getUnsplashAccessKey() {
    return unsplashAccessKey;
  }

  @JsonProperty("unsplashAccessKey")
  public void setUnsplashAccessKey(@Nullable Optional<String> unsplashAccessKey) {
    this.unsplashAccessKey = unsplashAccessKey;
  }

  public SettingsUpdate demoDataPresent(@Nullable Optional<Boolean> demoDataPresent) {
    this.demoDataPresent = demoDataPresent;
    return this;
  }

  /**
   * Demo data loaded.
   * @return demoDataPresent
   */
  
  @JsonProperty("demoDataPresent")
  public @Nullable Optional<Boolean> getDemoDataPresent() {
    return demoDataPresent;
  }

  @JsonProperty("demoDataPresent")
  public void setDemoDataPresent(@Nullable Optional<Boolean> demoDataPresent) {
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

