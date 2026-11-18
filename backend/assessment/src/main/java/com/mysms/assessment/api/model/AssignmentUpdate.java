// Owned API contract of assessment.
package com.mysms.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Assignment that can be updated (partial update; see updateMask).
 */

public class AssignmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignedByStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reviewerStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime issuedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attachmentFileId;

  public AssignmentUpdate institutionId(@Nullable UUID institutionId) {
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

  public AssignmentUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public AssignmentUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public AssignmentUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Instructions.
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

  public AssignmentUpdate assignmentTypeId(@Nullable UUID assignmentTypeId) {
    this.assignmentTypeId = assignmentTypeId;
    return this;
  }

  /**
   * Type.
   * @return assignmentTypeId
   */
  @Valid 
  @JsonProperty("assignmentTypeId")
  public @Nullable UUID getAssignmentTypeId() {
    return assignmentTypeId;
  }

  @JsonProperty("assignmentTypeId")
  public void setAssignmentTypeId(@Nullable UUID assignmentTypeId) {
    this.assignmentTypeId = assignmentTypeId;
  }

  public AssignmentUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section (K-12).
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public AssignmentUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (higher education).
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

  public AssignmentUpdate batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (higher education).
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

  public AssignmentUpdate assignedByStaffId(@Nullable UUID assignedByStaffId) {
    this.assignedByStaffId = assignedByStaffId;
    return this;
  }

  /**
   * Teacher who set it.
   * @return assignedByStaffId
   */
  @Valid 
  @JsonProperty("assignedByStaffId")
  public @Nullable UUID getAssignedByStaffId() {
    return assignedByStaffId;
  }

  @JsonProperty("assignedByStaffId")
  public void setAssignedByStaffId(@Nullable UUID assignedByStaffId) {
    this.assignedByStaffId = assignedByStaffId;
  }

  public AssignmentUpdate reviewerStaffId(@Nullable UUID reviewerStaffId) {
    this.reviewerStaffId = reviewerStaffId;
    return this;
  }

  /**
   * Teacher who grades it.
   * @return reviewerStaffId
   */
  @Valid 
  @JsonProperty("reviewerStaffId")
  public @Nullable UUID getReviewerStaffId() {
    return reviewerStaffId;
  }

  @JsonProperty("reviewerStaffId")
  public void setReviewerStaffId(@Nullable UUID reviewerStaffId) {
    this.reviewerStaffId = reviewerStaffId;
  }

  public AssignmentUpdate maxMarks(@Nullable String maxMarks) {
    this.maxMarks = maxMarks;
    return this;
  }

  /**
   * Maximum marks.
   * @return maxMarks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("maxMarks")
  public @Nullable String getMaxMarks() {
    return maxMarks;
  }

  @JsonProperty("maxMarks")
  public void setMaxMarks(@Nullable String maxMarks) {
    this.maxMarks = maxMarks;
  }

  public AssignmentUpdate issuedOn(@Nullable OffsetDateTime issuedOn) {
    this.issuedOn = issuedOn;
    return this;
  }

  /**
   * Issue date.
   * @return issuedOn
   */
  @Valid 
  @JsonProperty("issuedOn")
  public @Nullable OffsetDateTime getIssuedOn() {
    return issuedOn;
  }

  @JsonProperty("issuedOn")
  public void setIssuedOn(@Nullable OffsetDateTime issuedOn) {
    this.issuedOn = issuedOn;
  }

  public AssignmentUpdate dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Submission deadline.
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

  public AssignmentUpdate attachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  @Valid 
  @JsonProperty("attachmentFileId")
  public @Nullable UUID getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignmentUpdate assignmentUpdate = (AssignmentUpdate) o;
    return Objects.equals(this.institutionId, assignmentUpdate.institutionId) &&
        Objects.equals(this.academicYearId, assignmentUpdate.academicYearId) &&
        Objects.equals(this.title, assignmentUpdate.title) &&
        Objects.equals(this.description, assignmentUpdate.description) &&
        Objects.equals(this.assignmentTypeId, assignmentUpdate.assignmentTypeId) &&
        Objects.equals(this.classSectionId, assignmentUpdate.classSectionId) &&
        Objects.equals(this.courseId, assignmentUpdate.courseId) &&
        Objects.equals(this.batchId, assignmentUpdate.batchId) &&
        Objects.equals(this.assignedByStaffId, assignmentUpdate.assignedByStaffId) &&
        Objects.equals(this.reviewerStaffId, assignmentUpdate.reviewerStaffId) &&
        Objects.equals(this.maxMarks, assignmentUpdate.maxMarks) &&
        Objects.equals(this.issuedOn, assignmentUpdate.issuedOn) &&
        Objects.equals(this.dueAt, assignmentUpdate.dueAt) &&
        Objects.equals(this.attachmentFileId, assignmentUpdate.attachmentFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, title, description, assignmentTypeId, classSectionId, courseId, batchId, assignedByStaffId, reviewerStaffId, maxMarks, issuedOn, dueAt, attachmentFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignmentUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    assignmentTypeId: ").append(toIndentedString(assignmentTypeId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    assignedByStaffId: ").append(toIndentedString(assignedByStaffId)).append("\n");
    sb.append("    reviewerStaffId: ").append(toIndentedString(reviewerStaffId)).append("\n");
    sb.append("    maxMarks: ").append(toIndentedString(maxMarks)).append("\n");
    sb.append("    issuedOn: ").append(toIndentedString(issuedOn)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
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

