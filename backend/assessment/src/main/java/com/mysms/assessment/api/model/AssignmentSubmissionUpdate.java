// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
package com.mysms.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AssignmentSubmission that can be updated (partial update; see updateMask).
 */

public class AssignmentSubmissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String content;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime submittedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String marksObtained;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate gradedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  public AssignmentSubmissionUpdate assignmentId(@Nullable UUID assignmentId) {
    this.assignmentId = assignmentId;
    return this;
  }

  /**
   * Assignment.
   * @return assignmentId
   */
  @Valid 
  @JsonProperty("assignmentId")
  public @Nullable UUID getAssignmentId() {
    return assignmentId;
  }

  @JsonProperty("assignmentId")
  public void setAssignmentId(@Nullable UUID assignmentId) {
    this.assignmentId = assignmentId;
  }

  public AssignmentSubmissionUpdate studentId(@Nullable UUID studentId) {
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

  public AssignmentSubmissionUpdate content(@Nullable String content) {
    this.content = content;
    return this;
  }

  /**
   * Submission text.
   * @return content
   */
  
  @JsonProperty("content")
  public @Nullable String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(@Nullable String content) {
    this.content = content;
  }

  public AssignmentSubmissionUpdate submittedAt(@Nullable OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  /**
   * Submission time.
   * @return submittedAt
   */
  @Valid 
  @JsonProperty("submittedAt")
  public @Nullable OffsetDateTime getSubmittedAt() {
    return submittedAt;
  }

  @JsonProperty("submittedAt")
  public void setSubmittedAt(@Nullable OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public AssignmentSubmissionUpdate marksObtained(@Nullable String marksObtained) {
    this.marksObtained = marksObtained;
    return this;
  }

  /**
   * Marks awarded.
   * @return marksObtained
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marksObtained")
  public @Nullable String getMarksObtained() {
    return marksObtained;
  }

  @JsonProperty("marksObtained")
  public void setMarksObtained(@Nullable String marksObtained) {
    this.marksObtained = marksObtained;
  }

  public AssignmentSubmissionUpdate gradedByUserId(@Nullable UUID gradedByUserId) {
    this.gradedByUserId = gradedByUserId;
    return this;
  }

  /**
   * Grader.
   * @return gradedByUserId
   */
  @Valid 
  @JsonProperty("gradedByUserId")
  public @Nullable UUID getGradedByUserId() {
    return gradedByUserId;
  }

  @JsonProperty("gradedByUserId")
  public void setGradedByUserId(@Nullable UUID gradedByUserId) {
    this.gradedByUserId = gradedByUserId;
  }

  public AssignmentSubmissionUpdate gradedOn(@Nullable LocalDate gradedOn) {
    this.gradedOn = gradedOn;
    return this;
  }

  /**
   * Grading date.
   * @return gradedOn
   */
  @Valid 
  @JsonProperty("gradedOn")
  public @Nullable LocalDate getGradedOn() {
    return gradedOn;
  }

  @JsonProperty("gradedOn")
  public void setGradedOn(@Nullable LocalDate gradedOn) {
    this.gradedOn = gradedOn;
  }

  public AssignmentSubmissionUpdate feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback to the student.
   * @return feedback
   */
  
  @JsonProperty("feedback")
  public @Nullable String getFeedback() {
    return feedback;
  }

  @JsonProperty("feedback")
  public void setFeedback(@Nullable String feedback) {
    this.feedback = feedback;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignmentSubmissionUpdate assignmentSubmissionUpdate = (AssignmentSubmissionUpdate) o;
    return Objects.equals(this.assignmentId, assignmentSubmissionUpdate.assignmentId) &&
        Objects.equals(this.studentId, assignmentSubmissionUpdate.studentId) &&
        Objects.equals(this.content, assignmentSubmissionUpdate.content) &&
        Objects.equals(this.submittedAt, assignmentSubmissionUpdate.submittedAt) &&
        Objects.equals(this.marksObtained, assignmentSubmissionUpdate.marksObtained) &&
        Objects.equals(this.gradedByUserId, assignmentSubmissionUpdate.gradedByUserId) &&
        Objects.equals(this.gradedOn, assignmentSubmissionUpdate.gradedOn) &&
        Objects.equals(this.feedback, assignmentSubmissionUpdate.feedback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignmentId, studentId, content, submittedAt, marksObtained, gradedByUserId, gradedOn, feedback);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignmentSubmissionUpdate {\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    submittedAt: ").append(toIndentedString(submittedAt)).append("\n");
    sb.append("    marksObtained: ").append(toIndentedString(marksObtained)).append("\n");
    sb.append("    gradedByUserId: ").append(toIndentedString(gradedByUserId)).append("\n");
    sb.append("    gradedOn: ").append(toIndentedString(gradedOn)).append("\n");
    sb.append("    feedback: ").append(toIndentedString(feedback)).append("\n");
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

