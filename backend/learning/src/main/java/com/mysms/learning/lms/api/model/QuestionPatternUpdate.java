// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of QuestionPattern that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionPatternUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> modelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 1000) String> pattern;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public QuestionPatternUpdate modelAnswer(@Nullable Optional<String> modelAnswer) {
    this.modelAnswer = modelAnswer;
    return this;
  }

  /**
   * Model answer.
   * @return modelAnswer
   */
  
  @JsonProperty("modelAnswer")
  public @Nullable Optional<String> getModelAnswer() {
    return modelAnswer;
  }

  @JsonProperty("modelAnswer")
  public void setModelAnswer(@Nullable Optional<String> modelAnswer) {
    this.modelAnswer = modelAnswer;
  }

  public QuestionPatternUpdate pattern(@Nullable Optional<String> pattern) {
    this.pattern = pattern;
    return this;
  }

  /**
   * Pattern (regular expression) the answer must match.
   * @return pattern
   */
  
  @JsonProperty("pattern")
  public @Nullable Optional<String> getPattern() {
    return pattern;
  }

  @JsonProperty("pattern")
  public void setPattern(@Nullable Optional<String> pattern) {
    this.pattern = pattern;
  }

  public QuestionPatternUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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
    QuestionPatternUpdate questionPatternUpdate = (QuestionPatternUpdate) o;
    return Objects.equals(this.modelAnswer, questionPatternUpdate.modelAnswer) &&
        Objects.equals(this.pattern, questionPatternUpdate.pattern) &&
        Objects.equals(this.sortOrder, questionPatternUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelAnswer, pattern, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionPatternUpdate {\n");
    sb.append("    modelAnswer: ").append(toIndentedString(modelAnswer)).append("\n");
    sb.append("    pattern: ").append(toIndentedString(pattern)).append("\n");
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

