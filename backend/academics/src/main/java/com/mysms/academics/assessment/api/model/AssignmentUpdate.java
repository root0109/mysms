// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

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
 * Fields of Assignment that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AssignmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> assignmentTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> assignedByStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> reviewerStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> maxMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> issuedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attachmentFileId;

  public AssignmentUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public AssignmentUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public AssignmentUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public AssignmentUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Instructions.
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

  public AssignmentUpdate assignmentTypeId(@Nullable Optional<UUID> assignmentTypeId) {
    this.assignmentTypeId = assignmentTypeId;
    return this;
  }

  /**
   * Type.
   * @return assignmentTypeId
   */
  
  @JsonProperty("assignmentTypeId")
  public @Nullable Optional<UUID> getAssignmentTypeId() {
    return assignmentTypeId;
  }

  @JsonProperty("assignmentTypeId")
  public void setAssignmentTypeId(@Nullable Optional<UUID> assignmentTypeId) {
    this.assignmentTypeId = assignmentTypeId;
  }

  public AssignmentUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section (K-12).
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

  public AssignmentUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (higher education).
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

  public AssignmentUpdate batchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (higher education).
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

  public AssignmentUpdate assignedByStaffId(@Nullable Optional<UUID> assignedByStaffId) {
    this.assignedByStaffId = assignedByStaffId;
    return this;
  }

  /**
   * Teacher who set it.
   * @return assignedByStaffId
   */
  
  @JsonProperty("assignedByStaffId")
  public @Nullable Optional<UUID> getAssignedByStaffId() {
    return assignedByStaffId;
  }

  @JsonProperty("assignedByStaffId")
  public void setAssignedByStaffId(@Nullable Optional<UUID> assignedByStaffId) {
    this.assignedByStaffId = assignedByStaffId;
  }

  public AssignmentUpdate reviewerStaffId(@Nullable Optional<UUID> reviewerStaffId) {
    this.reviewerStaffId = reviewerStaffId;
    return this;
  }

  /**
   * Teacher who grades it.
   * @return reviewerStaffId
   */
  
  @JsonProperty("reviewerStaffId")
  public @Nullable Optional<UUID> getReviewerStaffId() {
    return reviewerStaffId;
  }

  @JsonProperty("reviewerStaffId")
  public void setReviewerStaffId(@Nullable Optional<UUID> reviewerStaffId) {
    this.reviewerStaffId = reviewerStaffId;
  }

  public AssignmentUpdate maxMarks(@Nullable Optional<String> maxMarks) {
    this.maxMarks = maxMarks;
    return this;
  }

  /**
   * Maximum marks.
   * @return maxMarks
   */
  
  @JsonProperty("maxMarks")
  public @Nullable Optional<String> getMaxMarks() {
    return maxMarks;
  }

  @JsonProperty("maxMarks")
  public void setMaxMarks(@Nullable Optional<String> maxMarks) {
    this.maxMarks = maxMarks;
  }

  public AssignmentUpdate issuedOn(@Nullable Optional<OffsetDateTime> issuedOn) {
    this.issuedOn = issuedOn;
    return this;
  }

  /**
   * Issue date.
   * @return issuedOn
   */
  
  @JsonProperty("issuedOn")
  public @Nullable Optional<OffsetDateTime> getIssuedOn() {
    return issuedOn;
  }

  @JsonProperty("issuedOn")
  public void setIssuedOn(@Nullable Optional<OffsetDateTime> issuedOn) {
    this.issuedOn = issuedOn;
  }

  public AssignmentUpdate dueAt(@Nullable Optional<OffsetDateTime> dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Submission deadline.
   * @return dueAt
   */
  
  @JsonProperty("dueAt")
  public @Nullable Optional<OffsetDateTime> getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable Optional<OffsetDateTime> dueAt) {
    this.dueAt = dueAt;
  }

  public AssignmentUpdate attachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  
  @JsonProperty("attachmentFileId")
  public @Nullable Optional<UUID> getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
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

