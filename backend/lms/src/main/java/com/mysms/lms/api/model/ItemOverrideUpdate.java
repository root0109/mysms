// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ItemOverride that can be updated (partial update; see updateMask).
 */

public class ItemOverrideUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime availableFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer extraAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer extraTimeMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reason;

  public ItemOverrideUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
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

  public ItemOverrideUpdate groupId(@Nullable UUID groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group.
   * @return groupId
   */
  @Valid 
  @JsonProperty("groupId")
  public @Nullable UUID getGroupId() {
    return groupId;
  }

  @JsonProperty("groupId")
  public void setGroupId(@Nullable UUID groupId) {
    this.groupId = groupId;
  }

  public ItemOverrideUpdate availableFrom(@Nullable OffsetDateTime availableFrom) {
    this.availableFrom = availableFrom;
    return this;
  }

  /**
   * Opens for this student.
   * @return availableFrom
   */
  @Valid 
  @JsonProperty("availableFrom")
  public @Nullable OffsetDateTime getAvailableFrom() {
    return availableFrom;
  }

  @JsonProperty("availableFrom")
  public void setAvailableFrom(@Nullable OffsetDateTime availableFrom) {
    this.availableFrom = availableFrom;
  }

  public ItemOverrideUpdate dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Due for this student.
   * @return dueAt
   */
  @Valid 
  @JsonProperty("dueAt")
  public @Nullable OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public ItemOverrideUpdate extraAttempts(@Nullable Integer extraAttempts) {
    this.extraAttempts = extraAttempts;
    return this;
  }

  /**
   * Additional attempts allowed.
   * @return extraAttempts
   */
  
  @JsonProperty("extraAttempts")
  public @Nullable Integer getExtraAttempts() {
    return extraAttempts;
  }

  @JsonProperty("extraAttempts")
  public void setExtraAttempts(@Nullable Integer extraAttempts) {
    this.extraAttempts = extraAttempts;
  }

  public ItemOverrideUpdate extraTimeMinutes(@Nullable Integer extraTimeMinutes) {
    this.extraTimeMinutes = extraTimeMinutes;
    return this;
  }

  /**
   * Additional time per timed attempt.
   * @return extraTimeMinutes
   */
  
  @JsonProperty("extraTimeMinutes")
  public @Nullable Integer getExtraTimeMinutes() {
    return extraTimeMinutes;
  }

  @JsonProperty("extraTimeMinutes")
  public void setExtraTimeMinutes(@Nullable Integer extraTimeMinutes) {
    this.extraTimeMinutes = extraTimeMinutes;
  }

  public ItemOverrideUpdate reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Reason (extension, accommodation...).
   * @return reason
   */
  @Size(max = 200) 
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItemOverrideUpdate itemOverrideUpdate = (ItemOverrideUpdate) o;
    return Objects.equals(this.userId, itemOverrideUpdate.userId) &&
        Objects.equals(this.groupId, itemOverrideUpdate.groupId) &&
        Objects.equals(this.availableFrom, itemOverrideUpdate.availableFrom) &&
        Objects.equals(this.dueAt, itemOverrideUpdate.dueAt) &&
        Objects.equals(this.extraAttempts, itemOverrideUpdate.extraAttempts) &&
        Objects.equals(this.extraTimeMinutes, itemOverrideUpdate.extraTimeMinutes) &&
        Objects.equals(this.reason, itemOverrideUpdate.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, groupId, availableFrom, dueAt, extraAttempts, extraTimeMinutes, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ItemOverrideUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    availableFrom: ").append(toIndentedString(availableFrom)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    extraAttempts: ").append(toIndentedString(extraAttempts)).append("\n");
    sb.append("    extraTimeMinutes: ").append(toIndentedString(extraTimeMinutes)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

