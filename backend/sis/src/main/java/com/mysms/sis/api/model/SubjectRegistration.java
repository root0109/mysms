// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * Student request to register compulsory and elective subjects for a course/batch.
 */

public class SubjectRegistration {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programEnrollmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String referenceNo;

  /**
   * Approval state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    DRAFT("draft"),
    
    SUBMITTED("submitted"),
    
    APPROVED("approved"),
    
    REJECTED("rejected");

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String minUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean readByStudent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean readByGuardian;

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

  public SubjectRegistration() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SubjectRegistration(UUID studentId, UUID courseId) {
    this.studentId = studentId;
    this.courseId = courseId;
  }

  public SubjectRegistration academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year the registration is for.
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

  public SubjectRegistration academicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Term/semester the registration is for (NULL = whole year).
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

  public SubjectRegistration programEnrollmentId(@Nullable UUID programEnrollmentId) {
    this.programEnrollmentId = programEnrollmentId;
    return this;
  }

  /**
   * Program enrollment this registration belongs to.
   * @return programEnrollmentId
   */
  @Valid 
  @JsonProperty("programEnrollmentId")
  public @Nullable UUID getProgramEnrollmentId() {
    return programEnrollmentId;
  }

  @JsonProperty("programEnrollmentId")
  public void setProgramEnrollmentId(@Nullable UUID programEnrollmentId) {
    this.programEnrollmentId = programEnrollmentId;
  }

  public SubjectRegistration id(@Nullable UUID id) {
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

  public SubjectRegistration studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public SubjectRegistration courseId(UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
   * @return courseId
   */
  @NotNull @Valid 
  @JsonProperty("courseId")
  public UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  public SubjectRegistration batchId(@Nullable UUID batchId) {
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

  public SubjectRegistration referenceNo(@Nullable String referenceNo) {
    this.referenceNo = referenceNo;
    return this;
  }

  /**
   * Registration reference.
   * @return referenceNo
   */
  @Size(max = 40) 
  @JsonProperty("referenceNo")
  public @Nullable String getReferenceNo() {
    return referenceNo;
  }

  @JsonProperty("referenceNo")
  public void setReferenceNo(@Nullable String referenceNo) {
    this.referenceNo = referenceNo;
  }

  public SubjectRegistration status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Approval state. Changed only through the workflow operations.
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

  public SubjectRegistration minUnitLoad(@Nullable String minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
    return this;
  }

  /**
   * Minimum credits.
   * @return minUnitLoad
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("minUnitLoad")
  public @Nullable String getMinUnitLoad() {
    return minUnitLoad;
  }

  @JsonProperty("minUnitLoad")
  public void setMinUnitLoad(@Nullable String minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
  }

  public SubjectRegistration maxUnitLoad(@Nullable String maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
    return this;
  }

  /**
   * Maximum credits.
   * @return maxUnitLoad
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("maxUnitLoad")
  public @Nullable String getMaxUnitLoad() {
    return maxUnitLoad;
  }

  @JsonProperty("maxUnitLoad")
  public void setMaxUnitLoad(@Nullable String maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
  }

  public SubjectRegistration readByStudent(@Nullable Boolean readByStudent) {
    this.readByStudent = readByStudent;
    return this;
  }

  /**
   * Student has seen the decision.
   * @return readByStudent
   */
  
  @JsonProperty("readByStudent")
  public @Nullable Boolean getReadByStudent() {
    return readByStudent;
  }

  @JsonProperty("readByStudent")
  public void setReadByStudent(@Nullable Boolean readByStudent) {
    this.readByStudent = readByStudent;
  }

  public SubjectRegistration readByGuardian(@Nullable Boolean readByGuardian) {
    this.readByGuardian = readByGuardian;
    return this;
  }

  /**
   * Guardian has seen the decision.
   * @return readByGuardian
   */
  
  @JsonProperty("readByGuardian")
  public @Nullable Boolean getReadByGuardian() {
    return readByGuardian;
  }

  @JsonProperty("readByGuardian")
  public void setReadByGuardian(@Nullable Boolean readByGuardian) {
    this.readByGuardian = readByGuardian;
  }

  public SubjectRegistration createTime(@Nullable OffsetDateTime createTime) {
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

  public SubjectRegistration createdBy(@Nullable UUID createdBy) {
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

  public SubjectRegistration updateTime(@Nullable OffsetDateTime updateTime) {
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

  public SubjectRegistration updatedBy(@Nullable UUID updatedBy) {
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

  public SubjectRegistration etag(@Nullable String etag) {
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
    SubjectRegistration subjectRegistration = (SubjectRegistration) o;
    return Objects.equals(this.academicYearId, subjectRegistration.academicYearId) &&
        Objects.equals(this.academicTermId, subjectRegistration.academicTermId) &&
        Objects.equals(this.programEnrollmentId, subjectRegistration.programEnrollmentId) &&
        Objects.equals(this.id, subjectRegistration.id) &&
        Objects.equals(this.studentId, subjectRegistration.studentId) &&
        Objects.equals(this.courseId, subjectRegistration.courseId) &&
        Objects.equals(this.batchId, subjectRegistration.batchId) &&
        Objects.equals(this.referenceNo, subjectRegistration.referenceNo) &&
        Objects.equals(this.status, subjectRegistration.status) &&
        Objects.equals(this.minUnitLoad, subjectRegistration.minUnitLoad) &&
        Objects.equals(this.maxUnitLoad, subjectRegistration.maxUnitLoad) &&
        Objects.equals(this.readByStudent, subjectRegistration.readByStudent) &&
        Objects.equals(this.readByGuardian, subjectRegistration.readByGuardian) &&
        Objects.equals(this.createTime, subjectRegistration.createTime) &&
        Objects.equals(this.createdBy, subjectRegistration.createdBy) &&
        Objects.equals(this.updateTime, subjectRegistration.updateTime) &&
        Objects.equals(this.updatedBy, subjectRegistration.updatedBy) &&
        Objects.equals(this.etag, subjectRegistration.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(academicYearId, academicTermId, programEnrollmentId, id, studentId, courseId, batchId, referenceNo, status, minUnitLoad, maxUnitLoad, readByStudent, readByGuardian, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubjectRegistration {\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    academicTermId: ").append(toIndentedString(academicTermId)).append("\n");
    sb.append("    programEnrollmentId: ").append(toIndentedString(programEnrollmentId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    referenceNo: ").append(toIndentedString(referenceNo)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    minUnitLoad: ").append(toIndentedString(minUnitLoad)).append("\n");
    sb.append("    maxUnitLoad: ").append(toIndentedString(maxUnitLoad)).append("\n");
    sb.append("    readByStudent: ").append(toIndentedString(readByStudent)).append("\n");
    sb.append("    readByGuardian: ").append(toIndentedString(readByGuardian)).append("\n");
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

