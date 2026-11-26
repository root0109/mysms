// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Cohort that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSubjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> details;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> categoryId;

  /**
   * Delivery medium.
   */
  public enum MediumEnum {
    ONLINE("online"),
    
    OFFLINE("offline");

    private final String value;

    MediumEnum(String value) {
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
    public static MediumEnum fromValue(String value) {
      for (MediumEnum b : MediumEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull MediumEnum> medium;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 64) String> timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> seatCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowSelfEnrollment;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowFutureAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPaid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> priceUsd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showLiveClass;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> evaluationEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> evaluationEndDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> certificationEnabled;

  /**
   * Live-class provider.
   */
  public enum ConferencingProviderEnum {
    ZOOM("zoom"),
    
    GOOGLE_MEET("google_meet");

    private final String value;

    ConferencingProviderEnum(String value) {
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
    public static ConferencingProviderEnum fromValue(String value) {
      for (ConferencingProviderEnum b : ConferencingProviderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<ConferencingProviderEnum> conferencingProvider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> conferencingAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> timetableTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> confirmationTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> metaImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> introVideoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> customComponent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> customScript;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> publishNotificationSent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 50) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> externalRef;

  /**
   * Delivery state (suspended = temporarily closed to members).
   */
  public enum LifecycleStatusEnum {
    DRAFT("draft"),
    
    ACTIVE("active"),
    
    SUSPENDED("suspended"),
    
    COMPLETED("completed"),
    
    ARCHIVED("archived");

    private final String value;

    LifecycleStatusEnum(String value) {
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
    public static LifecycleStatusEnum fromValue(String value) {
      for (LifecycleStatusEnum b : LifecycleStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull LifecycleStatusEnum> lifecycleStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> staffAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> staffAccessUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> studentAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> studentAccessUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> hasSchedule;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> staffCanEditSchedule;

  /**
   * Gradebook display mode.
   */
  public enum GradeDisplayEnum {
    POINTS("points"),
    
    PERCENTAGE("percentage");

    private final String value;

    GradeDisplayEnum(String value) {
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
    public static GradeDisplayEnum fromValue(String value) {
      for (GradeDisplayEnum b : GradeDisplayEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull GradeDisplayEnum> gradeDisplay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> gradingSchemeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> enrollmentOpensAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> enrollmentClosesAt;

  public CohortUpdate classSubjectId(@Nullable Optional<UUID> classSubjectId) {
    this.classSubjectId = classSubjectId;
    return this;
  }

  /**
   * K-12 class-section subject this cohort teaches, when school-linked.
   * @return classSubjectId
   */
  
  @JsonProperty("classSubjectId")
  public @Nullable Optional<UUID> getClassSubjectId() {
    return classSubjectId;
  }

  @JsonProperty("classSubjectId")
  public void setClassSubjectId(@Nullable Optional<UUID> classSubjectId) {
    this.classSubjectId = classSubjectId;
  }

  public CohortUpdate batchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Higher-ed batch this cohort serves, when school-linked.
   * @return batchId
   */
  
  @JsonProperty("batchId")
  public @Nullable Optional<UUID> getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
  }

  public CohortUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year, when school-linked.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public CohortUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution (NULL = tenant-wide).
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public CohortUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Cohort title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public CohortUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Short description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public CohortUpdate details(@Nullable Optional<String> details) {
    this.details = details;
    return this;
  }

  /**
   * Detailed information (rich text).
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable Optional<String> getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable Optional<String> details) {
    this.details = details;
  }

  public CohortUpdate categoryId(@Nullable Optional<UUID> categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  /**
   * Category.
   * @return categoryId
   */
  
  @JsonProperty("categoryId")
  public @Nullable Optional<UUID> getCategoryId() {
    return categoryId;
  }

  @JsonProperty("categoryId")
  public void setCategoryId(@Nullable Optional<UUID> categoryId) {
    this.categoryId = categoryId;
  }

  public CohortUpdate medium(@Nullable Optional<MediumEnum> medium) {
    this.medium = medium;
    return this;
  }

  /**
   * Delivery medium.
   * @return medium
   */
  
  @JsonProperty("medium")
  public @Nullable Optional<MediumEnum> getMedium() {
    return medium;
  }

  @JsonProperty("medium")
  public void setMedium(@Nullable Optional<MediumEnum> medium) {
    this.medium = medium;
  }

  public CohortUpdate startDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Start date.
   * @return startDate
   */
  
  @JsonProperty("startDate")
  public @Nullable Optional<LocalDate> getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
  }

  public CohortUpdate endDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * End date.
   * @return endDate
   */
  
  @JsonProperty("endDate")
  public @Nullable Optional<LocalDate> getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
  }

  public CohortUpdate startTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Daily start time.
   * @return startTime
   */
  
  @JsonProperty("startTime")
  public @Nullable Optional<String> getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
  }

  public CohortUpdate endTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Daily end time.
   * @return endTime
   */
  
  @JsonProperty("endTime")
  public @Nullable Optional<String> getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
  }

  public CohortUpdate timezone(@Nullable Optional<String> timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone.
   * @return timezone
   */
  
  @JsonProperty("timezone")
  public @Nullable Optional<String> getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable Optional<String> timezone) {
    this.timezone = timezone;
  }

  public CohortUpdate seatCount(@Nullable Optional<Integer> seatCount) {
    this.seatCount = seatCount;
    return this;
  }

  /**
   * Seats available.
   * @return seatCount
   */
  
  @JsonProperty("seatCount")
  public @Nullable Optional<Integer> getSeatCount() {
    return seatCount;
  }

  @JsonProperty("seatCount")
  public void setSeatCount(@Nullable Optional<Integer> seatCount) {
    this.seatCount = seatCount;
  }

  public CohortUpdate allowSelfEnrollment(@Nullable Optional<Boolean> allowSelfEnrollment) {
    this.allowSelfEnrollment = allowSelfEnrollment;
    return this;
  }

  /**
   * Students can enrol themselves.
   * @return allowSelfEnrollment
   */
  
  @JsonProperty("allowSelfEnrollment")
  public @Nullable Optional<Boolean> getAllowSelfEnrollment() {
    return allowSelfEnrollment;
  }

  @JsonProperty("allowSelfEnrollment")
  public void setAllowSelfEnrollment(@Nullable Optional<Boolean> allowSelfEnrollment) {
    this.allowSelfEnrollment = allowSelfEnrollment;
  }

  public CohortUpdate allowFutureAccess(@Nullable Optional<Boolean> allowFutureAccess) {
    this.allowFutureAccess = allowFutureAccess;
    return this;
  }

  /**
   * Students can open future timetable items.
   * @return allowFutureAccess
   */
  
  @JsonProperty("allowFutureAccess")
  public @Nullable Optional<Boolean> getAllowFutureAccess() {
    return allowFutureAccess;
  }

  @JsonProperty("allowFutureAccess")
  public void setAllowFutureAccess(@Nullable Optional<Boolean> allowFutureAccess) {
    this.allowFutureAccess = allowFutureAccess;
  }

  public CohortUpdate isPaid(@Nullable Optional<Boolean> isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Paid cohort.
   * @return isPaid
   */
  
  @JsonProperty("isPaid")
  public @Nullable Optional<Boolean> getIsPaid() {
    return isPaid;
  }

  @JsonProperty("isPaid")
  public void setIsPaid(@Nullable Optional<Boolean> isPaid) {
    this.isPaid = isPaid;
  }

  public CohortUpdate price(@Nullable Optional<String> price) {
    this.price = price;
    return this;
  }

  /**
   * Price.
   * @return price
   */
  
  @JsonProperty("price")
  public @Nullable Optional<String> getPrice() {
    return price;
  }

  @JsonProperty("price")
  public void setPrice(@Nullable Optional<String> price) {
    this.price = price;
  }

  public CohortUpdate currencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Price currency.
   * @return currencyId
   */
  
  @JsonProperty("currencyId")
  public @Nullable Optional<UUID> getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
  }

  public CohortUpdate priceUsd(@Nullable Optional<String> priceUsd) {
    this.priceUsd = priceUsd;
    return this;
  }

  /**
   * Equivalent USD price.
   * @return priceUsd
   */
  
  @JsonProperty("priceUsd")
  public @Nullable Optional<String> getPriceUsd() {
    return priceUsd;
  }

  @JsonProperty("priceUsd")
  public void setPriceUsd(@Nullable Optional<String> priceUsd) {
    this.priceUsd = priceUsd;
  }

  public CohortUpdate showLiveClass(@Nullable Optional<Boolean> showLiveClass) {
    this.showLiveClass = showLiveClass;
    return this;
  }

  /**
   * Show live classes tab.
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

  public CohortUpdate evaluationEnabled(@Nullable Optional<Boolean> evaluationEnabled) {
    this.evaluationEnabled = evaluationEnabled;
    return this;
  }

  /**
   * Cohort includes evaluations.
   * @return evaluationEnabled
   */
  
  @JsonProperty("evaluationEnabled")
  public @Nullable Optional<Boolean> getEvaluationEnabled() {
    return evaluationEnabled;
  }

  @JsonProperty("evaluationEnabled")
  public void setEvaluationEnabled(@Nullable Optional<Boolean> evaluationEnabled) {
    this.evaluationEnabled = evaluationEnabled;
  }

  public CohortUpdate evaluationEndDate(@Nullable Optional<LocalDate> evaluationEndDate) {
    this.evaluationEndDate = evaluationEndDate;
    return this;
  }

  /**
   * Last date for evaluations.
   * @return evaluationEndDate
   */
  
  @JsonProperty("evaluationEndDate")
  public @Nullable Optional<LocalDate> getEvaluationEndDate() {
    return evaluationEndDate;
  }

  @JsonProperty("evaluationEndDate")
  public void setEvaluationEndDate(@Nullable Optional<LocalDate> evaluationEndDate) {
    this.evaluationEndDate = evaluationEndDate;
  }

  public CohortUpdate certificationEnabled(@Nullable Optional<Boolean> certificationEnabled) {
    this.certificationEnabled = certificationEnabled;
    return this;
  }

  /**
   * Certificates issued.
   * @return certificationEnabled
   */
  
  @JsonProperty("certificationEnabled")
  public @Nullable Optional<Boolean> getCertificationEnabled() {
    return certificationEnabled;
  }

  @JsonProperty("certificationEnabled")
  public void setCertificationEnabled(@Nullable Optional<Boolean> certificationEnabled) {
    this.certificationEnabled = certificationEnabled;
  }

  public CohortUpdate conferencingProvider(@Nullable Optional<ConferencingProviderEnum> conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
    return this;
  }

  /**
   * Live-class provider.
   * @return conferencingProvider
   */
  
  @JsonProperty("conferencingProvider")
  public @Nullable Optional<ConferencingProviderEnum> getConferencingProvider() {
    return conferencingProvider;
  }

  @JsonProperty("conferencingProvider")
  public void setConferencingProvider(@Nullable Optional<ConferencingProviderEnum> conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
  }

  public CohortUpdate conferencingAccountId(@Nullable Optional<UUID> conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
    return this;
  }

  /**
   * Live-class account.
   * @return conferencingAccountId
   */
  
  @JsonProperty("conferencingAccountId")
  public @Nullable Optional<UUID> getConferencingAccountId() {
    return conferencingAccountId;
  }

  @JsonProperty("conferencingAccountId")
  public void setConferencingAccountId(@Nullable Optional<UUID> conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
  }

  public CohortUpdate timetableTemplateId(@Nullable Optional<UUID> timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template the timetable was created from.
   * @return timetableTemplateId
   */
  
  @JsonProperty("timetableTemplateId")
  public @Nullable Optional<UUID> getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(@Nullable Optional<UUID> timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public CohortUpdate confirmationTemplateId(@Nullable Optional<UUID> confirmationTemplateId) {
    this.confirmationTemplateId = confirmationTemplateId;
    return this;
  }

  /**
   * Enrolment confirmation e-mail template.
   * @return confirmationTemplateId
   */
  
  @JsonProperty("confirmationTemplateId")
  public @Nullable Optional<UUID> getConfirmationTemplateId() {
    return confirmationTemplateId;
  }

  @JsonProperty("confirmationTemplateId")
  public void setConfirmationTemplateId(@Nullable Optional<UUID> confirmationTemplateId) {
    this.confirmationTemplateId = confirmationTemplateId;
  }

  public CohortUpdate metaImageFileId(@Nullable Optional<UUID> metaImageFileId) {
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

  public CohortUpdate introVideoFileId(@Nullable Optional<UUID> introVideoFileId) {
    this.introVideoFileId = introVideoFileId;
    return this;
  }

  /**
   * Introductory video.
   * @return introVideoFileId
   */
  
  @JsonProperty("introVideoFileId")
  public @Nullable Optional<UUID> getIntroVideoFileId() {
    return introVideoFileId;
  }

  @JsonProperty("introVideoFileId")
  public void setIntroVideoFileId(@Nullable Optional<UUID> introVideoFileId) {
    this.introVideoFileId = introVideoFileId;
  }

  public CohortUpdate customComponent(@Nullable Optional<String> customComponent) {
    this.customComponent = customComponent;
    return this;
  }

  /**
   * Custom UI component markup.
   * @return customComponent
   */
  
  @JsonProperty("customComponent")
  public @Nullable Optional<String> getCustomComponent() {
    return customComponent;
  }

  @JsonProperty("customComponent")
  public void setCustomComponent(@Nullable Optional<String> customComponent) {
    this.customComponent = customComponent;
  }

  public CohortUpdate customScript(@Nullable Optional<String> customScript) {
    this.customScript = customScript;
    return this;
  }

  /**
   * Custom script.
   * @return customScript
   */
  
  @JsonProperty("customScript")
  public @Nullable Optional<String> getCustomScript() {
    return customScript;
  }

  @JsonProperty("customScript")
  public void setCustomScript(@Nullable Optional<String> customScript) {
    this.customScript = customScript;
  }

  public CohortUpdate publishNotificationSent(@Nullable Optional<Boolean> publishNotificationSent) {
    this.publishNotificationSent = publishNotificationSent;
    return this;
  }

  /**
   * Publication notification sent.
   * @return publishNotificationSent
   */
  
  @JsonProperty("publishNotificationSent")
  public @Nullable Optional<Boolean> getPublishNotificationSent() {
    return publishNotificationSent;
  }

  @JsonProperty("publishNotificationSent")
  public void setPublishNotificationSent(@Nullable Optional<Boolean> publishNotificationSent) {
    this.publishNotificationSent = publishNotificationSent;
  }

  public CohortUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Cohort (class / section) code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public CohortUpdate academicTermId(@Nullable Optional<UUID> academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Academic term the cohort runs in.
   * @return academicTermId
   */
  
  @JsonProperty("academicTermId")
  public @Nullable Optional<UUID> getAcademicTermId() {
    return academicTermId;
  }

  @JsonProperty("academicTermId")
  public void setAcademicTermId(@Nullable Optional<UUID> academicTermId) {
    this.academicTermId = academicTermId;
  }

  public CohortUpdate programId(@Nullable Optional<UUID> programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program the cohort belongs to.
   * @return programId
   */
  
  @JsonProperty("programId")
  public @Nullable Optional<UUID> getProgramId() {
    return programId;
  }

  @JsonProperty("programId")
  public void setProgramId(@Nullable Optional<UUID> programId) {
    this.programId = programId;
  }

  public CohortUpdate externalRef(@Nullable Optional<String> externalRef) {
    this.externalRef = externalRef;
    return this;
  }

  /**
   * External system reference (SIS integration key).
   * @return externalRef
   */
  
  @JsonProperty("externalRef")
  public @Nullable Optional<String> getExternalRef() {
    return externalRef;
  }

  @JsonProperty("externalRef")
  public void setExternalRef(@Nullable Optional<String> externalRef) {
    this.externalRef = externalRef;
  }

  public CohortUpdate lifecycleStatus(@Nullable Optional<LifecycleStatusEnum> lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
    return this;
  }

  /**
   * Delivery state (suspended = temporarily closed to members).
   * @return lifecycleStatus
   */
  
  @JsonProperty("lifecycleStatus")
  public @Nullable Optional<LifecycleStatusEnum> getLifecycleStatus() {
    return lifecycleStatus;
  }

  @JsonProperty("lifecycleStatus")
  public void setLifecycleStatus(@Nullable Optional<LifecycleStatusEnum> lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
  }

  public CohortUpdate staffAccessFrom(@Nullable Optional<OffsetDateTime> staffAccessFrom) {
    this.staffAccessFrom = staffAccessFrom;
    return this;
  }

  /**
   * Staff can enter from.
   * @return staffAccessFrom
   */
  
  @JsonProperty("staffAccessFrom")
  public @Nullable Optional<OffsetDateTime> getStaffAccessFrom() {
    return staffAccessFrom;
  }

  @JsonProperty("staffAccessFrom")
  public void setStaffAccessFrom(@Nullable Optional<OffsetDateTime> staffAccessFrom) {
    this.staffAccessFrom = staffAccessFrom;
  }

  public CohortUpdate staffAccessUntil(@Nullable Optional<OffsetDateTime> staffAccessUntil) {
    this.staffAccessUntil = staffAccessUntil;
    return this;
  }

  /**
   * Staff access ends.
   * @return staffAccessUntil
   */
  
  @JsonProperty("staffAccessUntil")
  public @Nullable Optional<OffsetDateTime> getStaffAccessUntil() {
    return staffAccessUntil;
  }

  @JsonProperty("staffAccessUntil")
  public void setStaffAccessUntil(@Nullable Optional<OffsetDateTime> staffAccessUntil) {
    this.staffAccessUntil = staffAccessUntil;
  }

  public CohortUpdate studentAccessFrom(@Nullable Optional<OffsetDateTime> studentAccessFrom) {
    this.studentAccessFrom = studentAccessFrom;
    return this;
  }

  /**
   * Students can enter from.
   * @return studentAccessFrom
   */
  
  @JsonProperty("studentAccessFrom")
  public @Nullable Optional<OffsetDateTime> getStudentAccessFrom() {
    return studentAccessFrom;
  }

  @JsonProperty("studentAccessFrom")
  public void setStudentAccessFrom(@Nullable Optional<OffsetDateTime> studentAccessFrom) {
    this.studentAccessFrom = studentAccessFrom;
  }

  public CohortUpdate studentAccessUntil(@Nullable Optional<OffsetDateTime> studentAccessUntil) {
    this.studentAccessUntil = studentAccessUntil;
    return this;
  }

  /**
   * Student access ends (read-only after the end date).
   * @return studentAccessUntil
   */
  
  @JsonProperty("studentAccessUntil")
  public @Nullable Optional<OffsetDateTime> getStudentAccessUntil() {
    return studentAccessUntil;
  }

  @JsonProperty("studentAccessUntil")
  public void setStudentAccessUntil(@Nullable Optional<OffsetDateTime> studentAccessUntil) {
    this.studentAccessUntil = studentAccessUntil;
  }

  public CohortUpdate hasSchedule(@Nullable Optional<Boolean> hasSchedule) {
    this.hasSchedule = hasSchedule;
    return this;
  }

  /**
   * Cohort meets on a weekly schedule.
   * @return hasSchedule
   */
  
  @JsonProperty("hasSchedule")
  public @Nullable Optional<Boolean> getHasSchedule() {
    return hasSchedule;
  }

  @JsonProperty("hasSchedule")
  public void setHasSchedule(@Nullable Optional<Boolean> hasSchedule) {
    this.hasSchedule = hasSchedule;
  }

  public CohortUpdate staffCanEditSchedule(@Nullable Optional<Boolean> staffCanEditSchedule) {
    this.staffCanEditSchedule = staffCanEditSchedule;
    return this;
  }

  /**
   * Cohort staff may change the schedule.
   * @return staffCanEditSchedule
   */
  
  @JsonProperty("staffCanEditSchedule")
  public @Nullable Optional<Boolean> getStaffCanEditSchedule() {
    return staffCanEditSchedule;
  }

  @JsonProperty("staffCanEditSchedule")
  public void setStaffCanEditSchedule(@Nullable Optional<Boolean> staffCanEditSchedule) {
    this.staffCanEditSchedule = staffCanEditSchedule;
  }

  public CohortUpdate gradeDisplay(@Nullable Optional<GradeDisplayEnum> gradeDisplay) {
    this.gradeDisplay = gradeDisplay;
    return this;
  }

  /**
   * Gradebook display mode.
   * @return gradeDisplay
   */
  
  @JsonProperty("gradeDisplay")
  public @Nullable Optional<GradeDisplayEnum> getGradeDisplay() {
    return gradeDisplay;
  }

  @JsonProperty("gradeDisplay")
  public void setGradeDisplay(@Nullable Optional<GradeDisplayEnum> gradeDisplay) {
    this.gradeDisplay = gradeDisplay;
  }

  public CohortUpdate gradingSchemeId(@Nullable Optional<UUID> gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
    return this;
  }

  /**
   * Letter-grade scale.
   * @return gradingSchemeId
   */
  
  @JsonProperty("gradingSchemeId")
  public @Nullable Optional<UUID> getGradingSchemeId() {
    return gradingSchemeId;
  }

  @JsonProperty("gradingSchemeId")
  public void setGradingSchemeId(@Nullable Optional<UUID> gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
  }

  public CohortUpdate enrollmentOpensAt(@Nullable Optional<OffsetDateTime> enrollmentOpensAt) {
    this.enrollmentOpensAt = enrollmentOpensAt;
    return this;
  }

  /**
   * Catalogue enrolment opens.
   * @return enrollmentOpensAt
   */
  
  @JsonProperty("enrollmentOpensAt")
  public @Nullable Optional<OffsetDateTime> getEnrollmentOpensAt() {
    return enrollmentOpensAt;
  }

  @JsonProperty("enrollmentOpensAt")
  public void setEnrollmentOpensAt(@Nullable Optional<OffsetDateTime> enrollmentOpensAt) {
    this.enrollmentOpensAt = enrollmentOpensAt;
  }

  public CohortUpdate enrollmentClosesAt(@Nullable Optional<OffsetDateTime> enrollmentClosesAt) {
    this.enrollmentClosesAt = enrollmentClosesAt;
    return this;
  }

  /**
   * Catalogue enrolment closes.
   * @return enrollmentClosesAt
   */
  
  @JsonProperty("enrollmentClosesAt")
  public @Nullable Optional<OffsetDateTime> getEnrollmentClosesAt() {
    return enrollmentClosesAt;
  }

  @JsonProperty("enrollmentClosesAt")
  public void setEnrollmentClosesAt(@Nullable Optional<OffsetDateTime> enrollmentClosesAt) {
    this.enrollmentClosesAt = enrollmentClosesAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortUpdate cohortUpdate = (CohortUpdate) o;
    return Objects.equals(this.classSubjectId, cohortUpdate.classSubjectId) &&
        Objects.equals(this.batchId, cohortUpdate.batchId) &&
        Objects.equals(this.academicYearId, cohortUpdate.academicYearId) &&
        Objects.equals(this.institutionId, cohortUpdate.institutionId) &&
        Objects.equals(this.title, cohortUpdate.title) &&
        Objects.equals(this.description, cohortUpdate.description) &&
        Objects.equals(this.details, cohortUpdate.details) &&
        Objects.equals(this.categoryId, cohortUpdate.categoryId) &&
        Objects.equals(this.medium, cohortUpdate.medium) &&
        Objects.equals(this.startDate, cohortUpdate.startDate) &&
        Objects.equals(this.endDate, cohortUpdate.endDate) &&
        Objects.equals(this.startTime, cohortUpdate.startTime) &&
        Objects.equals(this.endTime, cohortUpdate.endTime) &&
        Objects.equals(this.timezone, cohortUpdate.timezone) &&
        Objects.equals(this.seatCount, cohortUpdate.seatCount) &&
        Objects.equals(this.allowSelfEnrollment, cohortUpdate.allowSelfEnrollment) &&
        Objects.equals(this.allowFutureAccess, cohortUpdate.allowFutureAccess) &&
        Objects.equals(this.isPaid, cohortUpdate.isPaid) &&
        Objects.equals(this.price, cohortUpdate.price) &&
        Objects.equals(this.currencyId, cohortUpdate.currencyId) &&
        Objects.equals(this.priceUsd, cohortUpdate.priceUsd) &&
        Objects.equals(this.showLiveClass, cohortUpdate.showLiveClass) &&
        Objects.equals(this.evaluationEnabled, cohortUpdate.evaluationEnabled) &&
        Objects.equals(this.evaluationEndDate, cohortUpdate.evaluationEndDate) &&
        Objects.equals(this.certificationEnabled, cohortUpdate.certificationEnabled) &&
        Objects.equals(this.conferencingProvider, cohortUpdate.conferencingProvider) &&
        Objects.equals(this.conferencingAccountId, cohortUpdate.conferencingAccountId) &&
        Objects.equals(this.timetableTemplateId, cohortUpdate.timetableTemplateId) &&
        Objects.equals(this.confirmationTemplateId, cohortUpdate.confirmationTemplateId) &&
        Objects.equals(this.metaImageFileId, cohortUpdate.metaImageFileId) &&
        Objects.equals(this.introVideoFileId, cohortUpdate.introVideoFileId) &&
        Objects.equals(this.customComponent, cohortUpdate.customComponent) &&
        Objects.equals(this.customScript, cohortUpdate.customScript) &&
        Objects.equals(this.publishNotificationSent, cohortUpdate.publishNotificationSent) &&
        Objects.equals(this.code, cohortUpdate.code) &&
        Objects.equals(this.academicTermId, cohortUpdate.academicTermId) &&
        Objects.equals(this.programId, cohortUpdate.programId) &&
        Objects.equals(this.externalRef, cohortUpdate.externalRef) &&
        Objects.equals(this.lifecycleStatus, cohortUpdate.lifecycleStatus) &&
        Objects.equals(this.staffAccessFrom, cohortUpdate.staffAccessFrom) &&
        Objects.equals(this.staffAccessUntil, cohortUpdate.staffAccessUntil) &&
        Objects.equals(this.studentAccessFrom, cohortUpdate.studentAccessFrom) &&
        Objects.equals(this.studentAccessUntil, cohortUpdate.studentAccessUntil) &&
        Objects.equals(this.hasSchedule, cohortUpdate.hasSchedule) &&
        Objects.equals(this.staffCanEditSchedule, cohortUpdate.staffCanEditSchedule) &&
        Objects.equals(this.gradeDisplay, cohortUpdate.gradeDisplay) &&
        Objects.equals(this.gradingSchemeId, cohortUpdate.gradingSchemeId) &&
        Objects.equals(this.enrollmentOpensAt, cohortUpdate.enrollmentOpensAt) &&
        Objects.equals(this.enrollmentClosesAt, cohortUpdate.enrollmentClosesAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classSubjectId, batchId, academicYearId, institutionId, title, description, details, categoryId, medium, startDate, endDate, startTime, endTime, timezone, seatCount, allowSelfEnrollment, allowFutureAccess, isPaid, price, currencyId, priceUsd, showLiveClass, evaluationEnabled, evaluationEndDate, certificationEnabled, conferencingProvider, conferencingAccountId, timetableTemplateId, confirmationTemplateId, metaImageFileId, introVideoFileId, customComponent, customScript, publishNotificationSent, code, academicTermId, programId, externalRef, lifecycleStatus, staffAccessFrom, staffAccessUntil, studentAccessFrom, studentAccessUntil, hasSchedule, staffCanEditSchedule, gradeDisplay, gradingSchemeId, enrollmentOpensAt, enrollmentClosesAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortUpdate {\n");
    sb.append("    classSubjectId: ").append(toIndentedString(classSubjectId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
    sb.append("    categoryId: ").append(toIndentedString(categoryId)).append("\n");
    sb.append("    medium: ").append(toIndentedString(medium)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    seatCount: ").append(toIndentedString(seatCount)).append("\n");
    sb.append("    allowSelfEnrollment: ").append(toIndentedString(allowSelfEnrollment)).append("\n");
    sb.append("    allowFutureAccess: ").append(toIndentedString(allowFutureAccess)).append("\n");
    sb.append("    isPaid: ").append(toIndentedString(isPaid)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    priceUsd: ").append(toIndentedString(priceUsd)).append("\n");
    sb.append("    showLiveClass: ").append(toIndentedString(showLiveClass)).append("\n");
    sb.append("    evaluationEnabled: ").append(toIndentedString(evaluationEnabled)).append("\n");
    sb.append("    evaluationEndDate: ").append(toIndentedString(evaluationEndDate)).append("\n");
    sb.append("    certificationEnabled: ").append(toIndentedString(certificationEnabled)).append("\n");
    sb.append("    conferencingProvider: ").append(toIndentedString(conferencingProvider)).append("\n");
    sb.append("    conferencingAccountId: ").append(toIndentedString(conferencingAccountId)).append("\n");
    sb.append("    timetableTemplateId: ").append(toIndentedString(timetableTemplateId)).append("\n");
    sb.append("    confirmationTemplateId: ").append(toIndentedString(confirmationTemplateId)).append("\n");
    sb.append("    metaImageFileId: ").append(toIndentedString(metaImageFileId)).append("\n");
    sb.append("    introVideoFileId: ").append(toIndentedString(introVideoFileId)).append("\n");
    sb.append("    customComponent: ").append(toIndentedString(customComponent)).append("\n");
    sb.append("    customScript: ").append(toIndentedString(customScript)).append("\n");
    sb.append("    publishNotificationSent: ").append(toIndentedString(publishNotificationSent)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    academicTermId: ").append(toIndentedString(academicTermId)).append("\n");
    sb.append("    programId: ").append(toIndentedString(programId)).append("\n");
    sb.append("    externalRef: ").append(toIndentedString(externalRef)).append("\n");
    sb.append("    lifecycleStatus: ").append(toIndentedString(lifecycleStatus)).append("\n");
    sb.append("    staffAccessFrom: ").append(toIndentedString(staffAccessFrom)).append("\n");
    sb.append("    staffAccessUntil: ").append(toIndentedString(staffAccessUntil)).append("\n");
    sb.append("    studentAccessFrom: ").append(toIndentedString(studentAccessFrom)).append("\n");
    sb.append("    studentAccessUntil: ").append(toIndentedString(studentAccessUntil)).append("\n");
    sb.append("    hasSchedule: ").append(toIndentedString(hasSchedule)).append("\n");
    sb.append("    staffCanEditSchedule: ").append(toIndentedString(staffCanEditSchedule)).append("\n");
    sb.append("    gradeDisplay: ").append(toIndentedString(gradeDisplay)).append("\n");
    sb.append("    gradingSchemeId: ").append(toIndentedString(gradingSchemeId)).append("\n");
    sb.append("    enrollmentOpensAt: ").append(toIndentedString(enrollmentOpensAt)).append("\n");
    sb.append("    enrollmentClosesAt: ").append(toIndentedString(enrollmentClosesAt)).append("\n");
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

