// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of LmsAssignmentSubmission that can be updated (partial update; see updateMask).
 */

public class LmsAssignmentSubmissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  /**
   * Type submitted.
   */
  public enum SubmissionTypeEnum {
    DOCUMENT("document"),
    
    PDF("pdf"),
    
    URL("url"),
    
    IMAGE("image"),
    
    TEXT("text");

    private final String value;

    SubmissionTypeEnum(String value) {
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
    public static SubmissionTypeEnum fromValue(String value) {
      for (SubmissionTypeEnum b : SubmissionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable SubmissionTypeEnum submissionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String questionSnapshot;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String answer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attachmentFileId;

  /**
   * Evaluation result.
   */
  public enum StatusEnum {
    NOT_GRADED("not_graded"),
    
    PASS("pass"),
    
    FAIL("fail"),
    
    NOT_APPLICABLE("not_applicable");

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
  private @Nullable UUID evaluatorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String evaluatorComments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime submittedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isFinal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime finalizedAt;

  public LmsAssignmentSubmissionUpdate assignmentId(@Nullable UUID assignmentId) {
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

  public LmsAssignmentSubmissionUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public LmsAssignmentSubmissionUpdate lessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson context.
   * @return lessonId
   */
  @Valid 
  @JsonProperty("lessonId")
  public @Nullable UUID getLessonId() {
    return lessonId;
  }

  @JsonProperty("lessonId")
  public void setLessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
  }

  public LmsAssignmentSubmissionUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course context.
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

  public LmsAssignmentSubmissionUpdate submissionType(@Nullable SubmissionTypeEnum submissionType) {
    this.submissionType = submissionType;
    return this;
  }

  /**
   * Type submitted.
   * @return submissionType
   */
  
  @JsonProperty("submissionType")
  public @Nullable SubmissionTypeEnum getSubmissionType() {
    return submissionType;
  }

  @JsonProperty("submissionType")
  public void setSubmissionType(@Nullable SubmissionTypeEnum submissionType) {
    this.submissionType = submissionType;
  }

  public LmsAssignmentSubmissionUpdate questionSnapshot(@Nullable String questionSnapshot) {
    this.questionSnapshot = questionSnapshot;
    return this;
  }

  /**
   * Task statement at submission time.
   * @return questionSnapshot
   */
  
  @JsonProperty("questionSnapshot")
  public @Nullable String getQuestionSnapshot() {
    return questionSnapshot;
  }

  @JsonProperty("questionSnapshot")
  public void setQuestionSnapshot(@Nullable String questionSnapshot) {
    this.questionSnapshot = questionSnapshot;
  }

  public LmsAssignmentSubmissionUpdate answer(@Nullable String answer) {
    this.answer = answer;
    return this;
  }

  /**
   * Text / URL answer.
   * @return answer
   */
  
  @JsonProperty("answer")
  public @Nullable String getAnswer() {
    return answer;
  }

  @JsonProperty("answer")
  public void setAnswer(@Nullable String answer) {
    this.answer = answer;
  }

  public LmsAssignmentSubmissionUpdate attachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Uploaded file.
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

  public LmsAssignmentSubmissionUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Evaluation result.
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

  public LmsAssignmentSubmissionUpdate evaluatorUserId(@Nullable UUID evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
    return this;
  }

  /**
   * Evaluator.
   * @return evaluatorUserId
   */
  @Valid 
  @JsonProperty("evaluatorUserId")
  public @Nullable UUID getEvaluatorUserId() {
    return evaluatorUserId;
  }

  @JsonProperty("evaluatorUserId")
  public void setEvaluatorUserId(@Nullable UUID evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
  }

  public LmsAssignmentSubmissionUpdate evaluatorComments(@Nullable String evaluatorComments) {
    this.evaluatorComments = evaluatorComments;
    return this;
  }

  /**
   * Feedback.
   * @return evaluatorComments
   */
  
  @JsonProperty("evaluatorComments")
  public @Nullable String getEvaluatorComments() {
    return evaluatorComments;
  }

  @JsonProperty("evaluatorComments")
  public void setEvaluatorComments(@Nullable String evaluatorComments) {
    this.evaluatorComments = evaluatorComments;
  }

  public LmsAssignmentSubmissionUpdate contentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Placement of the assignment in a cohort.
   * @return contentItemId
   */
  @Valid 
  @JsonProperty("contentItemId")
  public @Nullable UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public LmsAssignmentSubmissionUpdate groupId(@Nullable UUID groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group submitting (group assignments).
   * @return groupId
   */
  @Valid 
  @JsonProperty("groupId")
  public @Nullable UUID getGroupId() {
    return groupId;
  }

  @JsonProperty("groupId")
  public void setGroupId(@Nullable UUID groupId) {
    this.groupId = groupId;
  }

  public LmsAssignmentSubmissionUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Submission title.
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

  public LmsAssignmentSubmissionUpdate submittedAt(@Nullable OffsetDateTime submittedAt) {
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

  public LmsAssignmentSubmissionUpdate isFinal(@Nullable Boolean isFinal) {
    this.isFinal = isFinal;
    return this;
  }

  /**
   * Final submission (false = draft that can still change).
   * @return isFinal
   */
  
  @JsonProperty("isFinal")
  public @Nullable Boolean getIsFinal() {
    return isFinal;
  }

  @JsonProperty("isFinal")
  public void setIsFinal(@Nullable Boolean isFinal) {
    this.isFinal = isFinal;
  }

  public LmsAssignmentSubmissionUpdate finalizedAt(@Nullable OffsetDateTime finalizedAt) {
    this.finalizedAt = finalizedAt;
    return this;
  }

  /**
   * When marked final.
   * @return finalizedAt
   */
  @Valid 
  @JsonProperty("finalizedAt")
  public @Nullable OffsetDateTime getFinalizedAt() {
    return finalizedAt;
  }

  @JsonProperty("finalizedAt")
  public void setFinalizedAt(@Nullable OffsetDateTime finalizedAt) {
    this.finalizedAt = finalizedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LmsAssignmentSubmissionUpdate lmsAssignmentSubmissionUpdate = (LmsAssignmentSubmissionUpdate) o;
    return Objects.equals(this.assignmentId, lmsAssignmentSubmissionUpdate.assignmentId) &&
        Objects.equals(this.userId, lmsAssignmentSubmissionUpdate.userId) &&
        Objects.equals(this.lessonId, lmsAssignmentSubmissionUpdate.lessonId) &&
        Objects.equals(this.courseId, lmsAssignmentSubmissionUpdate.courseId) &&
        Objects.equals(this.submissionType, lmsAssignmentSubmissionUpdate.submissionType) &&
        Objects.equals(this.questionSnapshot, lmsAssignmentSubmissionUpdate.questionSnapshot) &&
        Objects.equals(this.answer, lmsAssignmentSubmissionUpdate.answer) &&
        Objects.equals(this.attachmentFileId, lmsAssignmentSubmissionUpdate.attachmentFileId) &&
        Objects.equals(this.status, lmsAssignmentSubmissionUpdate.status) &&
        Objects.equals(this.evaluatorUserId, lmsAssignmentSubmissionUpdate.evaluatorUserId) &&
        Objects.equals(this.evaluatorComments, lmsAssignmentSubmissionUpdate.evaluatorComments) &&
        Objects.equals(this.contentItemId, lmsAssignmentSubmissionUpdate.contentItemId) &&
        Objects.equals(this.groupId, lmsAssignmentSubmissionUpdate.groupId) &&
        Objects.equals(this.title, lmsAssignmentSubmissionUpdate.title) &&
        Objects.equals(this.submittedAt, lmsAssignmentSubmissionUpdate.submittedAt) &&
        Objects.equals(this.isFinal, lmsAssignmentSubmissionUpdate.isFinal) &&
        Objects.equals(this.finalizedAt, lmsAssignmentSubmissionUpdate.finalizedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignmentId, userId, lessonId, courseId, submissionType, questionSnapshot, answer, attachmentFileId, status, evaluatorUserId, evaluatorComments, contentItemId, groupId, title, submittedAt, isFinal, finalizedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsAssignmentSubmissionUpdate {\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    submissionType: ").append(toIndentedString(submissionType)).append("\n");
    sb.append("    questionSnapshot: ").append(toIndentedString(questionSnapshot)).append("\n");
    sb.append("    answer: ").append(toIndentedString(answer)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    evaluatorUserId: ").append(toIndentedString(evaluatorUserId)).append("\n");
    sb.append("    evaluatorComments: ").append(toIndentedString(evaluatorComments)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    submittedAt: ").append(toIndentedString(submittedAt)).append("\n");
    sb.append("    isFinal: ").append(toIndentedString(isFinal)).append("\n");
    sb.append("    finalizedAt: ").append(toIndentedString(finalizedAt)).append("\n");
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

