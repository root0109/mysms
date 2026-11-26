// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of TaskAssignee that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class TaskAssigneeUpdate {

  /**
   * Assignee kind.
   */
  public enum AssigneeTypeEnum {
    USER("user"),
    
    propertyClass("class"),
    
    GROUP("group");

    private final String value;

    AssigneeTypeEnum(String value) {
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
    public static AssigneeTypeEnum fromValue(String value) {
      for (AssigneeTypeEnum b : AssigneeTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull AssigneeTypeEnum> assigneeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> claimedByUserId;

  /**
   * State.
   */
  public enum StatusEnum {
    PENDING("pending"),
    
    COMPLETED("completed");

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
  private @Nullable Optional<@NotNull StatusEnum> status;

  public TaskAssigneeUpdate assigneeType(@Nullable Optional<AssigneeTypeEnum> assigneeType) {
    this.assigneeType = assigneeType;
    return this;
  }

  /**
   * Assignee kind.
   * @return assigneeType
   */
  
  @JsonProperty("assigneeType")
  public @Nullable Optional<AssigneeTypeEnum> getAssigneeType() {
    return assigneeType;
  }

  @JsonProperty("assigneeType")
  public void setAssigneeType(@Nullable Optional<AssigneeTypeEnum> assigneeType) {
    this.assigneeType = assigneeType;
  }

  public TaskAssigneeUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User.
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

  public TaskAssigneeUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class.
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

  public TaskAssigneeUpdate groupId(@Nullable Optional<UUID> groupId) {
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

  public TaskAssigneeUpdate claimedByUserId(@Nullable Optional<UUID> claimedByUserId) {
    this.claimedByUserId = claimedByUserId;
    return this;
  }

  /**
   * User who claimed an open task.
   * @return claimedByUserId
   */
  
  @JsonProperty("claimedByUserId")
  public @Nullable Optional<UUID> getClaimedByUserId() {
    return claimedByUserId;
  }

  @JsonProperty("claimedByUserId")
  public void setClaimedByUserId(@Nullable Optional<UUID> claimedByUserId) {
    this.claimedByUserId = claimedByUserId;
  }

  public TaskAssigneeUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * State.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskAssigneeUpdate taskAssigneeUpdate = (TaskAssigneeUpdate) o;
    return Objects.equals(this.assigneeType, taskAssigneeUpdate.assigneeType) &&
        Objects.equals(this.userId, taskAssigneeUpdate.userId) &&
        Objects.equals(this.classSectionId, taskAssigneeUpdate.classSectionId) &&
        Objects.equals(this.groupId, taskAssigneeUpdate.groupId) &&
        Objects.equals(this.claimedByUserId, taskAssigneeUpdate.claimedByUserId) &&
        Objects.equals(this.status, taskAssigneeUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assigneeType, userId, classSectionId, groupId, claimedByUserId, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskAssigneeUpdate {\n");
    sb.append("    assigneeType: ").append(toIndentedString(assigneeType)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    claimedByUserId: ").append(toIndentedString(claimedByUserId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

