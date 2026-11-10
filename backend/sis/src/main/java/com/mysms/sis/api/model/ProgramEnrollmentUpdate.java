// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * Fields of ProgramEnrollment that can be updated (partial update; see updateMask).
 */

public class ProgramEnrollmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String rollNumber;

  /**
   * Registration state.
   */
  public enum StatusEnum {
    RUNNING("running"),
    
    FINISHED("finished"),
    
    DROPPED("dropped");

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
  private @Nullable StatusEnum status;

  public ProgramEnrollmentUpdate studentId(@Nullable UUID studentId) {
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

  public ProgramEnrollmentUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public ProgramEnrollmentUpdate batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch.
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public ProgramEnrollmentUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public ProgramEnrollmentUpdate academicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Academic term.
   * @return academicTermId
   */
  @Valid 
  @JsonProperty("academicTermId")
  public @Nullable UUID getAcademicTermId() {
    return academicTermId;
  }

  @JsonProperty("academicTermId")
  public void setAcademicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
  }

  public ProgramEnrollmentUpdate rollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
    return this;
  }

  /**
   * Roll number in the batch.
   * @return rollNumber
   */
  @Size(max = 20) 
  @JsonProperty("rollNumber")
  public @Nullable String getRollNumber() {
    return rollNumber;
  }

  @JsonProperty("rollNumber")
  public void setRollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
  }

  public ProgramEnrollmentUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Registration state.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
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
    ProgramEnrollmentUpdate programEnrollmentUpdate = (ProgramEnrollmentUpdate) o;
    return Objects.equals(this.studentId, programEnrollmentUpdate.studentId) &&
        Objects.equals(this.courseId, programEnrollmentUpdate.courseId) &&
        Objects.equals(this.batchId, programEnrollmentUpdate.batchId) &&
        Objects.equals(this.academicYearId, programEnrollmentUpdate.academicYearId) &&
        Objects.equals(this.academicTermId, programEnrollmentUpdate.academicTermId) &&
        Objects.equals(this.rollNumber, programEnrollmentUpdate.rollNumber) &&
        Objects.equals(this.status, programEnrollmentUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, courseId, batchId, academicYearId, academicTermId, rollNumber, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramEnrollmentUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    academicTermId: ").append(toIndentedString(academicTermId)).append("\n");
    sb.append("    rollNumber: ").append(toIndentedString(rollNumber)).append("\n");
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

