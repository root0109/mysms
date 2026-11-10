// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
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
 * Fields of Homework that can be updated (partial update; see updateMask).
 */

public class HomeworkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignedByStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime assignedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  public HomeworkUpdate institutionId(@Nullable UUID institutionId) {
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

  public HomeworkUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public HomeworkUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section.
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

  public HomeworkUpdate subjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  @Valid 
  @JsonProperty("subjectId")
  public @Nullable UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
  }

  public HomeworkUpdate assignedByStaffId(@Nullable UUID assignedByStaffId) {
    this.assignedByStaffId = assignedByStaffId;
    return this;
  }

  /**
   * Teacher.
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

  public HomeworkUpdate description(@Nullable String description) {
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

  public HomeworkUpdate attachmentFileId(@Nullable UUID attachmentFileId) {
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

  public HomeworkUpdate assignedOn(@Nullable OffsetDateTime assignedOn) {
    this.assignedOn = assignedOn;
    return this;
  }

  /**
   * Assigned on.
   * @return assignedOn
   */
  @Valid 
  @JsonProperty("assignedOn")
  public @Nullable OffsetDateTime getAssignedOn() {
    return assignedOn;
  }

  @JsonProperty("assignedOn")
  public void setAssignedOn(@Nullable OffsetDateTime assignedOn) {
    this.assignedOn = assignedOn;
  }

  public HomeworkUpdate dueAt(@Nullable OffsetDateTime dueAt) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HomeworkUpdate homeworkUpdate = (HomeworkUpdate) o;
    return Objects.equals(this.institutionId, homeworkUpdate.institutionId) &&
        Objects.equals(this.academicYearId, homeworkUpdate.academicYearId) &&
        Objects.equals(this.classSectionId, homeworkUpdate.classSectionId) &&
        Objects.equals(this.subjectId, homeworkUpdate.subjectId) &&
        Objects.equals(this.assignedByStaffId, homeworkUpdate.assignedByStaffId) &&
        Objects.equals(this.description, homeworkUpdate.description) &&
        Objects.equals(this.attachmentFileId, homeworkUpdate.attachmentFileId) &&
        Objects.equals(this.assignedOn, homeworkUpdate.assignedOn) &&
        Objects.equals(this.dueAt, homeworkUpdate.dueAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, classSectionId, subjectId, assignedByStaffId, description, attachmentFileId, assignedOn, dueAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HomeworkUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
    sb.append("    assignedByStaffId: ").append(toIndentedString(assignedByStaffId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    assignedOn: ").append(toIndentedString(assignedOn)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
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

