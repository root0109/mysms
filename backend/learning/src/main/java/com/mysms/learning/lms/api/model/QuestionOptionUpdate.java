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
 * Fields of QuestionOption that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionOptionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> optionText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isCorrect;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> explanation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 10) String> label;

  public QuestionOptionUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

  public QuestionOptionUpdate optionText(@Nullable Optional<String> optionText) {
    this.optionText = optionText;
    return this;
  }

  /**
   * Choice text.
   * @return optionText
   */
  
  @JsonProperty("optionText")
  public @Nullable Optional<String> getOptionText() {
    return optionText;
  }

  @JsonProperty("optionText")
  public void setOptionText(@Nullable Optional<String> optionText) {
    this.optionText = optionText;
  }

  public QuestionOptionUpdate isCorrect(@Nullable Optional<Boolean> isCorrect) {
    this.isCorrect = isCorrect;
    return this;
  }

  /**
   * Correct choice.
   * @return isCorrect
   */
  
  @JsonProperty("isCorrect")
  public @Nullable Optional<Boolean> getIsCorrect() {
    return isCorrect;
  }

  @JsonProperty("isCorrect")
  public void setIsCorrect(@Nullable Optional<Boolean> isCorrect) {
    this.isCorrect = isCorrect;
  }

  public QuestionOptionUpdate explanation(@Nullable Optional<String> explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * Why the choice is right/wrong.
   * @return explanation
   */
  
  @JsonProperty("explanation")
  public @Nullable Optional<String> getExplanation() {
    return explanation;
  }

  @JsonProperty("explanation")
  public void setExplanation(@Nullable Optional<String> explanation) {
    this.explanation = explanation;
  }

  public QuestionOptionUpdate label(@Nullable Optional<String> label) {
    this.label = label;
    return this;
  }

  /**
   * Option label (A, B, i...).
   * @return label
   */
  
  @JsonProperty("label")
  public @Nullable Optional<String> getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable Optional<String> label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionOptionUpdate questionOptionUpdate = (QuestionOptionUpdate) o;
    return Objects.equals(this.sortOrder, questionOptionUpdate.sortOrder) &&
        Objects.equals(this.optionText, questionOptionUpdate.optionText) &&
        Objects.equals(this.isCorrect, questionOptionUpdate.isCorrect) &&
        Objects.equals(this.explanation, questionOptionUpdate.explanation) &&
        Objects.equals(this.label, questionOptionUpdate.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortOrder, optionText, isCorrect, explanation, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionOptionUpdate {\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    optionText: ").append(toIndentedString(optionText)).append("\n");
    sb.append("    isCorrect: ").append(toIndentedString(isCorrect)).append("\n");
    sb.append("    explanation: ").append(toIndentedString(explanation)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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

