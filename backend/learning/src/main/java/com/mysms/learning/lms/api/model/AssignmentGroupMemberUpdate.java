// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AssignmentGroupMember that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AssignmentGroupMemberUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> assignmentGroupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> contentItemId;

  public AssignmentGroupMemberUpdate assignmentGroupId(@Nullable Optional<UUID> assignmentGroupId) {
    this.assignmentGroupId = assignmentGroupId;
    return this;
  }

  /**
   * Group.
   * @return assignmentGroupId
   */
  
  @JsonProperty("assignmentGroupId")
  public @Nullable Optional<UUID> getAssignmentGroupId() {
    return assignmentGroupId;
  }

  @JsonProperty("assignmentGroupId")
  public void setAssignmentGroupId(@Nullable Optional<UUID> assignmentGroupId) {
    this.assignmentGroupId = assignmentGroupId;
  }

  public AssignmentGroupMemberUpdate userId(@Nullable Optional<UUID> userId) {
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

  public AssignmentGroupMemberUpdate contentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Assignment placement (one group per student per placement).
   * @return contentItemId
   */
  
  @JsonProperty("contentItemId")
  public @Nullable Optional<UUID> getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignmentGroupMemberUpdate assignmentGroupMemberUpdate = (AssignmentGroupMemberUpdate) o;
    return Objects.equals(this.assignmentGroupId, assignmentGroupMemberUpdate.assignmentGroupId) &&
        Objects.equals(this.userId, assignmentGroupMemberUpdate.userId) &&
        Objects.equals(this.contentItemId, assignmentGroupMemberUpdate.contentItemId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignmentGroupId, userId, contentItemId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignmentGroupMemberUpdate {\n");
    sb.append("    assignmentGroupId: ").append(toIndentedString(assignmentGroupId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
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

