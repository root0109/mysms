// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of LmsCourse that can be updated (partial update; see updateMask).
 */

public class LmsCourseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String shortIntroduction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String videoUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID categoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate publishedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isUpcoming;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isFeatured;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean disableSelfLearning;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enforceLessonOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean certificationEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean paidCertificate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultEvaluatorId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPaid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String priceUsd;

  /**
   * Catalogue card colour.
   */
  public enum CardGradientEnum {
    RED("red"),
    
    BLUE("blue"),
    
    GREEN("green"),
    
    AMBER("amber"),
    
    CYAN("cyan"),
    
    ORANGE("orange"),
    
    PINK("pink"),
    
    PURPLE("purple"),
    
    TEAL("teal"),
    
    VIOLET("violet"),
    
    YELLOW("yellow"),
    
    GRAY("gray");

    private final String value;

    CardGradientEnum(String value) {
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
    public static CardGradientEnum fromValue(String value) {
      for (CardGradientEnum b : CardGradientEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable CardGradientEnum cardGradient;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean publishNotificationSent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer enrollmentCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer lessonCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String averageRating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String stateStandardCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String credits;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String prerequisites;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String corequisites;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate effectiveFrom;

  /**
   * Version lifecycle (separate from the authoring review status).
   */
  public enum LifecycleStatusEnum {
    DRAFT("draft"),
    
    PUBLISHED("published"),
    
    DEACTIVATED("deactivated");

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
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate deactivatedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer versionMajor;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer versionMinor;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID previousVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID rootCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String authorComments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String externalRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer nominalDuration;

  /**
   * Delivery modality.
   */
  public enum DeliveryModalityEnum {
    TRADITIONAL("traditional"),
    
    ONLINE("online"),
    
    HYBRID("hybrid");

    private final String value;

    DeliveryModalityEnum(String value) {
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
    public static DeliveryModalityEnum fromValue(String value) {
      for (DeliveryModalityEnum b : DeliveryModalityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DeliveryModalityEnum deliveryModality;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isShared;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime contentUpdatedAt;

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
  private @Nullable Boolean isInCatalog;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String catalogDescription;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime catalogVisibleFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime catalogVisibleUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String outline;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer workloadHours;

  /**
   * Catalogue listing state.
   */
  public enum CatalogStatusEnum {
    DRAFT("draft"),
    
    PUBLISHED("published"),
    
    ARCHIVED("archived");

    private final String value;

    CatalogStatusEnum(String value) {
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
    public static CatalogStatusEnum fromValue(String value) {
      for (CatalogStatusEnum b : CatalogStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable CatalogStatusEnum catalogStatus;

  public LmsCourseUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution (NULL = tenant-wide catalogue).
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

  public LmsCourseUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Course title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public LmsCourseUpdate shortIntroduction(@Nullable String shortIntroduction) {
    this.shortIntroduction = shortIntroduction;
    return this;
  }

  /**
   * Short introduction.
   * @return shortIntroduction
   */
  
  @JsonProperty("shortIntroduction")
  public @Nullable String getShortIntroduction() {
    return shortIntroduction;
  }

  @JsonProperty("shortIntroduction")
  public void setShortIntroduction(@Nullable String shortIntroduction) {
    this.shortIntroduction = shortIntroduction;
  }

  public LmsCourseUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Full description.
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

  public LmsCourseUpdate imageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Cover image.
   * @return imageFileId
   */
  @Valid 
  @JsonProperty("imageFileId")
  public @Nullable UUID getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
  }

  public LmsCourseUpdate videoUrl(@Nullable String videoUrl) {
    this.videoUrl = videoUrl;
    return this;
  }

  /**
   * Promo video URL.
   * @return videoUrl
   */
  @Size(max = 500) 
  @JsonProperty("videoUrl")
  public @Nullable String getVideoUrl() {
    return videoUrl;
  }

  @JsonProperty("videoUrl")
  public void setVideoUrl(@Nullable String videoUrl) {
    this.videoUrl = videoUrl;
  }

  public LmsCourseUpdate categoryId(@Nullable UUID categoryId) {
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

  public LmsCourseUpdate publishedOn(@Nullable LocalDate publishedOn) {
    this.publishedOn = publishedOn;
    return this;
  }

  /**
   * Publication date.
   * @return publishedOn
   */
  @Valid 
  @JsonProperty("publishedOn")
  public @Nullable LocalDate getPublishedOn() {
    return publishedOn;
  }

  @JsonProperty("publishedOn")
  public void setPublishedOn(@Nullable LocalDate publishedOn) {
    this.publishedOn = publishedOn;
  }

  public LmsCourseUpdate isUpcoming(@Nullable Boolean isUpcoming) {
    this.isUpcoming = isUpcoming;
    return this;
  }

  /**
   * Announced but not yet open.
   * @return isUpcoming
   */
  
  @JsonProperty("isUpcoming")
  public @Nullable Boolean getIsUpcoming() {
    return isUpcoming;
  }

  @JsonProperty("isUpcoming")
  public void setIsUpcoming(@Nullable Boolean isUpcoming) {
    this.isUpcoming = isUpcoming;
  }

  public LmsCourseUpdate isFeatured(@Nullable Boolean isFeatured) {
    this.isFeatured = isFeatured;
    return this;
  }

  /**
   * Featured in the catalogue.
   * @return isFeatured
   */
  
  @JsonProperty("isFeatured")
  public @Nullable Boolean getIsFeatured() {
    return isFeatured;
  }

  @JsonProperty("isFeatured")
  public void setIsFeatured(@Nullable Boolean isFeatured) {
    this.isFeatured = isFeatured;
  }

  public LmsCourseUpdate disableSelfLearning(@Nullable Boolean disableSelfLearning) {
    this.disableSelfLearning = disableSelfLearning;
    return this;
  }

  /**
   * Only available through cohorts.
   * @return disableSelfLearning
   */
  
  @JsonProperty("disableSelfLearning")
  public @Nullable Boolean getDisableSelfLearning() {
    return disableSelfLearning;
  }

  @JsonProperty("disableSelfLearning")
  public void setDisableSelfLearning(@Nullable Boolean disableSelfLearning) {
    this.disableSelfLearning = disableSelfLearning;
  }

  public LmsCourseUpdate enforceLessonOrder(@Nullable Boolean enforceLessonOrder) {
    this.enforceLessonOrder = enforceLessonOrder;
    return this;
  }

  /**
   * Lessons must be completed in order.
   * @return enforceLessonOrder
   */
  
  @JsonProperty("enforceLessonOrder")
  public @Nullable Boolean getEnforceLessonOrder() {
    return enforceLessonOrder;
  }

  @JsonProperty("enforceLessonOrder")
  public void setEnforceLessonOrder(@Nullable Boolean enforceLessonOrder) {
    this.enforceLessonOrder = enforceLessonOrder;
  }

  public LmsCourseUpdate certificationEnabled(@Nullable Boolean certificationEnabled) {
    this.certificationEnabled = certificationEnabled;
    return this;
  }

  /**
   * Certificate issued on completion.
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

  public LmsCourseUpdate paidCertificate(@Nullable Boolean paidCertificate) {
    this.paidCertificate = paidCertificate;
    return this;
  }

  /**
   * Certificate requires payment / evaluation.
   * @return paidCertificate
   */
  
  @JsonProperty("paidCertificate")
  public @Nullable Boolean getPaidCertificate() {
    return paidCertificate;
  }

  @JsonProperty("paidCertificate")
  public void setPaidCertificate(@Nullable Boolean paidCertificate) {
    this.paidCertificate = paidCertificate;
  }

  public LmsCourseUpdate defaultEvaluatorId(@Nullable UUID defaultEvaluatorId) {
    this.defaultEvaluatorId = defaultEvaluatorId;
    return this;
  }

  /**
   * Default evaluator.
   * @return defaultEvaluatorId
   */
  @Valid 
  @JsonProperty("defaultEvaluatorId")
  public @Nullable UUID getDefaultEvaluatorId() {
    return defaultEvaluatorId;
  }

  @JsonProperty("defaultEvaluatorId")
  public void setDefaultEvaluatorId(@Nullable UUID defaultEvaluatorId) {
    this.defaultEvaluatorId = defaultEvaluatorId;
  }

  public LmsCourseUpdate isPaid(@Nullable Boolean isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Paid course.
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

  public LmsCourseUpdate price(@Nullable String price) {
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

  public LmsCourseUpdate currencyId(@Nullable UUID currencyId) {
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

  public LmsCourseUpdate priceUsd(@Nullable String priceUsd) {
    this.priceUsd = priceUsd;
    return this;
  }

  /**
   * Equivalent USD price for international buyers.
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

  public LmsCourseUpdate cardGradient(@Nullable CardGradientEnum cardGradient) {
    this.cardGradient = cardGradient;
    return this;
  }

  /**
   * Catalogue card colour.
   * @return cardGradient
   */
  
  @JsonProperty("cardGradient")
  public @Nullable CardGradientEnum getCardGradient() {
    return cardGradient;
  }

  @JsonProperty("cardGradient")
  public void setCardGradient(@Nullable CardGradientEnum cardGradient) {
    this.cardGradient = cardGradient;
  }

  public LmsCourseUpdate timezone(@Nullable String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone for scheduled items.
   * @return timezone
   */
  @Size(max = 64) 
  @JsonProperty("timezone")
  public @Nullable String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable String timezone) {
    this.timezone = timezone;
  }

  public LmsCourseUpdate publishNotificationSent(@Nullable Boolean publishNotificationSent) {
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

  public LmsCourseUpdate enrollmentCount(@Nullable Integer enrollmentCount) {
    this.enrollmentCount = enrollmentCount;
    return this;
  }

  /**
   * Cached enrolment count.
   * @return enrollmentCount
   */
  
  @JsonProperty("enrollmentCount")
  public @Nullable Integer getEnrollmentCount() {
    return enrollmentCount;
  }

  @JsonProperty("enrollmentCount")
  public void setEnrollmentCount(@Nullable Integer enrollmentCount) {
    this.enrollmentCount = enrollmentCount;
  }

  public LmsCourseUpdate lessonCount(@Nullable Integer lessonCount) {
    this.lessonCount = lessonCount;
    return this;
  }

  /**
   * Cached lesson count.
   * @return lessonCount
   */
  
  @JsonProperty("lessonCount")
  public @Nullable Integer getLessonCount() {
    return lessonCount;
  }

  @JsonProperty("lessonCount")
  public void setLessonCount(@Nullable Integer lessonCount) {
    this.lessonCount = lessonCount;
  }

  public LmsCourseUpdate averageRating(@Nullable String averageRating) {
    this.averageRating = averageRating;
    return this;
  }

  /**
   * Cached average review rating (0-5).
   * @return averageRating
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("averageRating")
  public @Nullable String getAverageRating() {
    return averageRating;
  }

  @JsonProperty("averageRating")
  public void setAverageRating(@Nullable String averageRating) {
    this.averageRating = averageRating;
  }

  public LmsCourseUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Course code (unique per version line).
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

  public LmsCourseUpdate stateStandardCode(@Nullable String stateStandardCode) {
    this.stateStandardCode = stateStandardCode;
    return this;
  }

  /**
   * Government / state curriculum standard code.
   * @return stateStandardCode
   */
  @Size(max = 100) 
  @JsonProperty("stateStandardCode")
  public @Nullable String getStateStandardCode() {
    return stateStandardCode;
  }

  @JsonProperty("stateStandardCode")
  public void setStateStandardCode(@Nullable String stateStandardCode) {
    this.stateStandardCode = stateStandardCode;
  }

  public LmsCourseUpdate credits(@Nullable String credits) {
    this.credits = credits;
    return this;
  }

  /**
   * Credit value.
   * @return credits
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("credits")
  public @Nullable String getCredits() {
    return credits;
  }

  @JsonProperty("credits")
  public void setCredits(@Nullable String credits) {
    this.credits = credits;
  }

  public LmsCourseUpdate prerequisites(@Nullable String prerequisites) {
    this.prerequisites = prerequisites;
    return this;
  }

  /**
   * Prerequisites.
   * @return prerequisites
   */
  
  @JsonProperty("prerequisites")
  public @Nullable String getPrerequisites() {
    return prerequisites;
  }

  @JsonProperty("prerequisites")
  public void setPrerequisites(@Nullable String prerequisites) {
    this.prerequisites = prerequisites;
  }

  public LmsCourseUpdate corequisites(@Nullable String corequisites) {
    this.corequisites = corequisites;
    return this;
  }

  /**
   * Co-requisites.
   * @return corequisites
   */
  
  @JsonProperty("corequisites")
  public @Nullable String getCorequisites() {
    return corequisites;
  }

  @JsonProperty("corequisites")
  public void setCorequisites(@Nullable String corequisites) {
    this.corequisites = corequisites;
  }

  public LmsCourseUpdate effectiveFrom(@Nullable LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
    return this;
  }

  /**
   * Version effective date.
   * @return effectiveFrom
   */
  @Valid 
  @JsonProperty("effectiveFrom")
  public @Nullable LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  @JsonProperty("effectiveFrom")
  public void setEffectiveFrom(@Nullable LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LmsCourseUpdate lifecycleStatus(@Nullable LifecycleStatusEnum lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
    return this;
  }

  /**
   * Version lifecycle (separate from the authoring review status).
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

  public LmsCourseUpdate deactivatedOn(@Nullable LocalDate deactivatedOn) {
    this.deactivatedOn = deactivatedOn;
    return this;
  }

  /**
   * Deactivation date.
   * @return deactivatedOn
   */
  @Valid 
  @JsonProperty("deactivatedOn")
  public @Nullable LocalDate getDeactivatedOn() {
    return deactivatedOn;
  }

  @JsonProperty("deactivatedOn")
  public void setDeactivatedOn(@Nullable LocalDate deactivatedOn) {
    this.deactivatedOn = deactivatedOn;
  }

  public LmsCourseUpdate versionMajor(@Nullable Integer versionMajor) {
    this.versionMajor = versionMajor;
    return this;
  }

  /**
   * Major version number.
   * @return versionMajor
   */
  
  @JsonProperty("versionMajor")
  public @Nullable Integer getVersionMajor() {
    return versionMajor;
  }

  @JsonProperty("versionMajor")
  public void setVersionMajor(@Nullable Integer versionMajor) {
    this.versionMajor = versionMajor;
  }

  public LmsCourseUpdate versionMinor(@Nullable Integer versionMinor) {
    this.versionMinor = versionMinor;
    return this;
  }

  /**
   * Minor version number.
   * @return versionMinor
   */
  
  @JsonProperty("versionMinor")
  public @Nullable Integer getVersionMinor() {
    return versionMinor;
  }

  @JsonProperty("versionMinor")
  public void setVersionMinor(@Nullable Integer versionMinor) {
    this.versionMinor = versionMinor;
  }

  public LmsCourseUpdate previousVersionId(@Nullable UUID previousVersionId) {
    this.previousVersionId = previousVersionId;
    return this;
  }

  /**
   * Version this one was derived from.
   * @return previousVersionId
   */
  @Valid 
  @JsonProperty("previousVersionId")
  public @Nullable UUID getPreviousVersionId() {
    return previousVersionId;
  }

  @JsonProperty("previousVersionId")
  public void setPreviousVersionId(@Nullable UUID previousVersionId) {
    this.previousVersionId = previousVersionId;
  }

  public LmsCourseUpdate rootCourseId(@Nullable UUID rootCourseId) {
    this.rootCourseId = rootCourseId;
    return this;
  }

  /**
   * First version of the course line.
   * @return rootCourseId
   */
  @Valid 
  @JsonProperty("rootCourseId")
  public @Nullable UUID getRootCourseId() {
    return rootCourseId;
  }

  @JsonProperty("rootCourseId")
  public void setRootCourseId(@Nullable UUID rootCourseId) {
    this.rootCourseId = rootCourseId;
  }

  public LmsCourseUpdate authorComments(@Nullable String authorComments) {
    this.authorComments = authorComments;
    return this;
  }

  /**
   * Author's notes on this version.
   * @return authorComments
   */
  
  @JsonProperty("authorComments")
  public @Nullable String getAuthorComments() {
    return authorComments;
  }

  @JsonProperty("authorComments")
  public void setAuthorComments(@Nullable String authorComments) {
    this.authorComments = authorComments;
  }

  public LmsCourseUpdate externalRef(@Nullable String externalRef) {
    this.externalRef = externalRef;
    return this;
  }

  /**
   * External system reference (catalogue / SIS integration key).
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

  public LmsCourseUpdate nominalDuration(@Nullable Integer nominalDuration) {
    this.nominalDuration = nominalDuration;
    return this;
  }

  /**
   * Nominal duration of the course (in the institution's planning unit, normally weeks).
   * @return nominalDuration
   */
  
  @JsonProperty("nominalDuration")
  public @Nullable Integer getNominalDuration() {
    return nominalDuration;
  }

  @JsonProperty("nominalDuration")
  public void setNominalDuration(@Nullable Integer nominalDuration) {
    this.nominalDuration = nominalDuration;
  }

  public LmsCourseUpdate deliveryModality(@Nullable DeliveryModalityEnum deliveryModality) {
    this.deliveryModality = deliveryModality;
    return this;
  }

  /**
   * Delivery modality.
   * @return deliveryModality
   */
  
  @JsonProperty("deliveryModality")
  public @Nullable DeliveryModalityEnum getDeliveryModality() {
    return deliveryModality;
  }

  @JsonProperty("deliveryModality")
  public void setDeliveryModality(@Nullable DeliveryModalityEnum deliveryModality) {
    this.deliveryModality = deliveryModality;
  }

  public LmsCourseUpdate isShared(@Nullable Boolean isShared) {
    this.isShared = isShared;
    return this;
  }

  /**
   * Shared with every institution of the tenant.
   * @return isShared
   */
  
  @JsonProperty("isShared")
  public @Nullable Boolean getIsShared() {
    return isShared;
  }

  @JsonProperty("isShared")
  public void setIsShared(@Nullable Boolean isShared) {
    this.isShared = isShared;
  }

  public LmsCourseUpdate contentUpdatedAt(@Nullable OffsetDateTime contentUpdatedAt) {
    this.contentUpdatedAt = contentUpdatedAt;
    return this;
  }

  /**
   * Last change to the course content (drives cohort update prompts).
   * @return contentUpdatedAt
   */
  @Valid 
  @JsonProperty("contentUpdatedAt")
  public @Nullable OffsetDateTime getContentUpdatedAt() {
    return contentUpdatedAt;
  }

  @JsonProperty("contentUpdatedAt")
  public void setContentUpdatedAt(@Nullable OffsetDateTime contentUpdatedAt) {
    this.contentUpdatedAt = contentUpdatedAt;
  }

  public LmsCourseUpdate gradeDisplay(@Nullable GradeDisplayEnum gradeDisplay) {
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

  public LmsCourseUpdate gradingSchemeId(@Nullable UUID gradingSchemeId) {
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

  public LmsCourseUpdate isInCatalog(@Nullable Boolean isInCatalog) {
    this.isInCatalog = isInCatalog;
    return this;
  }

  /**
   * Listed in the course catalogue.
   * @return isInCatalog
   */
  
  @JsonProperty("isInCatalog")
  public @Nullable Boolean getIsInCatalog() {
    return isInCatalog;
  }

  @JsonProperty("isInCatalog")
  public void setIsInCatalog(@Nullable Boolean isInCatalog) {
    this.isInCatalog = isInCatalog;
  }

  public LmsCourseUpdate catalogDescription(@Nullable String catalogDescription) {
    this.catalogDescription = catalogDescription;
    return this;
  }

  /**
   * Catalogue description (marketing copy).
   * @return catalogDescription
   */
  
  @JsonProperty("catalogDescription")
  public @Nullable String getCatalogDescription() {
    return catalogDescription;
  }

  @JsonProperty("catalogDescription")
  public void setCatalogDescription(@Nullable String catalogDescription) {
    this.catalogDescription = catalogDescription;
  }

  public LmsCourseUpdate catalogVisibleFrom(@Nullable OffsetDateTime catalogVisibleFrom) {
    this.catalogVisibleFrom = catalogVisibleFrom;
    return this;
  }

  /**
   * Catalogue listing visible from.
   * @return catalogVisibleFrom
   */
  @Valid 
  @JsonProperty("catalogVisibleFrom")
  public @Nullable OffsetDateTime getCatalogVisibleFrom() {
    return catalogVisibleFrom;
  }

  @JsonProperty("catalogVisibleFrom")
  public void setCatalogVisibleFrom(@Nullable OffsetDateTime catalogVisibleFrom) {
    this.catalogVisibleFrom = catalogVisibleFrom;
  }

  public LmsCourseUpdate catalogVisibleUntil(@Nullable OffsetDateTime catalogVisibleUntil) {
    this.catalogVisibleUntil = catalogVisibleUntil;
    return this;
  }

  /**
   * Catalogue listing visible until.
   * @return catalogVisibleUntil
   */
  @Valid 
  @JsonProperty("catalogVisibleUntil")
  public @Nullable OffsetDateTime getCatalogVisibleUntil() {
    return catalogVisibleUntil;
  }

  @JsonProperty("catalogVisibleUntil")
  public void setCatalogVisibleUntil(@Nullable OffsetDateTime catalogVisibleUntil) {
    this.catalogVisibleUntil = catalogVisibleUntil;
  }

  public LmsCourseUpdate outline(@Nullable String outline) {
    this.outline = outline;
    return this;
  }

  /**
   * Course outline shown in the catalogue.
   * @return outline
   */
  
  @JsonProperty("outline")
  public @Nullable String getOutline() {
    return outline;
  }

  @JsonProperty("outline")
  public void setOutline(@Nullable String outline) {
    this.outline = outline;
  }

  public LmsCourseUpdate workloadHours(@Nullable Integer workloadHours) {
    this.workloadHours = workloadHours;
    return this;
  }

  /**
   * Expected student workload in hours.
   * @return workloadHours
   */
  
  @JsonProperty("workloadHours")
  public @Nullable Integer getWorkloadHours() {
    return workloadHours;
  }

  @JsonProperty("workloadHours")
  public void setWorkloadHours(@Nullable Integer workloadHours) {
    this.workloadHours = workloadHours;
  }

  public LmsCourseUpdate catalogStatus(@Nullable CatalogStatusEnum catalogStatus) {
    this.catalogStatus = catalogStatus;
    return this;
  }

  /**
   * Catalogue listing state.
   * @return catalogStatus
   */
  
  @JsonProperty("catalogStatus")
  public @Nullable CatalogStatusEnum getCatalogStatus() {
    return catalogStatus;
  }

  @JsonProperty("catalogStatus")
  public void setCatalogStatus(@Nullable CatalogStatusEnum catalogStatus) {
    this.catalogStatus = catalogStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LmsCourseUpdate lmsCourseUpdate = (LmsCourseUpdate) o;
    return Objects.equals(this.institutionId, lmsCourseUpdate.institutionId) &&
        Objects.equals(this.title, lmsCourseUpdate.title) &&
        Objects.equals(this.shortIntroduction, lmsCourseUpdate.shortIntroduction) &&
        Objects.equals(this.description, lmsCourseUpdate.description) &&
        Objects.equals(this.imageFileId, lmsCourseUpdate.imageFileId) &&
        Objects.equals(this.videoUrl, lmsCourseUpdate.videoUrl) &&
        Objects.equals(this.categoryId, lmsCourseUpdate.categoryId) &&
        Objects.equals(this.publishedOn, lmsCourseUpdate.publishedOn) &&
        Objects.equals(this.isUpcoming, lmsCourseUpdate.isUpcoming) &&
        Objects.equals(this.isFeatured, lmsCourseUpdate.isFeatured) &&
        Objects.equals(this.disableSelfLearning, lmsCourseUpdate.disableSelfLearning) &&
        Objects.equals(this.enforceLessonOrder, lmsCourseUpdate.enforceLessonOrder) &&
        Objects.equals(this.certificationEnabled, lmsCourseUpdate.certificationEnabled) &&
        Objects.equals(this.paidCertificate, lmsCourseUpdate.paidCertificate) &&
        Objects.equals(this.defaultEvaluatorId, lmsCourseUpdate.defaultEvaluatorId) &&
        Objects.equals(this.isPaid, lmsCourseUpdate.isPaid) &&
        Objects.equals(this.price, lmsCourseUpdate.price) &&
        Objects.equals(this.currencyId, lmsCourseUpdate.currencyId) &&
        Objects.equals(this.priceUsd, lmsCourseUpdate.priceUsd) &&
        Objects.equals(this.cardGradient, lmsCourseUpdate.cardGradient) &&
        Objects.equals(this.timezone, lmsCourseUpdate.timezone) &&
        Objects.equals(this.publishNotificationSent, lmsCourseUpdate.publishNotificationSent) &&
        Objects.equals(this.enrollmentCount, lmsCourseUpdate.enrollmentCount) &&
        Objects.equals(this.lessonCount, lmsCourseUpdate.lessonCount) &&
        Objects.equals(this.averageRating, lmsCourseUpdate.averageRating) &&
        Objects.equals(this.code, lmsCourseUpdate.code) &&
        Objects.equals(this.stateStandardCode, lmsCourseUpdate.stateStandardCode) &&
        Objects.equals(this.credits, lmsCourseUpdate.credits) &&
        Objects.equals(this.prerequisites, lmsCourseUpdate.prerequisites) &&
        Objects.equals(this.corequisites, lmsCourseUpdate.corequisites) &&
        Objects.equals(this.effectiveFrom, lmsCourseUpdate.effectiveFrom) &&
        Objects.equals(this.lifecycleStatus, lmsCourseUpdate.lifecycleStatus) &&
        Objects.equals(this.deactivatedOn, lmsCourseUpdate.deactivatedOn) &&
        Objects.equals(this.versionMajor, lmsCourseUpdate.versionMajor) &&
        Objects.equals(this.versionMinor, lmsCourseUpdate.versionMinor) &&
        Objects.equals(this.previousVersionId, lmsCourseUpdate.previousVersionId) &&
        Objects.equals(this.rootCourseId, lmsCourseUpdate.rootCourseId) &&
        Objects.equals(this.authorComments, lmsCourseUpdate.authorComments) &&
        Objects.equals(this.externalRef, lmsCourseUpdate.externalRef) &&
        Objects.equals(this.nominalDuration, lmsCourseUpdate.nominalDuration) &&
        Objects.equals(this.deliveryModality, lmsCourseUpdate.deliveryModality) &&
        Objects.equals(this.isShared, lmsCourseUpdate.isShared) &&
        Objects.equals(this.contentUpdatedAt, lmsCourseUpdate.contentUpdatedAt) &&
        Objects.equals(this.gradeDisplay, lmsCourseUpdate.gradeDisplay) &&
        Objects.equals(this.gradingSchemeId, lmsCourseUpdate.gradingSchemeId) &&
        Objects.equals(this.isInCatalog, lmsCourseUpdate.isInCatalog) &&
        Objects.equals(this.catalogDescription, lmsCourseUpdate.catalogDescription) &&
        Objects.equals(this.catalogVisibleFrom, lmsCourseUpdate.catalogVisibleFrom) &&
        Objects.equals(this.catalogVisibleUntil, lmsCourseUpdate.catalogVisibleUntil) &&
        Objects.equals(this.outline, lmsCourseUpdate.outline) &&
        Objects.equals(this.workloadHours, lmsCourseUpdate.workloadHours) &&
        Objects.equals(this.catalogStatus, lmsCourseUpdate.catalogStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, title, shortIntroduction, description, imageFileId, videoUrl, categoryId, publishedOn, isUpcoming, isFeatured, disableSelfLearning, enforceLessonOrder, certificationEnabled, paidCertificate, defaultEvaluatorId, isPaid, price, currencyId, priceUsd, cardGradient, timezone, publishNotificationSent, enrollmentCount, lessonCount, averageRating, code, stateStandardCode, credits, prerequisites, corequisites, effectiveFrom, lifecycleStatus, deactivatedOn, versionMajor, versionMinor, previousVersionId, rootCourseId, authorComments, externalRef, nominalDuration, deliveryModality, isShared, contentUpdatedAt, gradeDisplay, gradingSchemeId, isInCatalog, catalogDescription, catalogVisibleFrom, catalogVisibleUntil, outline, workloadHours, catalogStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsCourseUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    shortIntroduction: ").append(toIndentedString(shortIntroduction)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
    sb.append("    videoUrl: ").append(toIndentedString(videoUrl)).append("\n");
    sb.append("    categoryId: ").append(toIndentedString(categoryId)).append("\n");
    sb.append("    publishedOn: ").append(toIndentedString(publishedOn)).append("\n");
    sb.append("    isUpcoming: ").append(toIndentedString(isUpcoming)).append("\n");
    sb.append("    isFeatured: ").append(toIndentedString(isFeatured)).append("\n");
    sb.append("    disableSelfLearning: ").append(toIndentedString(disableSelfLearning)).append("\n");
    sb.append("    enforceLessonOrder: ").append(toIndentedString(enforceLessonOrder)).append("\n");
    sb.append("    certificationEnabled: ").append(toIndentedString(certificationEnabled)).append("\n");
    sb.append("    paidCertificate: ").append(toIndentedString(paidCertificate)).append("\n");
    sb.append("    defaultEvaluatorId: ").append(toIndentedString(defaultEvaluatorId)).append("\n");
    sb.append("    isPaid: ").append(toIndentedString(isPaid)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    priceUsd: ").append(toIndentedString(priceUsd)).append("\n");
    sb.append("    cardGradient: ").append(toIndentedString(cardGradient)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    publishNotificationSent: ").append(toIndentedString(publishNotificationSent)).append("\n");
    sb.append("    enrollmentCount: ").append(toIndentedString(enrollmentCount)).append("\n");
    sb.append("    lessonCount: ").append(toIndentedString(lessonCount)).append("\n");
    sb.append("    averageRating: ").append(toIndentedString(averageRating)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    stateStandardCode: ").append(toIndentedString(stateStandardCode)).append("\n");
    sb.append("    credits: ").append(toIndentedString(credits)).append("\n");
    sb.append("    prerequisites: ").append(toIndentedString(prerequisites)).append("\n");
    sb.append("    corequisites: ").append(toIndentedString(corequisites)).append("\n");
    sb.append("    effectiveFrom: ").append(toIndentedString(effectiveFrom)).append("\n");
    sb.append("    lifecycleStatus: ").append(toIndentedString(lifecycleStatus)).append("\n");
    sb.append("    deactivatedOn: ").append(toIndentedString(deactivatedOn)).append("\n");
    sb.append("    versionMajor: ").append(toIndentedString(versionMajor)).append("\n");
    sb.append("    versionMinor: ").append(toIndentedString(versionMinor)).append("\n");
    sb.append("    previousVersionId: ").append(toIndentedString(previousVersionId)).append("\n");
    sb.append("    rootCourseId: ").append(toIndentedString(rootCourseId)).append("\n");
    sb.append("    authorComments: ").append(toIndentedString(authorComments)).append("\n");
    sb.append("    externalRef: ").append(toIndentedString(externalRef)).append("\n");
    sb.append("    nominalDuration: ").append(toIndentedString(nominalDuration)).append("\n");
    sb.append("    deliveryModality: ").append(toIndentedString(deliveryModality)).append("\n");
    sb.append("    isShared: ").append(toIndentedString(isShared)).append("\n");
    sb.append("    contentUpdatedAt: ").append(toIndentedString(contentUpdatedAt)).append("\n");
    sb.append("    gradeDisplay: ").append(toIndentedString(gradeDisplay)).append("\n");
    sb.append("    gradingSchemeId: ").append(toIndentedString(gradingSchemeId)).append("\n");
    sb.append("    isInCatalog: ").append(toIndentedString(isInCatalog)).append("\n");
    sb.append("    catalogDescription: ").append(toIndentedString(catalogDescription)).append("\n");
    sb.append("    catalogVisibleFrom: ").append(toIndentedString(catalogVisibleFrom)).append("\n");
    sb.append("    catalogVisibleUntil: ").append(toIndentedString(catalogVisibleUntil)).append("\n");
    sb.append("    outline: ").append(toIndentedString(outline)).append("\n");
    sb.append("    workloadHours: ").append(toIndentedString(workloadHours)).append("\n");
    sb.append("    catalogStatus: ").append(toIndentedString(catalogStatus)).append("\n");
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

