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
 * Fields of QuestionPoolItem that can be updated (partial update; see updateMask).
 */

public class QuestionPoolItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID questionId;

  public QuestionPoolItemUpdate questionId(@Nullable UUID questionId) {
    this.questionId = questionId;
    return this;
  }

  /**
   * Question.
   * @return questionId
   */
  @Valid 
  @JsonProperty("questionId")
  public @Nullable UUID getQuestionId() {
    return questionId;
  }

  @JsonProperty("questionId")
  public void setQuestionId(@Nullable UUID questionId) {
    this.questionId = questionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionPoolItemUpdate questionPoolItemUpdate = (QuestionPoolItemUpdate) o;
    return Objects.equals(this.questionId, questionPoolItemUpdate.questionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionPoolItemUpdate {\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
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

