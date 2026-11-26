// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ItemOverride that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ItemOverrideUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> availableFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> extraAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> extraTimeMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> reason;

  public ItemOverrideUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
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

  public ItemOverrideUpdate groupId(@Nullable Optional<UUID> groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group.
   * @return groupId
   */
  
  @JsonProperty("groupId")
  public @Nullable Optional<UUID> getGroupId() {
    return groupId;
  }

  @JsonProperty("groupId")
  public void setGroupId(@Nullable Optional<UUID> groupId) {
    this.groupId = groupId;
  }

  public ItemOverrideUpdate availableFrom(@Nullable Optional<OffsetDateTime> availableFrom) {
    this.availableFrom = availableFrom;
    return this;
  }

  /**
   * Opens for this student.
   * @return availableFrom
   */
  
  @JsonProperty("availableFrom")
  public @Nullable Optional<OffsetDateTime> getAvailableFrom() {
    return availableFrom;
  }

  @JsonProperty("availableFrom")
  public void setAvailableFrom(@Nullable Optional<OffsetDateTime> availableFrom) {
    this.availableFrom = availableFrom;
  }

  public ItemOverrideUpdate dueAt(@Nullable Optional<OffsetDateTime> dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Due for this student.
   * @return dueAt
   */
  
  @JsonProperty("dueAt")
  public @Nullable Optional<OffsetDateTime> getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable Optional<OffsetDateTime> dueAt) {
    this.dueAt = dueAt;
  }

  public ItemOverrideUpdate extraAttempts(@Nullable Optional<Integer> extraAttempts) {
    this.extraAttempts = extraAttempts;
    return this;
  }

  /**
   * Additional attempts allowed.
   * @return extraAttempts
   */
  
  @JsonProperty("extraAttempts")
  public @Nullable Optional<Integer> getExtraAttempts() {
    return extraAttempts;
  }

  @JsonProperty("extraAttempts")
  public void setExtraAttempts(@Nullable Optional<Integer> extraAttempts) {
    this.extraAttempts = extraAttempts;
  }

  public ItemOverrideUpdate extraTimeMinutes(@Nullable Optional<Integer> extraTimeMinutes) {
    this.extraTimeMinutes = extraTimeMinutes;
    return this;
  }

  /**
   * Additional time per timed attempt.
   * @return extraTimeMinutes
   */
  
  @JsonProperty("extraTimeMinutes")
  public @Nullable Optional<Integer> getExtraTimeMinutes() {
    return extraTimeMinutes;
  }

  @JsonProperty("extraTimeMinutes")
  public void setExtraTimeMinutes(@Nullable Optional<Integer> extraTimeMinutes) {
    this.extraTimeMinutes = extraTimeMinutes;
  }

  public ItemOverrideUpdate reason(@Nullable Optional<String> reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Reason (extension, accommodation...).
   * @return reason
   */
  
  @JsonProperty("reason")
  public @Nullable Optional<String> getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable Optional<String> reason) {
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

