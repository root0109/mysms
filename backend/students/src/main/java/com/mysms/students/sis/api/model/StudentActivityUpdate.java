// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of StudentActivity that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StudentActivityUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> activityTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> activityDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  public StudentActivityUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public StudentActivityUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member who logged / supervised.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public StudentActivityUpdate activityTypeId(@Nullable Optional<UUID> activityTypeId) {
    this.activityTypeId = activityTypeId;
    return this;
  }

  /**
   * Type.
   * @return activityTypeId
   */
  
  @JsonProperty("activityTypeId")
  public @Nullable Optional<UUID> getActivityTypeId() {
    return activityTypeId;
  }

  @JsonProperty("activityTypeId")
  public void setActivityTypeId(@Nullable Optional<UUID> activityTypeId) {
    this.activityTypeId = activityTypeId;
  }

  public StudentActivityUpdate activityDate(@Nullable Optional<LocalDate> activityDate) {
    this.activityDate = activityDate;
    return this;
  }

  /**
   * Date.
   * @return activityDate
   */
  
  @JsonProperty("activityDate")
  public @Nullable Optional<LocalDate> getActivityDate() {
    return activityDate;
  }

  @JsonProperty("activityDate")
  public void setActivityDate(@Nullable Optional<LocalDate> activityDate) {
    this.activityDate = activityDate;
  }

  public StudentActivityUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentActivityUpdate studentActivityUpdate = (StudentActivityUpdate) o;
    return Objects.equals(this.studentId, studentActivityUpdate.studentId) &&
        Objects.equals(this.staffId, studentActivityUpdate.staffId) &&
        Objects.equals(this.activityTypeId, studentActivityUpdate.activityTypeId) &&
        Objects.equals(this.activityDate, studentActivityUpdate.activityDate) &&
        Objects.equals(this.description, studentActivityUpdate.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, staffId, activityTypeId, activityDate, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentActivityUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    activityTypeId: ").append(toIndentedString(activityTypeId)).append("\n");
    sb.append("    activityDate: ").append(toIndentedString(activityDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

