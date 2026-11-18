// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of LessonActivity that can be updated (partial update; see updateMask).
 */

public class LessonActivityUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean teacherOnly;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public LessonActivityUpdate quizId(@Nullable UUID quizId) {
    this.quizId = quizId;
    return this;
  }

  /**
   * Quiz.
   * @return quizId
   */
  @Valid 
  @JsonProperty("quizId")
  public @Nullable UUID getQuizId() {
    return quizId;
  }

  @JsonProperty("quizId")
  public void setQuizId(@Nullable UUID quizId) {
    this.quizId = quizId;
  }

  public LessonActivityUpdate assignmentId(@Nullable UUID assignmentId) {
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

  public LessonActivityUpdate exerciseId(@Nullable UUID exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Exercise.
   * @return exerciseId
   */
  @Valid 
  @JsonProperty("exerciseId")
  public @Nullable UUID getExerciseId() {
    return exerciseId;
  }

  @JsonProperty("exerciseId")
  public void setExerciseId(@Nullable UUID exerciseId) {
    this.exerciseId = exerciseId;
  }

  public LessonActivityUpdate teacherOnly(@Nullable Boolean teacherOnly) {
    this.teacherOnly = teacherOnly;
    return this;
  }

  /**
   * Visible to teachers only.
   * @return teacherOnly
   */
  
  @JsonProperty("teacherOnly")
  public @Nullable Boolean getTeacherOnly() {
    return teacherOnly;
  }

  @JsonProperty("teacherOnly")
  public void setTeacherOnly(@Nullable Boolean teacherOnly) {
    this.teacherOnly = teacherOnly;
  }

  public LessonActivityUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonActivityUpdate lessonActivityUpdate = (LessonActivityUpdate) o;
    return Objects.equals(this.quizId, lessonActivityUpdate.quizId) &&
        Objects.equals(this.assignmentId, lessonActivityUpdate.assignmentId) &&
        Objects.equals(this.exerciseId, lessonActivityUpdate.exerciseId) &&
        Objects.equals(this.teacherOnly, lessonActivityUpdate.teacherOnly) &&
        Objects.equals(this.sortOrder, lessonActivityUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(quizId, assignmentId, exerciseId, teacherOnly, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonActivityUpdate {\n");
    sb.append("    quizId: ").append(toIndentedString(quizId)).append("\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    exerciseId: ").append(toIndentedString(exerciseId)).append("\n");
    sb.append("    teacherOnly: ").append(toIndentedString(teacherOnly)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

