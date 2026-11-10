// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
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
 * Fields of EventOccurrence that can be updated (partial update; see updateMask).
 */

public class EventOccurrenceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isCancelled;

  /**
   * Attendee's response, or completed for a personal to-do event.
   */
  public enum ResponseStatusEnum {
    NEEDS_ACTION("needs_action"),
    
    ACCEPTED("accepted"),
    
    TENTATIVE("tentative"),
    
    DECLINED("declined"),
    
    COMPLETED("completed");

    private final String value;

    ResponseStatusEnum(String value) {
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
    public static ResponseStatusEnum fromValue(String value) {
      for (ResponseStatusEnum b : ResponseStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ResponseStatusEnum responseStatus;

  /**
   * Importance (personal events).
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
  private @Nullable ImportanceEnum importance;

  public EventOccurrenceUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Attendee whose personal copy this is (personal events shared with several users).
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public EventOccurrenceUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort of a cohort event.
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

  public EventOccurrenceUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title override.
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

  public EventOccurrenceUpdate color(@Nullable String color) {
    this.color = color;
    return this;
  }

  /**
   * Colour override.
   * @return color
   */
  @Size(max = 20) 
  @JsonProperty("color")
  public @Nullable String getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable String color) {
    this.color = color;
  }

  public EventOccurrenceUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description override.
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

  public EventOccurrenceUpdate startAt(@Nullable OffsetDateTime startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Start.
   * @return startAt
   */
  @Valid 
  @JsonProperty("startAt")
  public @Nullable OffsetDateTime getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(@Nullable OffsetDateTime startAt) {
    this.startAt = startAt;
  }

  public EventOccurrenceUpdate endAt(@Nullable OffsetDateTime endAt) {
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

  public EventOccurrenceUpdate isCancelled(@Nullable Boolean isCancelled) {
    this.isCancelled = isCancelled;
    return this;
  }

  /**
   * Occurrence cancelled.
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

  public EventOccurrenceUpdate responseStatus(@Nullable ResponseStatusEnum responseStatus) {
    this.responseStatus = responseStatus;
    return this;
  }

  /**
   * Attendee's response, or completed for a personal to-do event.
   * @return responseStatus
   */
  
  @JsonProperty("responseStatus")
  public @Nullable ResponseStatusEnum getResponseStatus() {
    return responseStatus;
  }

  @JsonProperty("responseStatus")
  public void setResponseStatus(@Nullable ResponseStatusEnum responseStatus) {
    this.responseStatus = responseStatus;
  }

  public EventOccurrenceUpdate importance(@Nullable ImportanceEnum importance) {
    this.importance = importance;
    return this;
  }

  /**
   * Importance (personal events).
   * @return importance
   */
  
  @JsonProperty("importance")
  public @Nullable ImportanceEnum getImportance() {
    return importance;
  }

  @JsonProperty("importance")
  public void setImportance(@Nullable ImportanceEnum importance) {
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
    EventOccurrenceUpdate eventOccurrenceUpdate = (EventOccurrenceUpdate) o;
    return Objects.equals(this.userId, eventOccurrenceUpdate.userId) &&
        Objects.equals(this.cohortId, eventOccurrenceUpdate.cohortId) &&
        Objects.equals(this.title, eventOccurrenceUpdate.title) &&
        Objects.equals(this.color, eventOccurrenceUpdate.color) &&
        Objects.equals(this.description, eventOccurrenceUpdate.description) &&
        Objects.equals(this.startAt, eventOccurrenceUpdate.startAt) &&
        Objects.equals(this.endAt, eventOccurrenceUpdate.endAt) &&
        Objects.equals(this.isCancelled, eventOccurrenceUpdate.isCancelled) &&
        Objects.equals(this.responseStatus, eventOccurrenceUpdate.responseStatus) &&
        Objects.equals(this.importance, eventOccurrenceUpdate.importance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, cohortId, title, color, description, startAt, endAt, isCancelled, responseStatus, importance);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventOccurrenceUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    startAt: ").append(toIndentedString(startAt)).append("\n");
    sb.append("    endAt: ").append(toIndentedString(endAt)).append("\n");
    sb.append("    isCancelled: ").append(toIndentedString(isCancelled)).append("\n");
    sb.append("    responseStatus: ").append(toIndentedString(responseStatus)).append("\n");
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

