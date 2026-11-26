// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ContentAuthor that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ContentAuthorUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> questionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> learningProgramId;

  public ContentAuthorUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Author.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public ContentAuthorUpdate quizId(@Nullable Optional<UUID> quizId) {
    this.quizId = quizId;
    return this;
  }

  /**
   * Quiz authored.
   * @return quizId
   */
  
  @JsonProperty("quizId")
  public @Nullable Optional<UUID> getQuizId() {
    return quizId;
  }

  @JsonProperty("quizId")
  public void setQuizId(@Nullable Optional<UUID> quizId) {
    this.quizId = quizId;
  }

  public ContentAuthorUpdate questionId(@Nullable Optional<UUID> questionId) {
    this.questionId = questionId;
    return this;
  }

  /**
   * Question authored.
   * @return questionId
   */
  
  @JsonProperty("questionId")
  public @Nullable Optional<UUID> getQuestionId() {
    return questionId;
  }

  @JsonProperty("questionId")
  public void setQuestionId(@Nullable Optional<UUID> questionId) {
    this.questionId = questionId;
  }

  public ContentAuthorUpdate assignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
    return this;
  }

  /**
   * Assignment authored.
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

  public ContentAuthorUpdate exerciseId(@Nullable Optional<UUID> exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Exercise authored.
   * @return exerciseId
   */
  
  @JsonProperty("exerciseId")
  public @Nullable Optional<UUID> getExerciseId() {
    return exerciseId;
  }

  @JsonProperty("exerciseId")
  public void setExerciseId(@Nullable Optional<UUID> exerciseId) {
    this.exerciseId = exerciseId;
  }

  public ContentAuthorUpdate learningProgramId(@Nullable Optional<UUID> learningProgramId) {
    this.learningProgramId = learningProgramId;
    return this;
  }

  /**
   * Program authored.
   * @return learningProgramId
   */
  
  @JsonProperty("learningProgramId")
  public @Nullable Optional<UUID> getLearningProgramId() {
    return learningProgramId;
  }

  @JsonProperty("learningProgramId")
  public void setLearningProgramId(@Nullable Optional<UUID> learningProgramId) {
    this.learningProgramId = learningProgramId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentAuthorUpdate contentAuthorUpdate = (ContentAuthorUpdate) o;
    return Objects.equals(this.userId, contentAuthorUpdate.userId) &&
        Objects.equals(this.quizId, contentAuthorUpdate.quizId) &&
        Objects.equals(this.questionId, contentAuthorUpdate.questionId) &&
        Objects.equals(this.assignmentId, contentAuthorUpdate.assignmentId) &&
        Objects.equals(this.exerciseId, contentAuthorUpdate.exerciseId) &&
        Objects.equals(this.learningProgramId, contentAuthorUpdate.learningProgramId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, quizId, questionId, assignmentId, exerciseId, learningProgramId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentAuthorUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    quizId: ").append(toIndentedString(quizId)).append("\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    exerciseId: ").append(toIndentedString(exerciseId)).append("\n");
    sb.append("    learningProgramId: ").append(toIndentedString(learningProgramId)).append("\n");
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

