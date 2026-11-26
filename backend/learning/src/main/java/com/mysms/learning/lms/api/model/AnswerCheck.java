// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * AnswerCheck
 */

public class AnswerCheck {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean isCorrect;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String explanation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> correctOptionIds = new ArrayList<>();

  public AnswerCheck() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AnswerCheck(Boolean isCorrect) {
    this.isCorrect = isCorrect;
  }

  public AnswerCheck isCorrect(Boolean isCorrect) {
    this.isCorrect = isCorrect;
    return this;
  }

  /**
   * Correct.
   * @return isCorrect
   */
  @NotNull 
  @JsonProperty("isCorrect")
  public Boolean getIsCorrect() {
    return isCorrect;
  }

  @JsonProperty("isCorrect")
  public void setIsCorrect(Boolean isCorrect) {
    this.isCorrect = isCorrect;
  }

  public AnswerCheck explanation(@Nullable String explanation) {
    this.explanation = explanation;
    return this;
  }

  /**
   * Explanation.
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

  public AnswerCheck correctOptionIds(List<UUID> correctOptionIds) {
    this.correctOptionIds = correctOptionIds;
    return this;
  }

  public AnswerCheck addCorrectOptionIdsItem(UUID correctOptionIdsItem) {
    if (this.correctOptionIds == null) {
      this.correctOptionIds = new ArrayList<>();
    }
    this.correctOptionIds.add(correctOptionIdsItem);
    return this;
  }

  /**
   * Correct choices (when the quiz shows answers).
   * @return correctOptionIds
   */
  
  @JsonProperty("correctOptionIds")
  public List<UUID> getCorrectOptionIds() {
    return correctOptionIds;
  }

  @JsonProperty("correctOptionIds")
  public void setCorrectOptionIds(List<UUID> correctOptionIds) {
    this.correctOptionIds = correctOptionIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnswerCheck answerCheck = (AnswerCheck) o;
    return Objects.equals(this.isCorrect, answerCheck.isCorrect) &&
        Objects.equals(this.explanation, answerCheck.explanation) &&
        Objects.equals(this.correctOptionIds, answerCheck.correctOptionIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isCorrect, explanation, correctOptionIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnswerCheck {\n");
    sb.append("    isCorrect: ").append(toIndentedString(isCorrect)).append("\n");
    sb.append("    explanation: ").append(toIndentedString(explanation)).append("\n");
    sb.append("    correctOptionIds: ").append(toIndentedString(correctOptionIds)).append("\n");
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

