// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * CalendarItem
 */

public class CalendarItem {

  /**
   * Kind of item.
   */
  public enum ItemTypeEnum {
    EVENT("event"),
    
    OCCURRENCE("occurrence"),
    
    DUE_DATE("due_date"),
    
    LIVE_CLASS("live_class"),
    
    COHORT_SESSION("cohort_session"),
    
    HOLIDAY("holiday");

    private final String value;

    ItemTypeEnum(String value) {
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
    public static ItemTypeEnum fromValue(String value) {
      for (ItemTypeEnum b : ItemTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ItemTypeEnum itemType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isCancelled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isCompleted;

  public CalendarItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CalendarItem(ItemTypeEnum itemType, UUID id, String title, OffsetDateTime startAt) {
    this.itemType = itemType;
    this.id = id;
    this.title = title;
    this.startAt = startAt;
  }

  public CalendarItem itemType(ItemTypeEnum itemType) {
    this.itemType = itemType;
    return this;
  }

  /**
   * Kind of item.
   * @return itemType
   */
  @NotNull 
  @JsonProperty("itemType")
  public ItemTypeEnum getItemType() {
    return itemType;
  }

  @JsonProperty("itemType")
  public void setItemType(ItemTypeEnum itemType) {
    this.itemType = itemType;
  }

  public CalendarItem id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Record id.
   * @return id
   */
  @NotNull @Valid 
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public CalendarItem title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public CalendarItem startAt(OffsetDateTime startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Start.
   * @return startAt
   */
  @NotNull @Valid 
  @JsonProperty("startAt")
  public OffsetDateTime getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(OffsetDateTime startAt) {
    this.startAt = startAt;
  }

  public CalendarItem endAt(@Nullable OffsetDateTime endAt) {
    this.endAt = endAt;
    return this;
  }

  /**
   * End.
   * @return endAt
   */
  @Valid 
  @JsonProperty("endAt")
  public @Nullable OffsetDateTime getEndAt() {
    return endAt;
  }

  @JsonProperty("endAt")
  public void setEndAt(@Nullable OffsetDateTime endAt) {
    this.endAt = endAt;
  }

  public CalendarItem allDay(@Nullable Boolean allDay) {
    this.allDay = allDay;
    return this;
  }

  /**
   * All-day item.
   * @return allDay
   */
  
  @JsonProperty("allDay")
  public @Nullable Boolean getAllDay() {
    return allDay;
  }

  @JsonProperty("allDay")
  public void setAllDay(@Nullable Boolean allDay) {
    this.allDay = allDay;
  }

  public CalendarItem color(@Nullable String color) {
    this.color = color;
    return this;
  }

  /**
   * Colour.
   * @return color
   */
  
  @JsonProperty("color")
  public @Nullable String getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable String color) {
    this.color = color;
  }

  public CalendarItem cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public CalendarItem contentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item due.
   * @return contentItemId
   */
  @Valid 
  @JsonProperty("contentItemId")
  public @Nullable UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public CalendarItem isCancelled(@Nullable Boolean isCancelled) {
    this.isCancelled = isCancelled;
    return this;
  }

  /**
   * Cancelled occurrence.
   * @return isCancelled
   */
  
  @JsonProperty("isCancelled")
  public @Nullable Boolean getIsCancelled() {
    return isCancelled;
  }

  @JsonProperty("isCancelled")
  public void setIsCancelled(@Nullable Boolean isCancelled) {
    this.isCancelled = isCancelled;
  }

  public CalendarItem isCompleted(@Nullable Boolean isCompleted) {
    this.isCompleted = isCompleted;
    return this;
  }

  /**
   * Submitted / completed by the caller.
   * @return isCompleted
   */
  
  @JsonProperty("isCompleted")
  public @Nullable Boolean getIsCompleted() {
    return isCompleted;
  }

  @JsonProperty("isCompleted")
  public void setIsCompleted(@Nullable Boolean isCompleted) {
    this.isCompleted = isCompleted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CalendarItem calendarItem = (CalendarItem) o;
    return Objects.equals(this.itemType, calendarItem.itemType) &&
        Objects.equals(this.id, calendarItem.id) &&
        Objects.equals(this.title, calendarItem.title) &&
        Objects.equals(this.startAt, calendarItem.startAt) &&
        Objects.equals(this.endAt, calendarItem.endAt) &&
        Objects.equals(this.allDay, calendarItem.allDay) &&
        Objects.equals(this.color, calendarItem.color) &&
        Objects.equals(this.cohortId, calendarItem.cohortId) &&
        Objects.equals(this.contentItemId, calendarItem.contentItemId) &&
        Objects.equals(this.isCancelled, calendarItem.isCancelled) &&
        Objects.equals(this.isCompleted, calendarItem.isCompleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemType, id, title, startAt, endAt, allDay, color, cohortId, contentItemId, isCancelled, isCompleted);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CalendarItem {\n");
    sb.append("    itemType: ").append(toIndentedString(itemType)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    startAt: ").append(toIndentedString(startAt)).append("\n");
    sb.append("    endAt: ").append(toIndentedString(endAt)).append("\n");
    sb.append("    allDay: ").append(toIndentedString(allDay)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    isCancelled: ").append(toIndentedString(isCancelled)).append("\n");
    sb.append("    isCompleted: ").append(toIndentedString(isCompleted)).append("\n");
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

