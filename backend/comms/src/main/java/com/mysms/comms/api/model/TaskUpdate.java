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
 * Fields of Task that can be updated (partial update; see updateMask).
 */

public class TaskUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

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
  private @Nullable TaskCategoryEnum taskCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

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
  private @Nullable ReminderOptionEnum reminderOption;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime reminderAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String checklist;

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

  public TaskUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public TaskUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public TaskUpdate ownerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Creator / owner.
   * @return ownerUserId
   */
  @Valid 
  @JsonProperty("ownerUserId")
  public @Nullable UUID getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public TaskUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public TaskUpdate taskCategory(@Nullable TaskCategoryEnum taskCategory) {
    this.taskCategory = taskCategory;
    return this;
  }

  /**
   * Who the task is for.
   * @return taskCategory
   */
  
  @JsonProperty("taskCategory")
  public @Nullable TaskCategoryEnum getTaskCategory() {
    return taskCategory;
  }

  @JsonProperty("taskCategory")
  public void setTaskCategory(@Nullable TaskCategoryEnum taskCategory) {
    this.taskCategory = taskCategory;
  }

  public TaskUpdate dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Due date-time.
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

  public TaskUpdate reminderOption(@Nullable ReminderOptionEnum reminderOption) {
    this.reminderOption = reminderOption;
    return this;
  }

  /**
   * Reminder rule.
   * @return reminderOption
   */
  
  @JsonProperty("reminderOption")
  public @Nullable ReminderOptionEnum getReminderOption() {
    return reminderOption;
  }

  @JsonProperty("reminderOption")
  public void setReminderOption(@Nullable ReminderOptionEnum reminderOption) {
    this.reminderOption = reminderOption;
  }

  public TaskUpdate reminderAt(@Nullable OffsetDateTime reminderAt) {
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

  public TaskUpdate checklist(@Nullable String checklist) {
    this.checklist = checklist;
    return this;
  }

  /**
   * Checklist / details.
   * @return checklist
   */
  
  @JsonProperty("checklist")
  public @Nullable String getChecklist() {
    return checklist;
  }

  @JsonProperty("checklist")
  public void setChecklist(@Nullable String checklist) {
    this.checklist = checklist;
  }

  public TaskUpdate priority(@Nullable PriorityEnum priority) {
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

  public TaskUpdate assignmentMode(@Nullable AssignmentModeEnum assignmentMode) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskUpdate taskUpdate = (TaskUpdate) o;
    return Objects.equals(this.institutionId, taskUpdate.institutionId) &&
        Objects.equals(this.academicYearId, taskUpdate.academicYearId) &&
        Objects.equals(this.ownerUserId, taskUpdate.ownerUserId) &&
        Objects.equals(this.title, taskUpdate.title) &&
        Objects.equals(this.taskCategory, taskUpdate.taskCategory) &&
        Objects.equals(this.dueAt, taskUpdate.dueAt) &&
        Objects.equals(this.reminderOption, taskUpdate.reminderOption) &&
        Objects.equals(this.reminderAt, taskUpdate.reminderAt) &&
        Objects.equals(this.checklist, taskUpdate.checklist) &&
        Objects.equals(this.priority, taskUpdate.priority) &&
        Objects.equals(this.assignmentMode, taskUpdate.assignmentMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, ownerUserId, title, taskCategory, dueAt, reminderOption, reminderAt, checklist, priority, assignmentMode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    taskCategory: ").append(toIndentedString(taskCategory)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    reminderOption: ").append(toIndentedString(reminderOption)).append("\n");
    sb.append("    reminderAt: ").append(toIndentedString(reminderAt)).append("\n");
    sb.append("    checklist: ").append(toIndentedString(checklist)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    assignmentMode: ").append(toIndentedString(assignmentMode)).append("\n");
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

