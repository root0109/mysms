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
 * Fields of CohortActivity that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortActivityUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> exerciseId;

  public CohortActivityUpdate quizId(@Nullable Optional<UUID> quizId) {
    this.quizId = quizId;
    return this;
  }

  /**
   * Quiz.
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

  public CohortActivityUpdate assignmentId(@Nullable Optional<UUID> assignmentId) {
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

  public CohortActivityUpdate exerciseId(@Nullable Optional<UUID> exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Exercise.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortActivityUpdate cohortActivityUpdate = (CohortActivityUpdate) o;
    return Objects.equals(this.quizId, cohortActivityUpdate.quizId) &&
        Objects.equals(this.assignmentId, cohortActivityUpdate.assignmentId) &&
        Objects.equals(this.exerciseId, cohortActivityUpdate.exerciseId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(quizId, assignmentId, exerciseId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortActivityUpdate {\n");
    sb.append("    quizId: ").append(toIndentedString(quizId)).append("\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    exerciseId: ").append(toIndentedString(exerciseId)).append("\n");
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

