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
 * Fields of EventOccurrence that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class EventOccurrenceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isCancelled;

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
  private @Nullable Optional<ResponseStatusEnum> responseStatus;

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
  private @Nullable Optional<ImportanceEnum> importance;

  public EventOccurrenceUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Attendee whose personal copy this is (personal events shared with several users).
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public EventOccurrenceUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort of a cohort event.
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

  public EventOccurrenceUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title override.
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

  public EventOccurrenceUpdate color(@Nullable Optional<String> color) {
    this.color = color;
    return this;
  }

  /**
   * Colour override.
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

  public EventOccurrenceUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description override.
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

  public EventOccurrenceUpdate startAt(@Nullable Optional<OffsetDateTime> startAt) {
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

  public EventOccurrenceUpdate endAt(@Nullable Optional<OffsetDateTime> endAt) {
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

  public EventOccurrenceUpdate isCancelled(@Nullable Optional<Boolean> isCancelled) {
    this.isCancelled = isCancelled;
    return this;
  }

  /**
   * Occurrence cancelled.
   * @return isCancelled
   */
  
  @JsonProperty("isCancelled")
  public @Nullable Optional<Boolean> getIsCancelled() {
    return isCancelled;
  }

  @JsonProperty("isCancelled")
  public void setIsCancelled(@Nullable Optional<Boolean> isCancelled) {
    this.isCancelled = isCancelled;
  }

  public EventOccurrenceUpdate responseStatus(@Nullable Optional<ResponseStatusEnum> responseStatus) {
    this.responseStatus = responseStatus;
    return this;
  }

  /**
   * Attendee's response, or completed for a personal to-do event.
   * @return responseStatus
   */
  
  @JsonProperty("responseStatus")
  public @Nullable Optional<ResponseStatusEnum> getResponseStatus() {
    return responseStatus;
  }

  @JsonProperty("responseStatus")
  public void setResponseStatus(@Nullable Optional<ResponseStatusEnum> responseStatus) {
    this.responseStatus = responseStatus;
  }

  public EventOccurrenceUpdate importance(@Nullable Optional<ImportanceEnum> importance) {
    this.importance = importance;
    return this;
  }

  /**
   * Importance (personal events).
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

