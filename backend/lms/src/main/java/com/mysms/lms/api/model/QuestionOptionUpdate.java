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
 * Fields of QuestionOption that can be updated (partial update; see updateMask).
 */

public class QuestionOptionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String optionText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isCorrect;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String explanation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String label;

  public QuestionOptionUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public QuestionOptionUpdate optionText(@Nullable String optionText) {
    this.optionText = optionText;
    return this;
  }

  /**
   * Choice text.
   * @return optionText
   */
  
  @JsonProperty("optionText")
  public @Nullable String getOptionText() {
    return optionText;
  }

  @JsonProperty("optionText")
  public void setOptionText(@Nullable String optionText) {
    this.optionText = optionText;
  }

  public QuestionOptionUpdate isCorrect(@Nullable Boolean isCorrect) {
    this.isCorrect = isCorrect;
    return this;
  }

  /**
   * Correct choice.
   * @return isCorrect
   */
  
  @JsonProperty("isCorrect")
  public @Nullable Boolean getIsCorrect() {
    return isCorrect;
  }

  @JsonProperty("isCorrect")
  public void setIsCorrect(@Nullable Boolean isCorrect) {
    this.isCorrect = isCorrect;
  }

  public QuestionOptionUpdate explanation(@Nullable String explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * Why the choice is right/wrong.
   * @return explanation
   */
  
  @JsonProperty("explanation")
  public @Nullable String getExplanation() {
    return explanation;
  }

  @JsonProperty("explanation")
  public void setExplanation(@Nullable String explanation) {
    this.explanation = explanation;
  }

  public QuestionOptionUpdate label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Option label (A, B, i...).
   * @return label
   */
  @Size(max = 10) 
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
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

