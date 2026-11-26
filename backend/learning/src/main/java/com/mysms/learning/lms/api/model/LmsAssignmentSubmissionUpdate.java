// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of LmsAssignmentSubmission that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LmsAssignmentSubmissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

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
  private @Nullable Optional<SubmissionTypeEnum> submissionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> questionSnapshot;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> answer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> title;

  public LmsAssignmentSubmissionUpdate assignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
    return this;
  }

  /**
   * Assignment.
   * @return assignmentId
   */
  
  @JsonProperty("assignmentId")
  public @Nullable Optional<UUID> getAssignmentId() {
    return assignmentId;
  }

  @JsonProperty("assignmentId")
  public void setAssignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
  }

  public LmsAssignmentSubmissionUpdate lessonId(@Nullable Optional<UUID> lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson context.
   * @return lessonId
   */
  
  @JsonProperty("lessonId")
  public @Nullable Optional<UUID> getLessonId() {
    return lessonId;
  }

  @JsonProperty("lessonId")
  public void setLessonId(@Nullable Optional<UUID> lessonId) {
    this.lessonId = lessonId;
  }

  public LmsAssignmentSubmissionUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course context.
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

  public LmsAssignmentSubmissionUpdate submissionType(@Nullable Optional<SubmissionTypeEnum> submissionType) {
    this.submissionType = submissionType;
    return this;
  }

  /**
   * Type submitted.
   * @return submissionType
   */
  
  @JsonProperty("submissionType")
  public @Nullable Optional<SubmissionTypeEnum> getSubmissionType() {
    return submissionType;
  }

  @JsonProperty("submissionType")
  public void setSubmissionType(@Nullable Optional<SubmissionTypeEnum> submissionType) {
    this.submissionType = submissionType;
  }

  public LmsAssignmentSubmissionUpdate questionSnapshot(@Nullable Optional<String> questionSnapshot) {
    this.questionSnapshot = questionSnapshot;
    return this;
  }

  /**
   * Task statement at submission time.
   * @return questionSnapshot
   */
  
  @JsonProperty("questionSnapshot")
  public @Nullable Optional<String> getQuestionSnapshot() {
    return questionSnapshot;
  }

  @JsonProperty("questionSnapshot")
  public void setQuestionSnapshot(@Nullable Optional<String> questionSnapshot) {
    this.questionSnapshot = questionSnapshot;
  }

  public LmsAssignmentSubmissionUpdate answer(@Nullable Optional<String> answer) {
    this.answer = answer;
    return this;
  }

  /**
   * Text / URL answer.
   * @return answer
   */
  
  @JsonProperty("answer")
  public @Nullable Optional<String> getAnswer() {
    return answer;
  }

  @JsonProperty("answer")
  public void setAnswer(@Nullable Optional<String> answer) {
    this.answer = answer;
  }

  public LmsAssignmentSubmissionUpdate attachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Uploaded file.
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

  public LmsAssignmentSubmissionUpdate contentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Placement of the assignment in a cohort.
   * @return contentItemId
   */
  
  @JsonProperty("contentItemId")
  public @Nullable Optional<UUID> getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
  }

  public LmsAssignmentSubmissionUpdate groupId(@Nullable Optional<UUID> groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group submitting (group assignments).
   * @return groupId
   */
  
  @JsonProperty("groupId")
  public @Nullable Optional<UUID> getGroupId() {
    return groupId;
  }

  @JsonProperty("groupId")
  public void setGroupId(@Nullable Optional<UUID> groupId) {
    this.groupId = groupId;
  }

  public LmsAssignmentSubmissionUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Submission title.
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
        Objects.equals(this.lessonId, lmsAssignmentSubmissionUpdate.lessonId) &&
        Objects.equals(this.courseId, lmsAssignmentSubmissionUpdate.courseId) &&
        Objects.equals(this.submissionType, lmsAssignmentSubmissionUpdate.submissionType) &&
        Objects.equals(this.questionSnapshot, lmsAssignmentSubmissionUpdate.questionSnapshot) &&
        Objects.equals(this.answer, lmsAssignmentSubmissionUpdate.answer) &&
        Objects.equals(this.attachmentFileId, lmsAssignmentSubmissionUpdate.attachmentFileId) &&
        Objects.equals(this.contentItemId, lmsAssignmentSubmissionUpdate.contentItemId) &&
        Objects.equals(this.groupId, lmsAssignmentSubmissionUpdate.groupId) &&
        Objects.equals(this.title, lmsAssignmentSubmissionUpdate.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignmentId, lessonId, courseId, submissionType, questionSnapshot, answer, attachmentFileId, contentItemId, groupId, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsAssignmentSubmissionUpdate {\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    submissionType: ").append(toIndentedString(submissionType)).append("\n");
    sb.append("    questionSnapshot: ").append(toIndentedString(questionSnapshot)).append("\n");
    sb.append("    answer: ").append(toIndentedString(answer)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

