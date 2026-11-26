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
 * Teacher-led cohort running one or more courses over fixed dates (online or offline).
 */

public class Cohort {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSubjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String details;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID categoryId;

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
  private @Nullable MediumEnum medium;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer seatCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPublished;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowSelfEnrollment;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowFutureAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPaid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String priceUsd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showLiveClass;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean evaluationEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate evaluationEndDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean certificationEnabled;

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
  private @Nullable ConferencingProviderEnum conferencingProvider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID conferencingAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID timetableTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID confirmationTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID metaImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID introVideoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String customComponent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String customScript;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean publishNotificationSent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String externalRef;

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
  private @Nullable LifecycleStatusEnum lifecycleStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime staffAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime staffAccessUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime studentAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime studentAccessUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hasSchedule;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean staffCanEditSchedule;

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
  private @Nullable GradeDisplayEnum gradeDisplay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradingSchemeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime enrollmentOpensAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime enrollmentClosesAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Cohort() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Cohort(String title, LocalDate startDate, LocalDate endDate, String timezone) {
    this.title = title;
    this.startDate = startDate;
    this.endDate = endDate;
    this.timezone = timezone;
  }

  public Cohort classSubjectId(@Nullable UUID classSubjectId) {
    this.classSubjectId = classSubjectId;
    return this;
  }

  /**
   * K-12 class-section subject this cohort teaches, when school-linked.
   * @return classSubjectId
   */
  @Valid 
  @JsonProperty("classSubjectId")
  public @Nullable UUID getClassSubjectId() {
    return classSubjectId;
  }

  @JsonProperty("classSubjectId")
  public void setClassSubjectId(@Nullable UUID classSubjectId) {
    this.classSubjectId = classSubjectId;
  }

  public Cohort batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Higher-ed batch this cohort serves, when school-linked.
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public Cohort academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year, when school-linked.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public Cohort id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public Cohort institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution (NULL = tenant-wide).
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public Cohort title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Cohort title.
   * @return title
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public Cohort description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Short description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public Cohort details(@Nullable String details) {
    this.details = details;
    return this;
  }

  /**
   * Detailed information (rich text).
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable String getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable String details) {
    this.details = details;
  }

  public Cohort categoryId(@Nullable UUID categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  /**
   * Category.
   * @return categoryId
   */
  @Valid 
  @JsonProperty("categoryId")
  public @Nullable UUID getCategoryId() {
    return categoryId;
  }

  @JsonProperty("categoryId")
  public void setCategoryId(@Nullable UUID categoryId) {
    this.categoryId = categoryId;
  }

  public Cohort medium(@Nullable MediumEnum medium) {
    this.medium = medium;
    return this;
  }

  /**
   * Delivery medium.
   * @return medium
   */
  
  @JsonProperty("medium")
  public @Nullable MediumEnum getMedium() {
    return medium;
  }

  @JsonProperty("medium")
  public void setMedium(@Nullable MediumEnum medium) {
    this.medium = medium;
  }

  public Cohort startDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Start date.
   * @return startDate
   */
  @NotNull @Valid 
  @JsonProperty("startDate")
  public LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public Cohort endDate(LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * End date.
   * @return endDate
   */
  @NotNull @Valid 
  @JsonProperty("endDate")
  public LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Cohort startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Daily start time.
   * @return startTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public @Nullable String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable String startTime) {
    this.startTime = startTime;
  }

  public Cohort endTime(@Nullable String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Daily end time.
   * @return endTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("endTime")
  public @Nullable String getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable String endTime) {
    this.endTime = endTime;
  }

  public Cohort timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone.
   * @return timezone
   */
  @NotNull @Size(max = 64) 
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public Cohort seatCount(@Nullable Integer seatCount) {
    this.seatCount = seatCount;
    return this;
  }

  /**
   * Seats available.
   * @return seatCount
   */
  
  @JsonProperty("seatCount")
  public @Nullable Integer getSeatCount() {
    return seatCount;
  }

  @JsonProperty("seatCount")
  public void setSeatCount(@Nullable Integer seatCount) {
    this.seatCount = seatCount;
  }

  public Cohort isPublished(@Nullable Boolean isPublished) {
    this.isPublished = isPublished;
    return this;
  }

  /**
   * Visible in the catalogue. Changed only through the workflow operations.
   * @return isPublished
   */
  
