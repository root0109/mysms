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
 * To-do / task created by a user for self or others.
 */

public class Task {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  /**
   * Who the task is for.
   */
  public enum TaskCategoryEnum {
    SELF("self"),
    
    STUDENT("student"),
    
    propertyClass("class"),
    
    GROUP("group"),
    
    TEACHER("teacher"),
    
    NON_TEACHING("non_teaching");

    private final String value;

    TaskCategoryEnum(String value) {
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
    public static TaskCategoryEnum fromValue(String value) {
      for (TaskCategoryEnum b : TaskCategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private TaskCategoryEnum taskCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dueAt;

  /**
   * Reminder rule.
   */
  public enum ReminderOptionEnum {
    ONE_HOUR_BEFORE("one_hour_before"),
    
    ONE_DAY_BEFORE("one_day_before"),
    
    TWO_DAYS_BEFORE("two_days_before"),
    
    CUSTOM("custom");

    private final String value;

    ReminderOptionEnum(String value) {
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
    public static ReminderOptionEnum fromValue(String value) {
      for (ReminderOptionEnum b : ReminderOptionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ReminderOptionEnum reminderOption;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime reminderAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String checklist;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isCompleted;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isSnoozed;

  /**
   * Priority.
   */
  public enum PriorityEnum {
    LOW("low"),
    
    NORMAL("normal"),
    
    HIGH("high");

    private final String value;

    PriorityEnum(String value) {
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
    public static PriorityEnum fromValue(String value) {
      for (PriorityEnum b : PriorityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable PriorityEnum priority;

  /**
   * Individual, shared or open-to-claim.
   */
  public enum AssignmentModeEnum {
    INDIVIDUAL("individual"),
    
    GROUP_TASK("group_task"),
    
    OPEN("open");

    private final String value;

    AssignmentModeEnum(String value) {
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
    public static AssignmentModeEnum fromValue(String value) {
      for (AssignmentModeEnum b : AssignmentModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AssignmentModeEnum assignmentMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Task() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Task(UUID institutionId, UUID ownerUserId, String title, TaskCategoryEnum taskCategory, OffsetDateTime dueAt, ReminderOptionEnum reminderOption, String checklist) {
    this.institutionId = institutionId;
    this.ownerUserId = ownerUserId;
    this.title = title;
    this.taskCategory = taskCategory;
    this.dueAt = dueAt;
    this.reminderOption = reminderOption;
    this.checklist = checklist;
  }

  public Task id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public Task institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public Task academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public Task ownerUserId(UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Creator / owner.
   * @return ownerUserId
   */
  @NotNull @Valid 
  @JsonProperty("ownerUserId")
  public UUID getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public Task title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @NotNull @Size(max = 255) 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public Task taskCategory(TaskCategoryEnum taskCategory) {
    this.taskCategory = taskCategory;
    return this;
  }

  /**
   * Who the task is for.
   * @return taskCategory
   */
  @NotNull 
  @JsonProperty("taskCategory")
  public TaskCategoryEnum getTaskCategory() {
    return taskCategory;
  }

  @JsonProperty("taskCategory")
  public void setTaskCategory(TaskCategoryEnum taskCategory) {
    this.taskCategory = taskCategory;
  }

  public Task dueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Due date-time.
   * @return dueAt
   */
  @NotNull @Valid 
  @JsonProperty("dueAt")
  public OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public Task reminderOption(ReminderOptionEnum reminderOption) {
    this.reminderOption = reminderOption;
    return this;
  }

  /**
   * Reminder rule.
   * @return reminderOption
   */
  @NotNull 
  @JsonProperty("reminderOption")
  public ReminderOptionEnum getReminderOption() {
    return reminderOption;
  }

  @JsonProperty("reminderOption")
  public void setReminderOption(ReminderOptionEnum reminderOption) {
    this.reminderOption = reminderOption;
  }

  public Task reminderAt(@Nullable OffsetDateTime reminderAt) {
    this.reminderAt = reminderAt;
    return this;
  }

  /**
   * Reminder time.
   * @return reminderAt
   */
  @Valid 
  @JsonProperty("reminderAt")
  public @Nullable OffsetDateTime getReminderAt() {
    return reminderAt;
  }

  @JsonProperty("reminderAt")
  public void setReminderAt(@Nullable OffsetDateTime reminderAt) {
    this.reminderAt = reminderAt;
  }

  public Task checklist(String checklist) {
    this.checklist = checklist;
    return this;
  }

  /**
   * Checklist / details.
   * @return checklist
   */
  @NotNull 
  @JsonProperty("checklist")
  public String getChecklist() {
    return checklist;
  }

  @JsonProperty("checklist")
  public void setChecklist(String checklist) {
    this.checklist = checklist;
  }

  public Task isCompleted(@Nullable Boolean isCompleted) {
    this.isCompleted = isCompleted;
    return this;
  }

  /**
   * Completed. Changed only through the workflow operations.
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

  public Task isSnoozed(@Nullable Boolean isSnoozed) {
    this.isSnoozed = isSnoozed;
    return this;
  }

  /**
   * Snoozed. Changed only through the workflow operations.
   * @return isSnoozed
   */
  
  @JsonProperty("isSnoozed")
  public @Nullable Boolean getIsSnoozed() {
    return isSnoozed;
  }

  @JsonProperty("isSnoozed")
  public void setIsSnoozed(@Nullable Boolean isSnoozed) {
    this.isSnoozed = isSnoozed;
  }

  public Task priority(@Nullable PriorityEnum priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Priority.
   * @return priority
   */
  
  @JsonProperty("priority")
  public @Nullable PriorityEnum getPriority() {
    return priority;
  }

  @JsonProperty("priority")
  public void setPriority(@Nullable PriorityEnum priority) {
    this.priority = priority;
  }

  public Task assignmentMode(@Nullable AssignmentModeEnum assignmentMode) {
    this.assignmentMode = assignmentMode;
    return this;
  }

  /**
   * Individual, shared or open-to-claim.
   * @return assignmentMode
   */
  
  @JsonProperty("assignmentMode")
  public @Nullable AssignmentModeEnum getAssignmentMode() {
    return assignmentMode;
  }

  @JsonProperty("assignmentMode")
  public void setAssignmentMode(@Nullable AssignmentModeEnum assignmentMode) {
    this.assignmentMode = assignmentMode;
  }

  public Task createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Task createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Task updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Task updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Task etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Task task = (Task) o;
    return Objects.equals(this.id, task.id) &&
        Objects.equals(this.institutionId, task.institutionId) &&
        Objects.equals(this.academicYearId, task.academicYearId) &&
        Objects.equals(this.ownerUserId, task.ownerUserId) &&
        Objects.equals(this.title, task.title) &&
        Objects.equals(this.taskCategory, task.taskCategory) &&
        Objects.equals(this.dueAt, task.dueAt) &&
        Objects.equals(this.reminderOption, task.reminderOption) &&
        Objects.equals(this.reminderAt, task.reminderAt) &&
        Objects.equals(this.checklist, task.checklist) &&
        Objects.equals(this.isCompleted, task.isCompleted) &&
        Objects.equals(this.isSnoozed, task.isSnoozed) &&
        Objects.equals(this.priority, task.priority) &&
        Objects.equals(this.assignmentMode, task.assignmentMode) &&
        Objects.equals(this.createTime, task.createTime) &&
        Objects.equals(this.createdBy, task.createdBy) &&
        Objects.equals(this.updateTime, task.updateTime) &&
        Objects.equals(this.updatedBy, task.updatedBy) &&
        Objects.equals(this.etag, task.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, institutionId, academicYearId, ownerUserId, title, taskCategory, dueAt, reminderOption, reminderAt, checklist, isCompleted, isSnoozed, priority, assignmentMode, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Task {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    taskCategory: ").append(toIndentedString(taskCategory)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    reminderOption: ").append(toIndentedString(reminderOption)).append("\n");
    sb.append("    reminderAt: ").append(toIndentedString(reminderAt)).append("\n");
    sb.append("    checklist: ").append(toIndentedString(checklist)).append("\n");
    sb.append("    isCompleted: ").append(toIndentedString(isCompleted)).append("\n");
    sb.append("    isSnoozed: ").append(toIndentedString(isSnoozed)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    assignmentMode: ").append(toIndentedString(assignmentMode)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

