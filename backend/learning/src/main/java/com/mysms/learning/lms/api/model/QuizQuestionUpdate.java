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
 * Fields of QuizQuestion that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuizQuestionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> questionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public QuizQuestionUpdate questionId(@Nullable Optional<UUID> questionId) {
    this.questionId = questionId;
    return this;
  }

  /**
   * Question.
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

  public QuizQuestionUpdate marks(@Nullable Optional<String> marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Marks in this quiz.
   * @return marks
   */
  
  @JsonProperty("marks")
  public @Nullable Optional<String> getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(@Nullable Optional<String> marks) {
    this.marks = marks;
  }

  public QuizQuestionUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
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
    QuizQuestionUpdate quizQuestionUpdate = (QuizQuestionUpdate) o;
    return Objects.equals(this.questionId, quizQuestionUpdate.questionId) &&
        Objects.equals(this.marks, quizQuestionUpdate.marks) &&
        Objects.equals(this.sortOrder, quizQuestionUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId, marks, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizQuestionUpdate {\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
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