  @JsonProperty("isPublished")
  public @Nullable Boolean getIsPublished() {
    return isPublished;
  }

  @JsonProperty("isPublished")
  public void setIsPublished(@Nullable Boolean isPublished) {
    this.isPublished = isPublished;
  }

  public Cohort allowSelfEnrollment(@Nullable Boolean allowSelfEnrollment) {
    this.allowSelfEnrollment = allowSelfEnrollment;
    return this;
  }

  /**
   * Students can enrol themselves.
   * @return allowSelfEnrollment
   */
  
  @JsonProperty("allowSelfEnrollment")
  public @Nullable Boolean getAllowSelfEnrollment() {
    return allowSelfEnrollment;
  }

  @JsonProperty("allowSelfEnrollment")
  public void setAllowSelfEnrollment(@Nullable Boolean allowSelfEnrollment) {
    this.allowSelfEnrollment = allowSelfEnrollment;
  }

  public Cohort allowFutureAccess(@Nullable Boolean allowFutureAccess) {
    this.allowFutureAccess = allowFutureAccess;
    return this;
  }

  /**
   * Students can open future timetable items.
   * @return allowFutureAccess
   */
  
  @JsonProperty("allowFutureAccess")
  public @Nullable Boolean getAllowFutureAccess() {
    return allowFutureAccess;
  }

  @JsonProperty("allowFutureAccess")
  public void setAllowFutureAccess(@Nullable Boolean allowFutureAccess) {
    this.allowFutureAccess = allowFutureAccess;
  }

  public Cohort isPaid(@Nullable Boolean isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Paid cohort.
   * @return isPaid
   */
  
  @JsonProperty("isPaid")
  public @Nullable Boolean getIsPaid() {
    return isPaid;
  }

  @JsonProperty("isPaid")
  public void setIsPaid(@Nullable Boolean isPaid) {
    this.isPaid = isPaid;
  }

  public Cohort price(@Nullable String price) {
    this.price = price;
    return this;
  }

  /**
   * Price.
   * @return price
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("price")
  public @Nullable String getPrice() {
    return price;
  }

  @JsonProperty("price")
  public void setPrice(@Nullable String price) {
    this.price = price;
  }

  public Cohort currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Price currency.
   * @return currencyId
   */
  @Valid 
  @JsonProperty("currencyId")
  public @Nullable UUID getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
  }

  public Cohort priceUsd(@Nullable String priceUsd) {
    this.priceUsd = priceUsd;
    return this;
  }

  /**
   * Equivalent USD price.
   * @return priceUsd
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("priceUsd")
  public @Nullable String getPriceUsd() {
    return priceUsd;
  }

  @JsonProperty("priceUsd")
  public void setPriceUsd(@Nullable String priceUsd) {
    this.priceUsd = priceUsd;
  }

  public Cohort showLiveClass(@Nullable Boolean showLiveClass) {
    this.showLiveClass = showLiveClass;
    return this;
  }

  /**
   * Show live classes tab.
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

  public Cohort evaluationEnabled(@Nullable Boolean evaluationEnabled) {
    this.evaluationEnabled = evaluationEnabled;
    return this;
  }

  /**
   * Cohort includes evaluations.
   * @return evaluationEnabled
   */
  
  @JsonProperty("evaluationEnabled")
  public @Nullable Boolean getEvaluationEnabled() {
    return evaluationEnabled;
  }

  @JsonProperty("evaluationEnabled")
  public void setEvaluationEnabled(@Nullable Boolean evaluationEnabled) {
    this.evaluationEnabled = evaluationEnabled;
  }

  public Cohort evaluationEndDate(@Nullable LocalDate evaluationEndDate) {
    this.evaluationEndDate = evaluationEndDate;
    return this;
  }

  /**
   * Last date for evaluations.
   * @return evaluationEndDate
   */
  @Valid 
  @JsonProperty("evaluationEndDate")
  public @Nullable LocalDate getEvaluationEndDate() {
    return evaluationEndDate;
  }

  @JsonProperty("evaluationEndDate")
  public void setEvaluationEndDate(@Nullable LocalDate evaluationEndDate) {
    this.evaluationEndDate = evaluationEndDate;
  }

  public Cohort certificationEnabled(@Nullable Boolean certificationEnabled) {
    this.certificationEnabled = certificationEnabled;
    return this;
  }

