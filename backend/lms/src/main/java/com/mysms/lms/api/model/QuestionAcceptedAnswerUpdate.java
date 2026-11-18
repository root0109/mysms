// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of QuestionAcceptedAnswer that can be updated (partial update; see updateMask).
 */

public class QuestionAcceptedAnswerUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String answerText;

  public QuestionAcceptedAnswerUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public QuestionAcceptedAnswerUpdate answerText(@Nullable String answerText) {
    this.answerText = answerText;
    return this;
  }

  /**
   * Accepted answer.
   * @return answerText
   */
  
  @JsonProperty("answerText")
  public @Nullable String getAnswerText() {
    return answerText;
  }

  @JsonProperty("answerText")
  public void setAnswerText(@Nullable String answerText) {
    this.answerText = answerText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionAcceptedAnswerUpdate questionAcceptedAnswerUpdate = (QuestionAcceptedAnswerUpdate) o;
    return Objects.equals(this.sortOrder, questionAcceptedAnswerUpdate.sortOrder) &&
        Objects.equals(this.answerText, questionAcceptedAnswerUpdate.answerText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortOrder, answerText);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionAcceptedAnswerUpdate {\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    answerText: ").append(toIndentedString(answerText)).append("\n");
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

