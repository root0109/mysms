// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Event that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class EventUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  /**
   * Audience.
   */
  public enum AudienceEnum {
    SCHOOL("school"),
    
    propertyClass("class"),
    
    ALUMNI("alumni"),
    
    TENANT("tenant"),
    
    COHORT("cohort"),
    
    PERSONAL("personal");

    private final String value;

    AudienceEnum(String value) {
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
    public static AudienceEnum fromValue(String value) {
      for (AudienceEnum b : AudienceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull AudienceEnum> audience;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> audienceLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  /**
   * Category.
   */
  public enum CategoryEnum {
    CULTURAL("cultural"),
    
    EDUCATION("education"),
    
    EXAM("exam"),
    
    HOLIDAY("holiday"),
    
    MEETING("meeting"),
    
    LIVE_CLASS("live_class"),
    
    OTHER("other");

    private final String value;

    CategoryEnum(String value) {
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
    public static CategoryEnum fromValue(String value) {
      for (CategoryEnum b : CategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull CategoryEnum> category;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> organisedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isAllDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> repeatCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> repeatInterval;

  /**
   * Unit of the interval.
   */
  public enum RepeatUnitEnum {
    DAY("day"),
    
    WEEK("week"),
    
    MONTH("month"),
    
    YEAR("year");

    private final String value;

    RepeatUnitEnum(String value) {
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
    public static RepeatUnitEnum fromValue(String value) {
      for (RepeatUnitEnum b : RepeatUnitEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<RepeatUnitEnum> repeatUnit;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> recurrenceRule;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> forAllMembers;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> staffCanEdit;

  /**
   * Importance of a personal event.
   */
  public enum ImportanceEnum {
    HIGH("high"),
    
    MEDIUM("medium"),
    
    LOW("low");

    private final String value;

    ImportanceEnum(String value) {
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
    public static ImportanceEnum fromValue(String value) {
      for (ImportanceEnum b : ImportanceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<ImportanceEnum> importance;

  public EventUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide event, e.g. a live class of a tenant-wide cohort).
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

  public EventUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
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

  public EventUpdate audience(@Nullable Optional<AudienceEnum> audience) {
    this.audience = audience;
    return this;
  }

  /**
   * Audience.
   * @return audience
   */
  
  @JsonProperty("audience")
  public @Nullable Optional<AudienceEnum> getAudience() {
    return audience;
  }

  @JsonProperty("audience")
  public void setAudience(@Nullable Optional<AudienceEnum> audience) {
    this.audience = audience;
  }

  public EventUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (audience = class).
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public EventUpdate audienceLabel(@Nullable Optional<String> audienceLabel) {
    this.audienceLabel = audienceLabel;
    return this;
  }

  /**
   * Free-text audience / batch label.
   * @return audienceLabel
   */
  
  @JsonProperty("audienceLabel")
  public @Nullable Optional<String> getAudienceLabel() {
    return audienceLabel;
  }

  @JsonProperty("audienceLabel")
  public void setAudienceLabel(@Nullable Optional<String> audienceLabel) {
    this.audienceLabel = audienceLabel;
  }

  public EventUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public EventUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

  public EventUpdate category(@Nullable Optional<CategoryEnum> category) {
    this.category = category;
    return this;
  }

  /**
   * Category.
   * @return category
   */
  
  @JsonProperty("category")
  public @Nullable Optional<CategoryEnum> getCategory() {
    return category;
  }

  @JsonProperty("category")
  public void setCategory(@Nullable Optional<CategoryEnum> category) {
    this.category = category;
  }

  public EventUpdate location(@Nullable Optional<String> location) {
    this.location = location;
    return this;
  }

  /**
   * Location.
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable Optional<String> getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable Optional<String> location) {
    this.location = location;
  }

  public EventUpdate organisedBy(@Nullable Optional<String> organisedBy) {
    this.organisedBy = organisedBy;
    return this;
  }

  /**
   * Organiser(s).
   * @return organisedBy
   */
  
  @JsonProperty("organisedBy")
  public @Nullable Optional<String> getOrganisedBy() {
    return organisedBy;
  }

  @JsonProperty("organisedBy")
  public void setOrganisedBy(@Nullable Optional<String> organisedBy) {
    this.organisedBy = organisedBy;
  }

  public EventUpdate imageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Banner image.
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

  public EventUpdate color(@Nullable Optional<String> color) {
    this.color = color;
    return this;
  }

  /**
   * Calendar colour.
   * @return color
   */
  
  @JsonProperty("color")
  public @Nullable Optional<String> getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable Optional<String> color) {
    this.color = color;
  }

  public EventUpdate startAt(@Nullable Optional<OffsetDateTime> startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Start.
   * @return startAt
   */
  
  @JsonProperty("startAt")
  public @Nullable Optional<OffsetDateTime> getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(@Nullable Optional<OffsetDateTime> startAt) {
    this.startAt = startAt;
  }

  public EventUpdate endAt(@Nullable Optional<OffsetDateTime> endAt) {
    this.endAt = endAt;
    return this;
  }

  /**
   * End.
   * @return endAt
   */
  
  @JsonProperty("endAt")
  public @Nullable Optional<OffsetDateTime> getEndAt() {
    return endAt;
  }

  @JsonProperty("endAt")
  public void setEndAt(@Nullable Optional<OffsetDateTime> endAt) {
    this.endAt = endAt;
  }

  public EventUpdate isAllDay(@Nullable Optional<Boolean> isAllDay) {
    this.isAllDay = isAllDay;
    return this;
  }

  /**
   * All-day event.
   * @return isAllDay
   */
  
  @JsonProperty("isAllDay")
  public @Nullable Optional<Boolean> getIsAllDay() {
    return isAllDay;
  }

  @JsonProperty("isAllDay")
  public void setIsAllDay(@Nullable Optional<Boolean> isAllDay) {
    this.isAllDay = isAllDay;
  }

  public EventUpdate repeatCount(@Nullable Optional<Integer> repeatCount) {
    this.repeatCount = repeatCount;
    return this;
  }

  /**
   * Occurrences to repeat (0 = none).
   * @return repeatCount
   */
  
  @JsonProperty("repeatCount")
  public @Nullable Optional<Integer> getRepeatCount() {
    return repeatCount;
  }

  @JsonProperty("repeatCount")
  public void setRepeatCount(@Nullable Optional<Integer> repeatCount) {
    this.repeatCount = repeatCount;
  }

  public EventUpdate repeatInterval(@Nullable Optional<Integer> repeatInterval) {
    this.repeatInterval = repeatInterval;
    return this;
  }

  /**
   * Interval between occurrences.
   * @return repeatInterval
   */
  
  @JsonProperty("repeatInterval")
  public @Nullable Optional<Integer> getRepeatInterval() {
    return repeatInterval;
  }

  @JsonProperty("repeatInterval")
  public void setRepeatInterval(@Nullable Optional<Integer> repeatInterval) {
    this.repeatInterval = repeatInterval;
  }

  public EventUpdate repeatUnit(@Nullable Optional<RepeatUnitEnum> repeatUnit) {
    this.repeatUnit = repeatUnit;
    return this;
  }

  /**
   * Unit of the interval.
   * @return repeatUnit
   */
  
  @JsonProperty("repeatUnit")
  public @Nullable Optional<RepeatUnitEnum> getRepeatUnit() {
    return repeatUnit;
  }

  @JsonProperty("repeatUnit")
  public void setRepeatUnit(@Nullable Optional<RepeatUnitEnum> repeatUnit) {
    this.repeatUnit = repeatUnit;
  }

  public EventUpdate url(@Nullable Optional<String> url) {
    this.url = url;
    return this;
  }

  /**
   * Related link.
   * @return url
   */
  
  @JsonProperty("url")
  public @Nullable Optional<String> getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable Optional<String> url) {
    this.url = url;
  }

  public EventUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort of a cohort event (audience = cohort).
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public EventUpdate ownerUserId(@Nullable Optional<UUID> ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Owner of a personal event (audience = personal).
   * @return ownerUserId
   */
  
  @JsonProperty("ownerUserId")
  public @Nullable Optional<UUID> getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable Optional<UUID> ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public EventUpdate recurrenceRule(@Nullable Optional<String> recurrenceRule) {
    this.recurrenceRule = recurrenceRule;
    return this;
  }

  /**
   * iCalendar RRULE of a recurring event (NULL = not recurring); occurrences are materialised in comms.event_occurrence.
   * @return recurrenceRule
   */
  
  @JsonProperty("recurrenceRule")
  public @Nullable Optional<String> getRecurrenceRule() {
    return recurrenceRule;
  }

  @JsonProperty("recurrenceRule")
  public void setRecurrenceRule(@Nullable Optional<String> recurrenceRule) {
    this.recurrenceRule = recurrenceRule;
  }

  public EventUpdate forAllMembers(@Nullable Optional<Boolean> forAllMembers) {
    this.forAllMembers = forAllMembers;
    return this;
  }

  /**
   * Cohort event for every member (false = invited attendees only).
   * @return forAllMembers
   */
  
  @JsonProperty("forAllMembers")
  public @Nullable Optional<Boolean> getForAllMembers() {
    return forAllMembers;
  }

  @JsonProperty("forAllMembers")
  public void setForAllMembers(@Nullable Optional<Boolean> forAllMembers) {
    this.forAllMembers = forAllMembers;
  }

  public EventUpdate staffCanEdit(@Nullable Optional<Boolean> staffCanEdit) {
    this.staffCanEdit = staffCanEdit;
    return this;
  }

  /**
   * Cohort staff may change the event.
   * @return staffCanEdit
   */
  
  @JsonProperty("staffCanEdit")
  public @Nullable Optional<Boolean> getStaffCanEdit() {
    return staffCanEdit;
  }

  @JsonProperty("staffCanEdit")
  public void setStaffCanEdit(@Nullable Optional<Boolean> staffCanEdit) {
    this.staffCanEdit = staffCanEdit;
  }

  public EventUpdate importance(@Nullable Optional<ImportanceEnum> importance) {
    this.importance = importance;
    return this;
  }

  /**
   * Importance of a personal event.
   * @return importance
   */
  
  @JsonProperty("importance")
  public @Nullable Optional<ImportanceEnum> getImportance() {
    return importance;
  }

  @JsonProperty("importance")
  public void setImportance(@Nullable Optional<ImportanceEnum> importance) {
    this.importance = importance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventUpdate eventUpdate = (EventUpdate) o;
    return Objects.equals(this.institutionId, eventUpdate.institutionId) &&
        Objects.equals(this.academicYearId, eventUpdate.academicYearId) &&
        Objects.equals(this.audience, eventUpdate.audience) &&
        Objects.equals(this.classSectionId, eventUpdate.classSectionId) &&
        Objects.equals(this.audienceLabel, eventUpdate.audienceLabel) &&
        Objects.equals(this.title, eventUpdate.title) &&
        Objects.equals(this.description, eventUpdate.description) &&
        Objects.equals(this.category, eventUpdate.category) &&
        Objects.equals(this.location, eventUpdate.location) &&
        Objects.equals(this.organisedBy, eventUpdate.organisedBy) &&
        Objects.equals(this.imageFileId, eventUpdate.imageFileId) &&
        Objects.equals(this.color, eventUpdate.color) &&
        Objects.equals(this.startAt, eventUpdate.startAt) &&
        Objects.equals(this.endAt, eventUpdate.endAt) &&
        Objects.equals(this.isAllDay, eventUpdate.isAllDay) &&
        Objects.equals(this.repeatCount, eventUpdate.repeatCount) &&
        Objects.equals(this.repeatInterval, eventUpdate.repeatInterval) &&
        Objects.equals(this.repeatUnit, eventUpdate.repeatUnit) &&
        Objects.equals(this.url, eventUpdate.url) &&
        Objects.equals(this.cohortId, eventUpdate.cohortId) &&
        Objects.equals(this.ownerUserId, eventUpdate.ownerUserId) &&
        Objects.equals(this.recurrenceRule, eventUpdate.recurrenceRule) &&
        Objects.equals(this.forAllMembers, eventUpdate.forAllMembers) &&
        Objects.equals(this.staffCanEdit, eventUpdate.staffCanEdit) &&
        Objects.equals(this.importance, eventUpdate.importance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, audience, classSectionId, audienceLabel, title, description, category, location, organisedBy, imageFileId, color, startAt, endAt, isAllDay, repeatCount, repeatInterval, repeatUnit, url, cohortId, ownerUserId, recurrenceRule, forAllMembers, staffCanEdit, importance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    audience: ").append(toIndentedString(audience)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    audienceLabel: ").append(toIndentedString(audienceLabel)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    organisedBy: ").append(toIndentedString(organisedBy)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
    sb.append("    startAt: ").append(toIndentedString(startAt)).append("\n");
    sb.append("    endAt: ").append(toIndentedString(endAt)).append("\n");
    sb.append("    isAllDay: ").append(toIndentedString(isAllDay)).append("\n");
    sb.append("    repeatCount: ").append(toIndentedString(repeatCount)).append("\n");
    sb.append("    repeatInterval: ").append(toIndentedString(repeatInterval)).append("\n");
    sb.append("    repeatUnit: ").append(toIndentedString(repeatUnit)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    recurrenceRule: ").append(toIndentedString(recurrenceRule)).append("\n");
    sb.append("    forAllMembers: ").append(toIndentedString(forAllMembers)).append("\n");
    sb.append("    staffCanEdit: ").append(toIndentedString(staffCanEdit)).append("\n");
    sb.append("    importance: ").append(toIndentedString(importance)).append("\n");
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

