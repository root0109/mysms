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
 * Fields of LmsCourse that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LmsCourseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> shortIntroduction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> videoUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> categoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> publishedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isUpcoming;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isFeatured;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> disableSelfLearning;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceLessonOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> certificationEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> paidCertificate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> defaultEvaluatorId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPaid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> priceUsd;

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
  private @Nullable Optional<CardGradientEnum> cardGradient;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 64) String> timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> publishNotificationSent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> enrollmentCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> lessonCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> averageRating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 50) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> stateStandardCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> credits;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> prerequisites;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> corequisites;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> effectiveFrom;

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
  private @Nullable Optional<@NotNull LifecycleStatusEnum> lifecycleStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> deactivatedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> versionMajor;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> versionMinor;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> previousVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> rootCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> authorComments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> externalRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> nominalDuration;

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
  private @Nullable Optional<DeliveryModalityEnum> deliveryModality;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isShared;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> contentUpdatedAt;

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
  private @Nullable Optional<@NotNull Boolean> isInCatalog;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> catalogDescription;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> catalogVisibleFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> catalogVisibleUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> outline;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> workloadHours;

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
  private @Nullable Optional<@NotNull CatalogStatusEnum> catalogStatus;

  public LmsCourseUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution (NULL = tenant-wide catalogue).
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

  public LmsCourseUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Course title.
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

  public LmsCourseUpdate shortIntroduction(@Nullable Optional<String> shortIntroduction) {
    this.shortIntroduction = shortIntroduction;
    return this;
  }

  /**
   * Short introduction.
   * @return shortIntroduction
   */
  
  @JsonProperty("shortIntroduction")
  public @Nullable Optional<String> getShortIntroduction() {
    return shortIntroduction;
  }

  @JsonProperty("shortIntroduction")
  public void setShortIntroduction(@Nullable Optional<String> shortIntroduction) {
    this.shortIntroduction = shortIntroduction;
  }

  public LmsCourseUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Full description.
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

  public LmsCourseUpdate imageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Cover image.
   * @return imageFileId
   */
  
  @JsonProperty("imageFileId")
  public @Nullable Optional<UUID> getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
  }

  public LmsCourseUpdate videoUrl(@Nullable Optional<String> videoUrl) {
    this.videoUrl = videoUrl;
    return this;
  }

  /**
   * Promo video URL.
   * @return videoUrl
   */
  
  @JsonProperty("videoUrl")
  public @Nullable Optional<String> getVideoUrl() {
    return videoUrl;
  }

  @JsonProperty("videoUrl")
  public void setVideoUrl(@Nullable Optional<String> videoUrl) {
    this.videoUrl = videoUrl;
  }

  public LmsCourseUpdate categoryId(@Nullable Optional<UUID> categoryId) {
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

  public LmsCourseUpdate publishedOn(@Nullable Optional<LocalDate> publishedOn) {
    this.publishedOn = publishedOn;
    return this;
  }

  /**
   * Publication date.
   * @return publishedOn
   */
  
  @JsonProperty("publishedOn")
  public @Nullable Optional<LocalDate> getPublishedOn() {
    return publishedOn;
  }

  @JsonProperty("publishedOn")
  public void setPublishedOn(@Nullable Optional<LocalDate> publishedOn) {
    this.publishedOn = publishedOn;
  }

  public LmsCourseUpdate isUpcoming(@Nullable Optional<Boolean> isUpcoming) {
    this.isUpcoming = isUpcoming;
    return this;
  }

  /**
   * Announced but not yet open.
   * @return isUpcoming
   */
  
  @JsonProperty("isUpcoming")
  public @Nullable Optional<Boolean> getIsUpcoming() {
    return isUpcoming;
  }

  @JsonProperty("isUpcoming")
  public void setIsUpcoming(@Nullable Optional<Boolean> isUpcoming) {
    this.isUpcoming = isUpcoming;
  }

  public LmsCourseUpdate isFeatured(@Nullable Optional<Boolean> isFeatured) {
    this.isFeatured = isFeatured;
    return this;
  }

  /**
   * Featured in the catalogue.
   * @return isFeatured
   */
  
  @JsonProperty("isFeatured")
  public @Nullable Optional<Boolean> getIsFeatured() {
    return isFeatured;
  }

  @JsonProperty("isFeatured")
  public void setIsFeatured(@Nullable Optional<Boolean> isFeatured) {
    this.isFeatured = isFeatured;
  }

  public LmsCourseUpdate disableSelfLearning(@Nullable Optional<Boolean> disableSelfLearning) {
    this.disableSelfLearning = disableSelfLearning;
    return this;
  }

  /**
   * Only available through cohorts.
   * @return disableSelfLearning
   */
  
  @JsonProperty("disableSelfLearning")
  public @Nullable Optional<Boolean> getDisableSelfLearning() {
    return disableSelfLearning;
  }

  @JsonProperty("disableSelfLearning")
  public void setDisableSelfLearning(@Nullable Optional<Boolean> disableSelfLearning) {
    this.disableSelfLearning = disableSelfLearning;
  }

  public LmsCourseUpdate enforceLessonOrder(@Nullable Optional<Boolean> enforceLessonOrder) {
    this.enforceLessonOrder = enforceLessonOrder;
    return this;
  }

  /**
   * Lessons must be completed in order.
   * @return enforceLessonOrder
   */
  
  @JsonProperty("enforceLessonOrder")
  public @Nullable Optional<Boolean> getEnforceLessonOrder() {
    return enforceLessonOrder;
  }

  @JsonProperty("enforceLessonOrder")
  public void setEnforceLessonOrder(@Nullable Optional<Boolean> enforceLessonOrder) {
    this.enforceLessonOrder = enforceLessonOrder;
  }

  public LmsCourseUpdate certificationEnabled(@Nullable Optional<Boolean> certificationEnabled) {
    this.certificationEnabled = certificationEnabled;
    return this;
  }

  /**
   * Certificate issued on completion.
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

  public LmsCourseUpdate paidCertificate(@Nullable Optional<Boolean> paidCertificate) {
    this.paidCertificate = paidCertificate;
    return this;
  }

  /**
   * Certificate requires payment / evaluation.
   * @return paidCertificate
   */
  
  @JsonProperty("paidCertificate")
  public @Nullable Optional<Boolean> getPaidCertificate() {
    return paidCertificate;
  }

  @JsonProperty("paidCertificate")
  public void setPaidCertificate(@Nullable Optional<Boolean> paidCertificate) {
    this.paidCertificate = paidCertificate;
  }

  public LmsCourseUpdate defaultEvaluatorId(@Nullable Optional<UUID> defaultEvaluatorId) {
    this.defaultEvaluatorId = defaultEvaluatorId;
    return this;
  }

  /**
   * Default evaluator.
   * @return defaultEvaluatorId
   */
  
  @JsonProperty("defaultEvaluatorId")
  public @Nullable Optional<UUID> getDefaultEvaluatorId() {
    return defaultEvaluatorId;
  }

  @JsonProperty("defaultEvaluatorId")
  public void setDefaultEvaluatorId(@Nullable Optional<UUID> defaultEvaluatorId) {
    this.defaultEvaluatorId = defaultEvaluatorId;
  }

  public LmsCourseUpdate isPaid(@Nullable Optional<Boolean> isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Paid course.
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

  public LmsCourseUpdate price(@Nullable Optional<String> price) {
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

  public LmsCourseUpdate currencyId(@Nullable Optional<UUID> currencyId) {
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

  public LmsCourseUpdate priceUsd(@Nullable Optional<String> priceUsd) {
    this.priceUsd = priceUsd;
    return this;
  }

  /**
   * Equivalent USD price for international buyers.
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

  public LmsCourseUpdate cardGradient(@Nullable Optional<CardGradientEnum> cardGradient) {
    this.cardGradient = cardGradient;
    return this;
  }

  /**
   * Catalogue card colour.
   * @return cardGradient
   */
  
  @JsonProperty("cardGradient")
  public @Nullable Optional<CardGradientEnum> getCardGradient() {
    return cardGradient;
  }

  @JsonProperty("cardGradient")
  public void setCardGradient(@Nullable Optional<CardGradientEnum> cardGradient) {
    this.cardGradient = cardGradient;
  }

  public LmsCourseUpdate timezone(@Nullable Optional<String> timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone for scheduled items.
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

  public LmsCourseUpdate publishNotificationSent(@Nullable Optional<Boolean> publishNotificationSent) {
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

  public LmsCourseUpdate enrollmentCount(@Nullable Optional<Integer> enrollmentCount) {
    this.enrollmentCount = enrollmentCount;
    return this;
  }

  /**
   * Cached enrolment count.
   * @return enrollmentCount
   */
  
  @JsonProperty("enrollmentCount")
  public @Nullable Optional<Integer> getEnrollmentCount() {
    return enrollmentCount;
  }

  @JsonProperty("enrollmentCount")
  public void setEnrollmentCount(@Nullable Optional<Integer> enrollmentCount) {
    this.enrollmentCount = enrollmentCount;
  }

  public LmsCourseUpdate lessonCount(@Nullable Optional<Integer> lessonCount) {
    this.lessonCount = lessonCount;
    return this;
  }

  /**
   * Cached lesson count.
   * @return lessonCount
   */
  
  @JsonProperty("lessonCount")
  public @Nullable Optional<Integer> getLessonCount() {
    return lessonCount;
  }

  @JsonProperty("lessonCount")
  public void setLessonCount(@Nullable Optional<Integer> lessonCount) {
    this.lessonCount = lessonCount;
  }

  public LmsCourseUpdate averageRating(@Nullable Optional<String> averageRating) {
    this.averageRating = averageRating;
    return this;
  }

  /**
   * Cached average review rating (0-5).
   * @return averageRating
   */
  
  @JsonProperty("averageRating")
  public @Nullable Optional<String> getAverageRating() {
    return averageRating;
  }

  @JsonProperty("averageRating")
  public void setAverageRating(@Nullable Optional<String> averageRating) {
    this.averageRating = averageRating;
  }

  public LmsCourseUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Course code (unique per version line).
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

  public LmsCourseUpdate stateStandardCode(@Nullable Optional<String> stateStandardCode) {
    this.stateStandardCode = stateStandardCode;
    return this;
  }

  /**
   * Government / state curriculum standard code.
   * @return stateStandardCode
   */
  
  @JsonProperty("stateStandardCode")
  public @Nullable Optional<String> getStateStandardCode() {
    return stateStandardCode;
  }

  @JsonProperty("stateStandardCode")
  public void setStateStandardCode(@Nullable Optional<String> stateStandardCode) {
    this.stateStandardCode = stateStandardCode;
  }

  public LmsCourseUpdate credits(@Nullable Optional<String> credits) {
    this.credits = credits;
    return this;
  }

  /**
   * Credit value.
   * @return credits
   */
  
  @JsonProperty("credits")
  public @Nullable Optional<String> getCredits() {
    return credits;
  }

  @JsonProperty("credits")
  public void setCredits(@Nullable Optional<String> credits) {
    this.credits = credits;
  }

  public LmsCourseUpdate prerequisites(@Nullable Optional<String> prerequisites) {
    this.prerequisites = prerequisites;
    return this;
  }

  /**
   * Prerequisites.
   * @return prerequisites
   */
  
  @JsonProperty("prerequisites")
  public @Nullable Optional<String> getPrerequisites() {
    return prerequisites;
  }

  @JsonProperty("prerequisites")
  public void setPrerequisites(@Nullable Optional<String> prerequisites) {
    this.prerequisites = prerequisites;
  }

  public LmsCourseUpdate corequisites(@Nullable Optional<String> corequisites) {
    this.corequisites = corequisites;
    return this;
  }

  /**
   * Co-requisites.
   * @return corequisites
   */
  
  @JsonProperty("corequisites")
  public @Nullable Optional<String> getCorequisites() {
    return corequisites;
  }

  @JsonProperty("corequisites")
  public void setCorequisites(@Nullable Optional<String> corequisites) {
    this.corequisites = corequisites;
  }

  public LmsCourseUpdate effectiveFrom(@Nullable Optional<LocalDate> effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
    return this;
  }

  /**
   * Version effective date.
   * @return effectiveFrom
   */
  
  @JsonProperty("effectiveFrom")
  public @Nullable Optional<LocalDate> getEffectiveFrom() {
    return effectiveFrom;
  }

  @JsonProperty("effectiveFrom")
  public void setEffectiveFrom(@Nullable Optional<LocalDate> effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LmsCourseUpdate lifecycleStatus(@Nullable Optional<LifecycleStatusEnum> lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
    return this;
  }

  /**
   * Version lifecycle (separate from the authoring review status).
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

  public LmsCourseUpdate deactivatedOn(@Nullable Optional<LocalDate> deactivatedOn) {
    this.deactivatedOn = deactivatedOn;
    return this;
  }

  /**
   * Deactivation date.
   * @return deactivatedOn
   */
  
  @JsonProperty("deactivatedOn")
  public @Nullable Optional<LocalDate> getDeactivatedOn() {
    return deactivatedOn;
  }

  @JsonProperty("deactivatedOn")
  public void setDeactivatedOn(@Nullable Optional<LocalDate> deactivatedOn) {
    this.deactivatedOn = deactivatedOn;
  }

  public LmsCourseUpdate versionMajor(@Nullable Optional<Integer> versionMajor) {
    this.versionMajor = versionMajor;
    return this;
  }

  /**
   * Major version number.
   * @return versionMajor
   */
  
  @JsonProperty("versionMajor")
  public @Nullable Optional<Integer> getVersionMajor() {
    return versionMajor;
  }

  @JsonProperty("versionMajor")
  public void setVersionMajor(@Nullable Optional<Integer> versionMajor) {
    this.versionMajor = versionMajor;
  }

  public LmsCourseUpdate versionMinor(@Nullable Optional<Integer> versionMinor) {
    this.versionMinor = versionMinor;
    return this;
  }

  /**
   * Minor version number.
   * @return versionMinor
   */
  
  @JsonProperty("versionMinor")
  public @Nullable Optional<Integer> getVersionMinor() {
    return versionMinor;
  }

  @JsonProperty("versionMinor")
  public void setVersionMinor(@Nullable Optional<Integer> versionMinor) {
    this.versionMinor = versionMinor;
  }

  public LmsCourseUpdate previousVersionId(@Nullable Optional<UUID> previousVersionId) {
    this.previousVersionId = previousVersionId;
    return this;
  }

  /**
   * Version this one was derived from.
   * @return previousVersionId
   */
  
  @JsonProperty("previousVersionId")
  public @Nullable Optional<UUID> getPreviousVersionId() {
    return previousVersionId;
  }

  @JsonProperty("previousVersionId")
  public void setPreviousVersionId(@Nullable Optional<UUID> previousVersionId) {
    this.previousVersionId = previousVersionId;
  }

  public LmsCourseUpdate rootCourseId(@Nullable Optional<UUID> rootCourseId) {
    this.rootCourseId = rootCourseId;
    return this;
  }

  /**
   * First version of the course line.
   * @return rootCourseId
   */
  
  @JsonProperty("rootCourseId")
  public @Nullable Optional<UUID> getRootCourseId() {
    return rootCourseId;
  }

  @JsonProperty("rootCourseId")
  public void setRootCourseId(@Nullable Optional<UUID> rootCourseId) {
    this.rootCourseId = rootCourseId;
  }

  public LmsCourseUpdate authorComments(@Nullable Optional<String> authorComments) {
    this.authorComments = authorComments;
    return this;
  }

  /**
   * Author's notes on this version.
   * @return authorComments
   */
  
  @JsonProperty("authorComments")
  public @Nullable Optional<String> getAuthorComments() {
    return authorComments;
  }

  @JsonProperty("authorComments")
  public void setAuthorComments(@Nullable Optional<String> authorComments) {
    this.authorComments = authorComments;
  }

  public LmsCourseUpdate externalRef(@Nullable Optional<String> externalRef) {
    this.externalRef = externalRef;
    return this;
  }

  /**
   * External system reference (catalogue / SIS integration key).
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

  public LmsCourseUpdate nominalDuration(@Nullable Optional<Integer> nominalDuration) {
    this.nominalDuration = nominalDuration;
    return this;
  }

  /**
   * Nominal duration of the course (in the institution's planning unit, normally weeks).
   * @return nominalDuration
   */
  
  @JsonProperty("nominalDuration")
  public @Nullable Optional<Integer> getNominalDuration() {
    return nominalDuration;
  }

  @JsonProperty("nominalDuration")
  public void setNominalDuration(@Nullable Optional<Integer> nominalDuration) {
    this.nominalDuration = nominalDuration;
  }

  public LmsCourseUpdate deliveryModality(@Nullable Optional<DeliveryModalityEnum> deliveryModality) {
    this.deliveryModality = deliveryModality;
    return this;
  }

  /**
   * Delivery modality.
   * @return deliveryModality
   */
  
  @JsonProperty("deliveryModality")
  public @Nullable Optional<DeliveryModalityEnum> getDeliveryModality() {
    return deliveryModality;
  }

  @JsonProperty("deliveryModality")
  public void setDeliveryModality(@Nullable Optional<DeliveryModalityEnum> deliveryModality) {
    this.deliveryModality = deliveryModality;
  }

  public LmsCourseUpdate isShared(@Nullable Optional<Boolean> isShared) {
    this.isShared = isShared;
    return this;
  }

  /**
   * Shared with every institution of the tenant.
   * @return isShared
   */
  
  @JsonProperty("isShared")
  public @Nullable Optional<Boolean> getIsShared() {
    return isShared;
  }

  @JsonProperty("isShared")
  public void setIsShared(@Nullable Optional<Boolean> isShared) {
    this.isShared = isShared;
  }

  public LmsCourseUpdate contentUpdatedAt(@Nullable Optional<OffsetDateTime> contentUpdatedAt) {
    this.contentUpdatedAt = contentUpdatedAt;
    return this;
  }

  /**
   * Last change to the course content (drives cohort update prompts).
   * @return contentUpdatedAt
   */
  
  @JsonProperty("contentUpdatedAt")
  public @Nullable Optional<OffsetDateTime> getContentUpdatedAt() {
    return contentUpdatedAt;
  }

  @JsonProperty("contentUpdatedAt")
  public void setContentUpdatedAt(@Nullable Optional<OffsetDateTime> contentUpdatedAt) {
    this.contentUpdatedAt = contentUpdatedAt;
  }

  public LmsCourseUpdate gradeDisplay(@Nullable Optional<GradeDisplayEnum> gradeDisplay) {
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

  public LmsCourseUpdate gradingSchemeId(@Nullable Optional<UUID> gradingSchemeId) {
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

  public LmsCourseUpdate isInCatalog(@Nullable Optional<Boolean> isInCatalog) {
    this.isInCatalog = isInCatalog;
    return this;
  }

  /**
   * Listed in the course catalogue.
   * @return isInCatalog
   */
  
  @JsonProperty("isInCatalog")
  public @Nullable Optional<Boolean> getIsInCatalog() {
    return isInCatalog;
  }

  @JsonProperty("isInCatalog")
  public void setIsInCatalog(@Nullable Optional<Boolean> isInCatalog) {
    this.isInCatalog = isInCatalog;
  }

  public LmsCourseUpdate catalogDescription(@Nullable Optional<String> catalogDescription) {
    this.catalogDescription = catalogDescription;
    return this;
  }

  /**
   * Catalogue description (marketing copy).
   * @return catalogDescription
   */
  
  @JsonProperty("catalogDescription")
  public @Nullable Optional<String> getCatalogDescription() {
    return catalogDescription;
  }

  @JsonProperty("catalogDescription")
  public void setCatalogDescription(@Nullable Optional<String> catalogDescription) {
    this.catalogDescription = catalogDescription;
  }

  public LmsCourseUpdate catalogVisibleFrom(@Nullable Optional<OffsetDateTime> catalogVisibleFrom) {
    this.catalogVisibleFrom = catalogVisibleFrom;
    return this;
  }

  /**
   * Catalogue listing visible from.
   * @return catalogVisibleFrom
   */
  
  @JsonProperty("catalogVisibleFrom")
  public @Nullable Optional<OffsetDateTime> getCatalogVisibleFrom() {
    return catalogVisibleFrom;
  }

  @JsonProperty("catalogVisibleFrom")
  public void setCatalogVisibleFrom(@Nullable Optional<OffsetDateTime> catalogVisibleFrom) {
    this.catalogVisibleFrom = catalogVisibleFrom;
  }

  public LmsCourseUpdate catalogVisibleUntil(@Nullable Optional<OffsetDateTime> catalogVisibleUntil) {
    this.catalogVisibleUntil = catalogVisibleUntil;
    return this;
  }

  /**
   * Catalogue listing visible until.
   * @return catalogVisibleUntil
   */
  
  @JsonProperty("catalogVisibleUntil")
  public @Nullable Optional<OffsetDateTime> getCatalogVisibleUntil() {
    return catalogVisibleUntil;
  }

  @JsonProperty("catalogVisibleUntil")
  public void setCatalogVisibleUntil(@Nullable Optional<OffsetDateTime> catalogVisibleUntil) {
    this.catalogVisibleUntil = catalogVisibleUntil;
  }

  public LmsCourseUpdate outline(@Nullable Optional<String> outline) {
    this.outline = outline;
    return this;
  }

  /**
   * Course outline shown in the catalogue.
   * @return outline
   */
  
  @JsonProperty("outline")
  public @Nullable Optional<String> getOutline() {
    return outline;
  }

  @JsonProperty("outline")
  public void setOutline(@Nullable Optional<String> outline) {
    this.outline = outline;
  }

  public LmsCourseUpdate workloadHours(@Nullable Optional<Integer> workloadHours) {
    this.workloadHours = workloadHours;
    return this;
  }

  /**
   * Expected student workload in hours.
   * @return workloadHours
   */
  
  @JsonProperty("workloadHours")
  public @Nullable Optional<Integer> getWorkloadHours() {
    return workloadHours;
  }

  @JsonProperty("workloadHours")
  public void setWorkloadHours(@Nullable Optional<Integer> workloadHours) {
    this.workloadHours = workloadHours;
  }

  public LmsCourseUpdate catalogStatus(@Nullable Optional<CatalogStatusEnum> catalogStatus) {
    this.catalogStatus = catalogStatus;
    return this;
  }

  /**
   * Catalogue listing state.
   * @return catalogStatus
   */
  
  @JsonProperty("catalogStatus")
  public @Nullable Optional<CatalogStatusEnum> getCatalogStatus() {
    return catalogStatus;
  }

  @JsonProperty("catalogStatus")
  public void setCatalogStatus(@Nullable Optional<CatalogStatusEnum> catalogStatus) {
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