  /**
   * Certificates issued.
   * @return certificationEnabled
   */
  
  @JsonProperty("certificationEnabled")
  public @Nullable Boolean getCertificationEnabled() {
    return certificationEnabled;
  }

  @JsonProperty("certificationEnabled")
  public void setCertificationEnabled(@Nullable Boolean certificationEnabled) {
    this.certificationEnabled = certificationEnabled;
  }

  public Cohort conferencingProvider(@Nullable ConferencingProviderEnum conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
    return this;
  }

  /**
   * Live-class provider.
   * @return conferencingProvider
   */
  
  @JsonProperty("conferencingProvider")
  public @Nullable ConferencingProviderEnum getConferencingProvider() {
    return conferencingProvider;
  }

  @JsonProperty("conferencingProvider")
  public void setConferencingProvider(@Nullable ConferencingProviderEnum conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
  }

  public Cohort conferencingAccountId(@Nullable UUID conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
    return this;
  }

  /**
   * Live-class account.
   * @return conferencingAccountId
   */
  @Valid 
  @JsonProperty("conferencingAccountId")
  public @Nullable UUID getConferencingAccountId() {
    return conferencingAccountId;
  }

  @JsonProperty("conferencingAccountId")
  public void setConferencingAccountId(@Nullable UUID conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
  }

