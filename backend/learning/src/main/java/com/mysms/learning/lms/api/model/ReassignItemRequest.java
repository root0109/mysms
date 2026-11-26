// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ReassignItemRequest
 */

public class ReassignItemRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> userIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> groupIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime availableFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String comment;

  public ReassignItemRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public ReassignItemRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Students.
   * @return userIds
   */
  
  @JsonProperty("userIds")
  public List<UUID> getUserIds() {
    return userIds;
  }

  @JsonProperty("userIds")
  public void setUserIds(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public ReassignItemRequest groupIds(List<UUID> groupIds) {
    this.groupIds = groupIds;
    return this;
  }

  public ReassignItemRequest addGroupIdsItem(UUID groupIdsItem) {
    if (this.groupIds == null) {
      this.groupIds = new ArrayList<>();
    }
    this.groupIds.add(groupIdsItem);
    return this;
  }

  /**
   * Groups.
   * @return groupIds
   */
  
  @JsonProperty("groupIds")
  public List<UUID> getGroupIds() {
    return groupIds;
  }

  @JsonProperty("groupIds")
  public void setGroupIds(List<UUID> groupIds) {
    this.groupIds = groupIds;
  }

  public ReassignItemRequest availableFrom(@Nullable OffsetDateTime availableFrom) {
    this.availableFrom = availableFrom;
    return this;
  }

  /**
   * New window start.
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

  public ReassignItemRequest dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * New due date.
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

  public ReassignItemRequest comment(@Nullable String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Reason.
   * @return comment
   */
  
  @JsonProperty("comment")
  public @Nullable String getComment() {
    return comment;
  }

  @JsonProperty("comment")
  public void setComment(@Nullable String comment) {
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReassignItemRequest reassignItemRequest = (ReassignItemRequest) o;
    return Objects.equals(this.userIds, reassignItemRequest.userIds) &&
        Objects.equals(this.groupIds, reassignItemRequest.groupIds) &&
        Objects.equals(this.availableFrom, reassignItemRequest.availableFrom) &&
        Objects.equals(this.dueAt, reassignItemRequest.dueAt) &&
        Objects.equals(this.comment, reassignItemRequest.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds, groupIds, availableFrom, dueAt, comment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReassignItemRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    groupIds: ").append(toIndentedString(groupIds)).append("\n");
    sb.append("    availableFrom: ").append(toIndentedString(availableFrom)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
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

