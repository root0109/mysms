// Owned API contract of sis.
package com.mysms.students.sis.api.model;

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
 * Fields of SubjectRegistration that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SubjectRegistrationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> referenceNo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> minUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> maxUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> readByStudent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> readByGuardian;

  public SubjectRegistrationUpdate studentId(@Nullable Optional<UUID> studentId) {
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

  public SubjectRegistrationUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
   * @return courseId
   */
  
  @JsonProperty("courseId")
  public @Nullable Optional<UUID> getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
  }

  public SubjectRegistrationUpdate batchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch.
   * @return batchId
   */
  
  @JsonProperty("batchId")
  public @Nullable Optional<UUID> getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
  }

  public SubjectRegistrationUpdate referenceNo(@Nullable Optional<String> referenceNo) {
    this.referenceNo = referenceNo;
    return this;
  }

  /**
   * Registration reference.
   * @return referenceNo
   */
  
  @JsonProperty("referenceNo")
  public @Nullable Optional<String> getReferenceNo() {
    return referenceNo;
  }

  @JsonProperty("referenceNo")
  public void setReferenceNo(@Nullable Optional<String> referenceNo) {
    this.referenceNo = referenceNo;
  }

  public SubjectRegistrationUpdate minUnitLoad(@Nullable Optional<String> minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
    return this;
  }

  /**
   * Minimum credits.
   * @return minUnitLoad
   */
  
  @JsonProperty("minUnitLoad")
  public @Nullable Optional<String> getMinUnitLoad() {
    return minUnitLoad;
  }

  @JsonProperty("minUnitLoad")
  public void setMinUnitLoad(@Nullable Optional<String> minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
  }

  public SubjectRegistrationUpdate maxUnitLoad(@Nullable Optional<String> maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
    return this;
  }

  /**
   * Maximum credits.
   * @return maxUnitLoad
   */
  
  @JsonProperty("maxUnitLoad")
  public @Nullable Optional<String> getMaxUnitLoad() {
    return maxUnitLoad;
  }

  @JsonProperty("maxUnitLoad")
  public void setMaxUnitLoad(@Nullable Optional<String> maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
  }

  public SubjectRegistrationUpdate readByStudent(@Nullable Optional<Boolean> readByStudent) {
    this.readByStudent = readByStudent;
    return this;
  }

  /**
   * Student has seen the decision.
   * @return readByStudent
   */
  
  @JsonProperty("readByStudent")
  public @Nullable Optional<Boolean> getReadByStudent() {
    return readByStudent;
  }

  @JsonProperty("readByStudent")
  public void setReadByStudent(@Nullable Optional<Boolean> readByStudent) {
    this.readByStudent = readByStudent;
  }

  public SubjectRegistrationUpdate readByGuardian(@Nullable Optional<Boolean> readByGuardian) {
    this.readByGuardian = readByGuardian;
    return this;
  }

  /**
   * Guardian has seen the decision.
   * @return readByGuardian
   */
  
  @JsonProperty("readByGuardian")
  public @Nullable Optional<Boolean> getReadByGuardian() {
    return readByGuardian;
  }

  @JsonProperty("readByGuardian")
  public void setReadByGuardian(@Nullable Optional<Boolean> readByGuardian) {
    this.readByGuardian = readByGuardian;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubjectRegistrationUpdate subjectRegistrationUpdate = (SubjectRegistrationUpdate) o;
    return Objects.equals(this.studentId, subjectRegistrationUpdate.studentId) &&
        Objects.equals(this.courseId, subjectRegistrationUpdate.courseId) &&
        Objects.equals(this.batchId, subjectRegistrationUpdate.batchId) &&
        Objects.equals(this.referenceNo, subjectRegistrationUpdate.referenceNo) &&
        Objects.equals(this.minUnitLoad, subjectRegistrationUpdate.minUnitLoad) &&
        Objects.equals(this.maxUnitLoad, subjectRegistrationUpdate.maxUnitLoad) &&
        Objects.equals(this.readByStudent, subjectRegistrationUpdate.readByStudent) &&
        Objects.equals(this.readByGuardian, subjectRegistrationUpdate.readByGuardian);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, courseId, batchId, referenceNo, minUnitLoad, maxUnitLoad, readByStudent, readByGuardian);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubjectRegistrationUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    referenceNo: ").append(toIndentedString(referenceNo)).append("\n");
    sb.append("    minUnitLoad: ").append(toIndentedString(minUnitLoad)).append("\n");
    sb.append("    maxUnitLoad: ").append(toIndentedString(maxUnitLoad)).append("\n");
    sb.append("    readByStudent: ").append(toIndentedString(readByStudent)).append("\n");
    sb.append("    readByGuardian: ").append(toIndentedString(readByGuardian)).append("\n");
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