  public Cohort timetableTemplateId(@Nullable UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template the timetable was created from.
   * @return timetableTemplateId
   */
  @Valid 
  @JsonProperty("timetableTemplateId")
  public @Nullable UUID getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(@Nullable UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public Cohort confirmationTemplateId(@Nullable UUID confirmationTemplateId) {
    this.confirmationTemplateId = confirmationTemplateId;
    return this;
  }

  /**
   * Enrolment confirmation e-mail template.
   * @return confirmationTemplateId
   */
  @Valid 
  @JsonProperty("confirmationTemplateId")
  public @Nullable UUID getConfirmationTemplateId() {
    return confirmationTemplateId;
  }

  @JsonProperty("confirmationTemplateId")
  public void setConfirmationTemplateId(@Nullable UUID confirmationTemplateId) {
    this.confirmationTemplateId = confirmationTemplateId;
  }

  public Cohort metaImageFileId(@Nullable UUID metaImageFileId) {
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

  public Cohort introVideoFileId(@Nullable UUID introVideoFileId) {
    this.introVideoFileId = introVideoFileId;
    return this;
  }

  /**
   * Introductory video.
   * @return introVideoFileId
   */
  @Valid 
  @JsonProperty("introVideoFileId")
  public @Nullable UUID getIntroVideoFileId() {
    return introVideoFileId;
  }

  @JsonProperty("introVideoFileId")
  public void setIntroVideoFileId(@Nullable UUID introVideoFileId) {
    this.introVideoFileId = introVideoFileId;
  }

  public Cohort customComponent(@Nullable String customComponent) {
    this.customComponent = customComponent;
    return this;
  }

  /**
   * Custom UI component markup.
   * @return customComponent
   */
  
  @JsonProperty("customComponent")
  public @Nullable String getCustomComponent() {
    return customComponent;
  }

  @JsonProperty("customComponent")
  public void setCustomComponent(@Nullable String customComponent) {
    this.customComponent = customComponent;
  }

  public Cohort customScript(@Nullable String customScript) {
    this.customScript = customScript;
    return this;
  }

  /**
   * Custom script.
   * @return customScript
   */
  
  @JsonProperty("customScript")
  public @Nullable String getCustomScript() {
    return customScript;
  }

  @JsonProperty("customScript")
  public void setCustomScript(@Nullable String customScript) {
    this.customScript = customScript;
  }

  public Cohort publishNotificationSent(@Nullable Boolean publishNotificationSent) {
    this.publishNotificationSent = publishNotificationSent;
    return this;
  }

  /**
   * Publication notification sent.
   * @return publishNotificationSent
   */
  
  @JsonProperty("publishNotificationSent")
  public @Nullable Boolean getPublishNotificationSent() {
    return publishNotificationSent;
  }

  @JsonProperty("publishNotificationSent")
  public void setPublishNotificationSent(@Nullable Boolean publishNotificationSent) {
    this.publishNotificationSent = publishNotificationSent;
  }

  public Cohort code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Cohort (class / section) code.
   * @return code
   */
  @Size(max = 50) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public Cohort academicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Academic term the cohort runs in.
   * @return academicTermId
   */
  @Valid 
  @JsonProperty("academicTermId")
  public @Nullable UUID getAcademicTermId() {
    return academicTermId;
  }

  @JsonProperty("academicTermId")
  public void setAcademicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
  }

  public Cohort programId(@Nullable UUID programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program the cohort belongs to.
   * @return programId
   */
  @Valid 
  @JsonProperty("programId")
  public @Nullable UUID getProgramId() {
    return programId;
  }

  @JsonProperty("programId")
  public void setProgramId(@Nullable UUID programId) {
    this.programId = programId;
  }

  public Cohort externalRef(@Nullable String externalRef) {
    this.externalRef = externalRef;
    return this;
  }

  /**
   * External system reference (SIS integration key).
   * @return externalRef
   */
  @Size(max = 100) 
  @JsonProperty("externalRef")
  public @Nullable String getExternalRef() {
    return externalRef;
  }

  @JsonProperty("externalRef")
  public void setExternalRef(@Nullable String externalRef) {
    this.externalRef = externalRef;
  }

  public Cohort lifecycleStatus(@Nullable LifecycleStatusEnum lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
    return this;
  }

  /**
   * Delivery state (suspended = temporarily closed to members).
   * @return lifecycleStatus
   */
  
  @JsonProperty("lifecycleStatus")
  public @Nullable LifecycleStatusEnum getLifecycleStatus() {
    return lifecycleStatus;
  }

  @JsonProperty("lifecycleStatus")
  public void setLifecycleStatus(@Nullable LifecycleStatusEnum lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
  }

  public Cohort staffAccessFrom(@Nullable OffsetDateTime staffAccessFrom) {
    this.staffAccessFrom = staffAccessFrom;
    return this;
  }

  /**
   * Staff can enter from.
   * @return staffAccessFrom
   */
  @Valid 
  @JsonProperty("staffAccessFrom")
  public @Nullable OffsetDateTime getStaffAccessFrom() {
    return staffAccessFrom;
  }

  @JsonProperty("staffAccessFrom")
  public void setStaffAccessFrom(@Nullable OffsetDateTime staffAccessFrom) {
    this.staffAccessFrom = staffAccessFrom;
  }

  public Cohort staffAccessUntil(@Nullable OffsetDateTime staffAccessUntil) {
    this.staffAccessUntil = staffAccessUntil;
    return this;
  }

  /**
   * Staff access ends.
   * @return staffAccessUntil
   */
  @Valid 
  @JsonProperty("staffAccessUntil")
  public @Nullable OffsetDateTime getStaffAccessUntil() {
    return staffAccessUntil;
  }

  @JsonProperty("staffAccessUntil")
  public void setStaffAccessUntil(@Nullable OffsetDateTime staffAccessUntil) {
    this.staffAccessUntil = staffAccessUntil;
  }

  public Cohort studentAccessFrom(@Nullable OffsetDateTime studentAccessFrom) {
    this.studentAccessFrom = studentAccessFrom;
    return this;
  }

  /**
   * Students can enter from.
   * @return studentAccessFrom
   */
  @Valid 
  @JsonProperty("studentAccessFrom")
  public @Nullable OffsetDateTime getStudentAccessFrom() {
    return studentAccessFrom;
  }

  @JsonProperty("studentAccessFrom")
  public void setStudentAccessFrom(@Nullable OffsetDateTime studentAccessFrom) {
    this.studentAccessFrom = studentAccessFrom;
  }

  public Cohort studentAccessUntil(@Nullable OffsetDateTime studentAccessUntil) {
    this.studentAccessUntil = studentAccessUntil;
    return this;
  }

  /**
   * Student access ends (read-only after the end date).
   * @return studentAccessUntil
   */
  @Valid 
  @JsonProperty("studentAccessUntil")
  public @Nullable OffsetDateTime getStudentAccessUntil() {
    return studentAccessUntil;
  }

  @JsonProperty("studentAccessUntil")
  public void setStudentAccessUntil(@Nullable OffsetDateTime studentAccessUntil) {
    this.studentAccessUntil = studentAccessUntil;
  }

  public Cohort hasSchedule(@Nullable Boolean hasSchedule) {
    this.hasSchedule = hasSchedule;
    return this;
  }

  /**
   * Cohort meets on a weekly schedule.
   * @return hasSchedule
   */
  
  @JsonProperty("hasSchedule")
  public @Nullable Boolean getHasSchedule() {
    return hasSchedule;
  }

  @JsonProperty("hasSchedule")
  public void setHasSchedule(@Nullable Boolean hasSchedule) {
    this.hasSchedule = hasSchedule;
  }

  public Cohort staffCanEditSchedule(@Nullable Boolean staffCanEditSchedule) {
    this.staffCanEditSchedule = staffCanEditSchedule;
    return this;
  }

  /**
   * Cohort staff may change the schedule.
   * @return staffCanEditSchedule
   */
  
  @JsonProperty("staffCanEditSchedule")
  public @Nullable Boolean getStaffCanEditSchedule() {
    return staffCanEditSchedule;
  }

  @JsonProperty("staffCanEditSchedule")
  public void setStaffCanEditSchedule(@Nullable Boolean staffCanEditSchedule) {
    this.staffCanEditSchedule = staffCanEditSchedule;
  }

  public Cohort gradeDisplay(@Nullable GradeDisplayEnum gradeDisplay) {
    this.gradeDisplay = gradeDisplay;
    return this;
  }

  /**
   * Gradebook display mode.
   * @return gradeDisplay
   */
  
  @JsonProperty("gradeDisplay")
  public @Nullable GradeDisplayEnum getGradeDisplay() {
    return gradeDisplay;
  }

  @JsonProperty("gradeDisplay")
  public void setGradeDisplay(@Nullable GradeDisplayEnum gradeDisplay) {
    this.gradeDisplay = gradeDisplay;
  }

  public Cohort gradingSchemeId(@Nullable UUID gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
    return this;
  }

  /**
   * Letter-grade scale.
   * @return gradingSchemeId
   */
  @Valid 
  @JsonProperty("gradingSchemeId")
  public @Nullable UUID getGradingSchemeId() {
    return gradingSchemeId;
  }

  @JsonProperty("gradingSchemeId")
  public void setGradingSchemeId(@Nullable UUID gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
  }

  public Cohort enrollmentOpensAt(@Nullable OffsetDateTime enrollmentOpensAt) {
    this.enrollmentOpensAt = enrollmentOpensAt;
    return this;
  }

  /**
   * Catalogue enrolment opens.
   * @return enrollmentOpensAt
   */
  @Valid 
  @JsonProperty("enrollmentOpensAt")
  public @Nullable OffsetDateTime getEnrollmentOpensAt() {
    return enrollmentOpensAt;
  }

  @JsonProperty("enrollmentOpensAt")
  public void setEnrollmentOpensAt(@Nullable OffsetDateTime enrollmentOpensAt) {
    this.enrollmentOpensAt = enrollmentOpensAt;
  }

  public Cohort enrollmentClosesAt(@Nullable OffsetDateTime enrollmentClosesAt) {
    this.enrollmentClosesAt = enrollmentClosesAt;
    return this;
  }

  /**
   * Catalogue enrolment closes.
   * @return enrollmentClosesAt
   */
  @Valid 
  @JsonProperty("enrollmentClosesAt")
  public @Nullable OffsetDateTime getEnrollmentClosesAt() {
    return enrollmentClosesAt;
  }

  @JsonProperty("enrollmentClosesAt")
  public void setEnrollmentClosesAt(@Nullable OffsetDateTime enrollmentClosesAt) {
    this.enrollmentClosesAt = enrollmentClosesAt;
  }

  public Cohort createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Cohort createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Cohort updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Cohort updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Cohort etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Cohort cohort = (Cohort) o;
    return Objects.equals(this.classSubjectId, cohort.classSubjectId) &&
        Objects.equals(this.batchId, cohort.batchId) &&
        Objects.equals(this.academicYearId, cohort.academicYearId) &&
        Objects.equals(this.id, cohort.id) &&
        Objects.equals(this.institutionId, cohort.institutionId) &&
        Objects.equals(this.title, cohort.title) &&
        Objects.equals(this.description, cohort.description) &&
        Objects.equals(this.details, cohort.details) &&
        Objects.equals(this.categoryId, cohort.categoryId) &&
        Objects.equals(this.medium, cohort.medium) &&
        Objects.equals(this.startDate, cohort.startDate) &&
        Objects.equals(this.endDate, cohort.endDate) &&
        Objects.equals(this.startTime, cohort.startTime) &&
        Objects.equals(this.endTime, cohort.endTime) &&
        Objects.equals(this.timezone, cohort.timezone) &&
        Objects.equals(this.seatCount, cohort.seatCount) &&
        Objects.equals(this.isPublished, cohort.isPublished) &&
        Objects.equals(this.allowSelfEnrollment, cohort.allowSelfEnrollment) &&
        Objects.equals(this.allowFutureAccess, cohort.allowFutureAccess) &&
        Objects.equals(this.isPaid, cohort.isPaid) &&
        Objects.equals(this.price, cohort.price) &&
        Objects.equals(this.currencyId, cohort.currencyId) &&
        Objects.equals(this.priceUsd, cohort.priceUsd) &&
        Objects.equals(this.showLiveClass, cohort.showLiveClass) &&
        Objects.equals(this.evaluationEnabled, cohort.evaluationEnabled) &&
        Objects.equals(this.evaluationEndDate, cohort.evaluationEndDate) &&
        Objects.equals(this.certificationEnabled, cohort.certificationEnabled) &&
        Objects.equals(this.conferencingProvider, cohort.conferencingProvider) &&
        Objects.equals(this.conferencingAccountId, cohort.conferencingAccountId) &&
        Objects.equals(this.timetableTemplateId, cohort.timetableTemplateId) &&
        Objects.equals(this.confirmationTemplateId, cohort.confirmationTemplateId) &&
        Objects.equals(this.metaImageFileId, cohort.metaImageFileId) &&
        Objects.equals(this.introVideoFileId, cohort.introVideoFileId) &&
        Objects.equals(this.customComponent, cohort.customComponent) &&
        Objects.equals(this.customScript, cohort.customScript) &&
        Objects.equals(this.publishNotificationSent, cohort.publishNotificationSent) &&
        Objects.equals(this.code, cohort.code) &&
        Objects.equals(this.academicTermId, cohort.academicTermId) &&
        Objects.equals(this.programId, cohort.programId) &&
        Objects.equals(this.externalRef, cohort.externalRef) &&
        Objects.equals(this.lifecycleStatus, cohort.lifecycleStatus) &&
        Objects.equals(this.staffAccessFrom, cohort.staffAccessFrom) &&
        Objects.equals(this.staffAccessUntil, cohort.staffAccessUntil) &&
        Objects.equals(this.studentAccessFrom, cohort.studentAccessFrom) &&
        Objects.equals(this.studentAccessUntil, cohort.studentAccessUntil) &&
        Objects.equals(this.hasSchedule, cohort.hasSchedule) &&
        Objects.equals(this.staffCanEditSchedule, cohort.staffCanEditSchedule) &&
        Objects.equals(this.gradeDisplay, cohort.gradeDisplay) &&
        Objects.equals(this.gradingSchemeId, cohort.gradingSchemeId) &&
        Objects.equals(this.enrollmentOpensAt, cohort.enrollmentOpensAt) &&
        Objects.equals(this.enrollmentClosesAt, cohort.enrollmentClosesAt) &&
        Objects.equals(this.createTime, cohort.createTime) &&
        Objects.equals(this.createdBy, cohort.createdBy) &&
        Objects.equals(this.updateTime, cohort.updateTime) &&
        Objects.equals(this.updatedBy, cohort.updatedBy) &&
        Objects.equals(this.etag, cohort.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classSubjectId, batchId, academicYearId, id, institutionId, title, description, details, categoryId, medium, startDate, endDate, startTime, endTime, timezone, seatCount, isPublished, allowSelfEnrollment, allowFutureAccess, isPaid, price, currencyId, priceUsd, showLiveClass, evaluationEnabled, evaluationEndDate, certificationEnabled, conferencingProvider, conferencingAccountId, timetableTemplateId, confirmationTemplateId, metaImageFileId, introVideoFileId, customComponent, customScript, publishNotificationSent, code, academicTermId, programId, externalRef, lifecycleStatus, staffAccessFrom, staffAccessUntil, studentAccessFrom, studentAccessUntil, hasSchedule, staffCanEditSchedule, gradeDisplay, gradingSchemeId, enrollmentOpensAt, enrollmentClosesAt, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Cohort {\n");
    sb.append("    classSubjectId: ").append(toIndentedString(classSubjectId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    sb.append("    isPublished: ").append(toIndentedString(isPublished)).append("\n");
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
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

