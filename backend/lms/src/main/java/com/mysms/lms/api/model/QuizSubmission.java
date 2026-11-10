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
 * One attempt at a quiz.
 */

public class QuizSubmission {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer attemptNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String score;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scoreOutOf;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String percentage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String passingPercentage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer violationCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String submissionReason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  /**
   * Attempt state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    ASSIGNED("assigned"),
    
    IN_PROGRESS("in_progress"),
    
    PAUSED("paused"),
    
    SUBMITTED("submitted"),
    
    GRADED("graded");

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
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime submittedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer timeTakenSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer answeredCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer correctCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer incorrectCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isReassigned;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reassignedFromId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherComment;

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

  public QuizSubmission() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuizSubmission(UUID quizId, UUID userId) {
    this.quizId = quizId;
    this.userId = userId;
  }

  public QuizSubmission attemptNumber(@Nullable Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
    return this;
  }

  /**
   * 1-based attempt number of this user on this quiz placement.
   * @return attemptNumber
   */
  
  @JsonProperty("attemptNumber")
  public @Nullable Integer getAttemptNumber() {
    return attemptNumber;
  }

  @JsonProperty("attemptNumber")
  public void setAttemptNumber(@Nullable Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public QuizSubmission id(@Nullable UUID id) {
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

  public QuizSubmission quizId(UUID quizId) {
    this.quizId = quizId;
    return this;
  }

  /**
   * Quiz.
   * @return quizId
   */
  @NotNull @Valid 
  @JsonProperty("quizId")
  public UUID getQuizId() {
    return quizId;
  }

  @JsonProperty("quizId")
  public void setQuizId(UUID quizId) {
    this.quizId = quizId;
  }

  public QuizSubmission userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public QuizSubmission courseId(@Nullable UUID courseId) {
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

  public QuizSubmission score(@Nullable String score) {
    this.score = score;
    return this;
  }

  /**
   * Score.
   * @return score
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("score")
  public @Nullable String getScore() {
    return score;
  }

  @JsonProperty("score")
  public void setScore(@Nullable String score) {
    this.score = score;
  }

  public QuizSubmission scoreOutOf(@Nullable String scoreOutOf) {
    this.scoreOutOf = scoreOutOf;
    return this;
  }

  /**
   * Maximum score.
   * @return scoreOutOf
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("scoreOutOf")
  public @Nullable String getScoreOutOf() {
    return scoreOutOf;
  }

  @JsonProperty("scoreOutOf")
  public void setScoreOutOf(@Nullable String scoreOutOf) {
    this.scoreOutOf = scoreOutOf;
  }

  public QuizSubmission percentage(@Nullable String percentage) {
    this.percentage = percentage;
    return this;
  }

  /**
   * Percentage.
   * @return percentage
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("percentage")
  public @Nullable String getPercentage() {
    return percentage;
  }

  @JsonProperty("percentage")
  public void setPercentage(@Nullable String percentage) {
    this.percentage = percentage;
  }

  public QuizSubmission passingPercentage(@Nullable String passingPercentage) {
    this.passingPercentage = passingPercentage;
    return this;
  }

  /**
   * Pass mark at the time of attempt.
   * @return passingPercentage
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("passingPercentage")
  public @Nullable String getPassingPercentage() {
    return passingPercentage;
  }

  @JsonProperty("passingPercentage")
  public void setPassingPercentage(@Nullable String passingPercentage) {
    this.passingPercentage = passingPercentage;
  }

  public QuizSubmission violationCount(@Nullable Integer violationCount) {
    this.violationCount = violationCount;
    return this;
  }

  /**
   * Proctoring violations.
   * @return violationCount
   */
  
  @JsonProperty("violationCount")
  public @Nullable Integer getViolationCount() {
    return violationCount;
  }

  @JsonProperty("violationCount")
  public void setViolationCount(@Nullable Integer violationCount) {
    this.violationCount = violationCount;
  }

  public QuizSubmission submissionReason(@Nullable String submissionReason) {
    this.submissionReason = submissionReason;
    return this;
  }

  /**
   * Why submitted (manual, time_up, violations).
   * @return submissionReason
   */
  @Size(max = 60) 
  @JsonProperty("submissionReason")
  public @Nullable String getSubmissionReason() {
    return submissionReason;
  }

  @JsonProperty("submissionReason")
  public void setSubmissionReason(@Nullable String submissionReason) {
    this.submissionReason = submissionReason;
  }

  public QuizSubmission contentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Placement of the quiz in a cohort.
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

  public QuizSubmission cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public QuizSubmission status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Attempt state. Changed only through the workflow operations.
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

  public QuizSubmission startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Attempt started.
   * @return startedAt
   */
  @Valid 
  @JsonProperty("startedAt")
  public @Nullable OffsetDateTime getStartedAt() {
    return startedAt;
  }

  @JsonProperty("startedAt")
  public void setStartedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public QuizSubmission submittedAt(@Nullable OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  /**
   * Attempt submitted.
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

  public QuizSubmission timeTakenSeconds(@Nullable Integer timeTakenSeconds) {
    this.timeTakenSeconds = timeTakenSeconds;
    return this;
  }

  /**
   * Time taken.
   * @return timeTakenSeconds
   */
  
  @JsonProperty("timeTakenSeconds")
  public @Nullable Integer getTimeTakenSeconds() {
    return timeTakenSeconds;
  }

  @JsonProperty("timeTakenSeconds")
  public void setTimeTakenSeconds(@Nullable Integer timeTakenSeconds) {
    this.timeTakenSeconds = timeTakenSeconds;
  }

  public QuizSubmission answeredCount(@Nullable Integer answeredCount) {
    this.answeredCount = answeredCount;
    return this;
  }

  /**
   * Questions answered so far in the current attempt.
   * @return answeredCount
   */
  
  @JsonProperty("answeredCount")
  public @Nullable Integer getAnsweredCount() {
    return answeredCount;
  }

  @JsonProperty("answeredCount")
  public void setAnsweredCount(@Nullable Integer answeredCount) {
    this.answeredCount = answeredCount;
  }

  public QuizSubmission correctCount(@Nullable Integer correctCount) {
    this.correctCount = correctCount;
    return this;
  }

  /**
   * Correct answers.
   * @return correctCount
   */
  
  @JsonProperty("correctCount")
  public @Nullable Integer getCorrectCount() {
    return correctCount;
  }

  @JsonProperty("correctCount")
  public void setCorrectCount(@Nullable Integer correctCount) {
    this.correctCount = correctCount;
  }

  public QuizSubmission incorrectCount(@Nullable Integer incorrectCount) {
    this.incorrectCount = incorrectCount;
    return this;
  }

  /**
   * Incorrect answers.
   * @return incorrectCount
   */
  
  @JsonProperty("incorrectCount")
  public @Nullable Integer getIncorrectCount() {
    return incorrectCount;
  }

  @JsonProperty("incorrectCount")
  public void setIncorrectCount(@Nullable Integer incorrectCount) {
    this.incorrectCount = incorrectCount;
  }

  public QuizSubmission isReassigned(@Nullable Boolean isReassigned) {
    this.isReassigned = isReassigned;
    return this;
  }

  /**
   * Reassigned for a fresh attempt.
   * @return isReassigned
   */
  
  @JsonProperty("isReassigned")
  public @Nullable Boolean getIsReassigned() {
    return isReassigned;
  }

  @JsonProperty("isReassigned")
  public void setIsReassigned(@Nullable Boolean isReassigned) {
    this.isReassigned = isReassigned;
  }

  public QuizSubmission reassignedFromId(@Nullable UUID reassignedFromId) {
    this.reassignedFromId = reassignedFromId;
    return this;
  }

  /**
   * Attempt record this one replaced.
   * @return reassignedFromId
   */
  @Valid 
  @JsonProperty("reassignedFromId")
  public @Nullable UUID getReassignedFromId() {
    return reassignedFromId;
  }

  @JsonProperty("reassignedFromId")
  public void setReassignedFromId(@Nullable UUID reassignedFromId) {
    this.reassignedFromId = reassignedFromId;
  }

  public QuizSubmission teacherComment(@Nullable String teacherComment) {
    this.teacherComment = teacherComment;
    return this;
  }

  /**
   * Teacher's comment.
   * @return teacherComment
   */
  
  @JsonProperty("teacherComment")
  public @Nullable String getTeacherComment() {
    return teacherComment;
  }

  @JsonProperty("teacherComment")
  public void setTeacherComment(@Nullable String teacherComment) {
    this.teacherComment = teacherComment;
  }

  public QuizSubmission createTime(@Nullable OffsetDateTime createTime) {
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

  public QuizSubmission createdBy(@Nullable UUID createdBy) {
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

  public QuizSubmission updateTime(@Nullable OffsetDateTime updateTime) {
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

  public QuizSubmission updatedBy(@Nullable UUID updatedBy) {
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

  public QuizSubmission etag(@Nullable String etag) {
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
    QuizSubmission quizSubmission = (QuizSubmission) o;
    return Objects.equals(this.attemptNumber, quizSubmission.attemptNumber) &&
        Objects.equals(this.id, quizSubmission.id) &&
        Objects.equals(this.quizId, quizSubmission.quizId) &&
        Objects.equals(this.userId, quizSubmission.userId) &&
        Objects.equals(this.courseId, quizSubmission.courseId) &&
        Objects.equals(this.score, quizSubmission.score) &&
        Objects.equals(this.scoreOutOf, quizSubmission.scoreOutOf) &&
        Objects.equals(this.percentage, quizSubmission.percentage) &&
        Objects.equals(this.passingPercentage, quizSubmission.passingPercentage) &&
        Objects.equals(this.violationCount, quizSubmission.violationCount) &&
        Objects.equals(this.submissionReason, quizSubmission.submissionReason) &&
        Objects.equals(this.contentItemId, quizSubmission.contentItemId) &&
        Objects.equals(this.cohortId, quizSubmission.cohortId) &&
        Objects.equals(this.status, quizSubmission.status) &&
        Objects.equals(this.startedAt, quizSubmission.startedAt) &&
        Objects.equals(this.submittedAt, quizSubmission.submittedAt) &&
        Objects.equals(this.timeTakenSeconds, quizSubmission.timeTakenSeconds) &&
        Objects.equals(this.answeredCount, quizSubmission.answeredCount) &&
        Objects.equals(this.correctCount, quizSubmission.correctCount) &&
        Objects.equals(this.incorrectCount, quizSubmission.incorrectCount) &&
        Objects.equals(this.isReassigned, quizSubmission.isReassigned) &&
        Objects.equals(this.reassignedFromId, quizSubmission.reassignedFromId) &&
        Objects.equals(this.teacherComment, quizSubmission.teacherComment) &&
        Objects.equals(this.createTime, quizSubmission.createTime) &&
        Objects.equals(this.createdBy, quizSubmission.createdBy) &&
        Objects.equals(this.updateTime, quizSubmission.updateTime) &&
        Objects.equals(this.updatedBy, quizSubmission.updatedBy) &&
        Objects.equals(this.etag, quizSubmission.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attemptNumber, id, quizId, userId, courseId, score, scoreOutOf, percentage, passingPercentage, violationCount, submissionReason, contentItemId, cohortId, status, startedAt, submittedAt, timeTakenSeconds, answeredCount, correctCount, incorrectCount, isReassigned, reassignedFromId, teacherComment, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizSubmission {\n");
    sb.append("    attemptNumber: ").append(toIndentedString(attemptNumber)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    quizId: ").append(toIndentedString(quizId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    score: ").append(toIndentedString(score)).append("\n");
    sb.append("    scoreOutOf: ").append(toIndentedString(scoreOutOf)).append("\n");
    sb.append("    percentage: ").append(toIndentedString(percentage)).append("\n");
    sb.append("    passingPercentage: ").append(toIndentedString(passingPercentage)).append("\n");
    sb.append("    violationCount: ").append(toIndentedString(violationCount)).append("\n");
    sb.append("    submissionReason: ").append(toIndentedString(submissionReason)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    submittedAt: ").append(toIndentedString(submittedAt)).append("\n");
    sb.append("    timeTakenSeconds: ").append(toIndentedString(timeTakenSeconds)).append("\n");
    sb.append("    answeredCount: ").append(toIndentedString(answeredCount)).append("\n");
    sb.append("    correctCount: ").append(toIndentedString(correctCount)).append("\n");
    sb.append("    incorrectCount: ").append(toIndentedString(incorrectCount)).append("\n");
    sb.append("    isReassigned: ").append(toIndentedString(isReassigned)).append("\n");
    sb.append("    reassignedFromId: ").append(toIndentedString(reassignedFromId)).append("\n");
    sb.append("    teacherComment: ").append(toIndentedString(teacherComment)).append("\n");
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

