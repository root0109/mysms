// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * Fields of StudentActivity that can be updated (partial update; see updateMask).
 */

public class StudentActivityUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID activityTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate activityDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  public StudentActivityUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public StudentActivityUpdate staffId(@Nullable UUID staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member who logged / supervised.
   * @return staffId
   */
  @Valid 
  @JsonProperty("staffId")
  public @Nullable UUID getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable UUID staffId) {
    this.staffId = staffId;
  }

  public StudentActivityUpdate activityTypeId(@Nullable UUID activityTypeId) {
    this.activityTypeId = activityTypeId;
    return this;
  }

  /**
   * Type.
   * @return activityTypeId
   */
  @Valid 
  @JsonProperty("activityTypeId")
  public @Nullable UUID getActivityTypeId() {
    return activityTypeId;
  }

  @JsonProperty("activityTypeId")
  public void setActivityTypeId(@Nullable UUID activityTypeId) {
    this.activityTypeId = activityTypeId;
  }

  public StudentActivityUpdate activityDate(@Nullable LocalDate activityDate) {
    this.activityDate = activityDate;
    return this;
  }

  /**
   * Date.
   * @return activityDate
   */
  @Valid 
  @JsonProperty("activityDate")
  public @Nullable LocalDate getActivityDate() {
    return activityDate;
  }

  @JsonProperty("activityDate")
  public void setActivityDate(@Nullable LocalDate activityDate) {
    this.activityDate = activityDate;
  }

  public StudentActivityUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

