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
 * Online course.
 */

public class LmsCourse {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

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

  /**
   * Authoring / review state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    IN_PROGRESS("in_progress"),
    
    UNDER_REVIEW("under_review"),
    
    APPROVED("approved");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPublished;

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

  public LmsCourse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LmsCourse(String title) {
    this.title = title;
  }

  public LmsCourse id(@Nullable UUID id) {
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

  public LmsCourse institutionId(@Nullable UUID institutionId) {
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

  public LmsCourse title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Course title.
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

  public LmsCourse shortIntroduction(@Nullable String shortIntroduction) {
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

  public LmsCourse description(@Nullable String description) {
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

  public LmsCourse imageFileId(@Nullable UUID imageFileId) {
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

  public LmsCourse videoUrl(@Nullable String videoUrl) {
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

  public LmsCourse categoryId(@Nullable UUID categoryId) {
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

  public LmsCourse status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Authoring / review state. Changed only through the workflow operations.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public LmsCourse isPublished(@Nullable Boolean isPublished) {
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

  public LmsCourse publishedOn(@Nullable LocalDate publishedOn) {
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

  public LmsCourse isUpcoming(@Nullable Boolean isUpcoming) {
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

  public LmsCourse isFeatured(@Nullable Boolean isFeatured) {
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

  public LmsCourse disableSelfLearning(@Nullable Boolean disableSelfLearning) {
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

  public LmsCourse enforceLessonOrder(@Nullable Boolean enforceLessonOrder) {
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

  public LmsCourse certificationEnabled(@Nullable Boolean certificationEnabled) {
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

  public LmsCourse paidCertificate(@Nullable Boolean paidCertificate) {
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

  public LmsCourse defaultEvaluatorId(@Nullable UUID defaultEvaluatorId) {
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

  public LmsCourse isPaid(@Nullable Boolean isPaid) {
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

  public LmsCourse price(@Nullable String price) {
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

  public LmsCourse currencyId(@Nullable UUID currencyId) {
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

  public LmsCourse priceUsd(@Nullable String priceUsd) {
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

  public LmsCourse cardGradient(@Nullable CardGradientEnum cardGradient) {
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

  public LmsCourse timezone(@Nullable String timezone) {
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

  public LmsCourse publishNotificationSent(@Nullable Boolean publishNotificationSent) {
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

  public LmsCourse enrollmentCount(@Nullable Integer enrollmentCount) {
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

  public LmsCourse lessonCount(@Nullable Integer lessonCount) {
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

  public LmsCourse averageRating(@Nullable String averageRating) {
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

  public LmsCourse code(@Nullable String code) {
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

  public LmsCourse stateStandardCode(@Nullable String stateStandardCode) {
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

  public LmsCourse credits(@Nullable String credits) {
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

  public LmsCourse prerequisites(@Nullable String prerequisites) {
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

  public LmsCourse corequisites(@Nullable String corequisites) {
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

  public LmsCourse effectiveFrom(@Nullable LocalDate effectiveFrom) {
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

  public LmsCourse lifecycleStatus(@Nullable LifecycleStatusEnum lifecycleStatus) {
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

  public LmsCourse deactivatedOn(@Nullable LocalDate deactivatedOn) {
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

  public LmsCourse versionMajor(@Nullable Integer versionMajor) {
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

  public LmsCourse versionMinor(@Nullable Integer versionMinor) {
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

  public LmsCourse previousVersionId(@Nullable UUID previousVersionId) {
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

  public LmsCourse rootCourseId(@Nullable UUID rootCourseId) {
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

  public LmsCourse authorComments(@Nullable String authorComments) {
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

  public LmsCourse externalRef(@Nullable String externalRef) {
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

  public LmsCourse nominalDuration(@Nullable Integer nominalDuration) {
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

  public LmsCourse deliveryModality(@Nullable DeliveryModalityEnum deliveryModality) {
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

  public LmsCourse isShared(@Nullable Boolean isShared) {
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

  public LmsCourse contentUpdatedAt(@Nullable OffsetDateTime contentUpdatedAt) {
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

  public LmsCourse gradeDisplay(@Nullable GradeDisplayEnum gradeDisplay) {
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

  public LmsCourse gradingSchemeId(@Nullable UUID gradingSchemeId) {
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

  public LmsCourse isInCatalog(@Nullable Boolean isInCatalog) {
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

  public LmsCourse catalogDescription(@Nullable String catalogDescription) {
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

  public LmsCourse catalogVisibleFrom(@Nullable OffsetDateTime catalogVisibleFrom) {
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

  public LmsCourse catalogVisibleUntil(@Nullable OffsetDateTime catalogVisibleUntil) {
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

  public LmsCourse outline(@Nullable String outline) {
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

  public LmsCourse workloadHours(@Nullable Integer workloadHours) {
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

  public LmsCourse catalogStatus(@Nullable CatalogStatusEnum catalogStatus) {
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

  public LmsCourse createTime(@Nullable OffsetDateTime createTime) {
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

  public LmsCourse createdBy(@Nullable UUID createdBy) {
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

  public LmsCourse updateTime(@Nullable OffsetDateTime updateTime) {
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

  public LmsCourse updatedBy(@Nullable UUID updatedBy) {
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

  public LmsCourse etag(@Nullable String etag) {
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
    LmsCourse lmsCourse = (LmsCourse) o;
    return Objects.equals(this.id, lmsCourse.id) &&
        Objects.equals(this.institutionId, lmsCourse.institutionId) &&
        Objects.equals(this.title, lmsCourse.title) &&
        Objects.equals(this.shortIntroduction, lmsCourse.shortIntroduction) &&
        Objects.equals(this.description, lmsCourse.description) &&
        Objects.equals(this.imageFileId, lmsCourse.imageFileId) &&
        Objects.equals(this.videoUrl, lmsCourse.videoUrl) &&
        Objects.equals(this.categoryId, lmsCourse.categoryId) &&
        Objects.equals(this.status, lmsCourse.status) &&
        Objects.equals(this.isPublished, lmsCourse.isPublished) &&
        Objects.equals(this.publishedOn, lmsCourse.publishedOn) &&
        Objects.equals(this.isUpcoming, lmsCourse.isUpcoming) &&
        Objects.equals(this.isFeatured, lmsCourse.isFeatured) &&
        Objects.equals(this.disableSelfLearning, lmsCourse.disableSelfLearning) &&
        Objects.equals(this.enforceLessonOrder, lmsCourse.enforceLessonOrder) &&
        Objects.equals(this.certificationEnabled, lmsCourse.certificationEnabled) &&
        Objects.equals(this.paidCertificate, lmsCourse.paidCertificate) &&
        Objects.equals(this.defaultEvaluatorId, lmsCourse.defaultEvaluatorId) &&
        Objects.equals(this.isPaid, lmsCourse.isPaid) &&
        Objects.equals(this.price, lmsCourse.price) &&
        Objects.equals(this.currencyId, lmsCourse.currencyId) &&
        Objects.equals(this.priceUsd, lmsCourse.priceUsd) &&
        Objects.equals(this.cardGradient, lmsCourse.cardGradient) &&
        Objects.equals(this.timezone, lmsCourse.timezone) &&
        Objects.equals(this.publishNotificationSent, lmsCourse.publishNotificationSent) &&
        Objects.equals(this.enrollmentCount, lmsCourse.enrollmentCount) &&
        Objects.equals(this.lessonCount, lmsCourse.lessonCount) &&
        Objects.equals(this.averageRating, lmsCourse.averageRating) &&
        Objects.equals(this.code, lmsCourse.code) &&
        Objects.equals(this.stateStandardCode, lmsCourse.stateStandardCode) &&
        Objects.equals(this.credits, lmsCourse.credits) &&
        Objects.equals(this.prerequisites, lmsCourse.prerequisites) &&
        Objects.equals(this.corequisites, lmsCourse.corequisites) &&
        Objects.equals(this.effectiveFrom, lmsCourse.effectiveFrom) &&
        Objects.equals(this.lifecycleStatus, lmsCourse.lifecycleStatus) &&
        Objects.equals(this.deactivatedOn, lmsCourse.deactivatedOn) &&
        Objects.equals(this.versionMajor, lmsCourse.versionMajor) &&
        Objects.equals(this.versionMinor, lmsCourse.versionMinor) &&
        Objects.equals(this.previousVersionId, lmsCourse.previousVersionId) &&
        Objects.equals(this.rootCourseId, lmsCourse.rootCourseId) &&
        Objects.equals(this.authorComments, lmsCourse.authorComments) &&
        Objects.equals(this.externalRef, lmsCourse.externalRef) &&
        Objects.equals(this.nominalDuration, lmsCourse.nominalDuration) &&
        Objects.equals(this.deliveryModality, lmsCourse.deliveryModality) &&
        Objects.equals(this.isShared, lmsCourse.isShared) &&
        Objects.equals(this.contentUpdatedAt, lmsCourse.contentUpdatedAt) &&
        Objects.equals(this.gradeDisplay, lmsCourse.gradeDisplay) &&
        Objects.equals(this.gradingSchemeId, lmsCourse.gradingSchemeId) &&
        Objects.equals(this.isInCatalog, lmsCourse.isInCatalog) &&
        Objects.equals(this.catalogDescription, lmsCourse.catalogDescription) &&
        Objects.equals(this.catalogVisibleFrom, lmsCourse.catalogVisibleFrom) &&
        Objects.equals(this.catalogVisibleUntil, lmsCourse.catalogVisibleUntil) &&
        Objects.equals(this.outline, lmsCourse.outline) &&
        Objects.equals(this.workloadHours, lmsCourse.workloadHours) &&
        Objects.equals(this.catalogStatus, lmsCourse.catalogStatus) &&
        Objects.equals(this.createTime, lmsCourse.createTime) &&
        Objects.equals(this.createdBy, lmsCourse.createdBy) &&
        Objects.equals(this.updateTime, lmsCourse.updateTime) &&
        Objects.equals(this.updatedBy, lmsCourse.updatedBy) &&
        Objects.equals(this.etag, lmsCourse.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, institutionId, title, shortIntroduction, description, imageFileId, videoUrl, categoryId, status, isPublished, publishedOn, isUpcoming, isFeatured, disableSelfLearning, enforceLessonOrder, certificationEnabled, paidCertificate, defaultEvaluatorId, isPaid, price, currencyId, priceUsd, cardGradient, timezone, publishNotificationSent, enrollmentCount, lessonCount, averageRating, code, stateStandardCode, credits, prerequisites, corequisites, effectiveFrom, lifecycleStatus, deactivatedOn, versionMajor, versionMinor, previousVersionId, rootCourseId, authorComments, externalRef, nominalDuration, deliveryModality, isShared, contentUpdatedAt, gradeDisplay, gradingSchemeId, isInCatalog, catalogDescription, catalogVisibleFrom, catalogVisibleUntil, outline, workloadHours, catalogStatus, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsCourse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    shortIntroduction: ").append(toIndentedString(shortIntroduction)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
    sb.append("    videoUrl: ").append(toIndentedString(videoUrl)).append("\n");
    sb.append("    categoryId: ").append(toIndentedString(categoryId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    isPublished: ").append(toIndentedString(isPublished)).append("\n");
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

