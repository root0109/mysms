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
 * Fields of QuestionBlankAnswer that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionBlankAnswerUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> questionBlankId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 500) String> answerText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> feedback;

  public QuestionBlankAnswerUpdate questionBlankId(@Nullable Optional<UUID> questionBlankId) {
    this.questionBlankId = questionBlankId;
    return this;
  }

  /**
   * Blank.
   * @return questionBlankId
   */
  
  @JsonProperty("questionBlankId")
  public @Nullable Optional<UUID> getQuestionBlankId() {
    return questionBlankId;
  }

  @JsonProperty("questionBlankId")
  public void setQuestionBlankId(@Nullable Optional<UUID> questionBlankId) {
    this.questionBlankId = questionBlankId;
  }

  public QuestionBlankAnswerUpdate answerText(@Nullable Optional<String> answerText) {
    this.answerText = answerText;
    return this;
  }

  /**
   * Accepted answer.
   * @return answerText
   */
  
  @JsonProperty("answerText")
  public @Nullable Optional<String> getAnswerText() {
    return answerText;
  }

  @JsonProperty("answerText")
  public void setAnswerText(@Nullable Optional<String> answerText) {
    this.answerText = answerText;
  }

  public QuestionBlankAnswerUpdate feedback(@Nullable Optional<String> feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback when given.
   * @return feedback
   */
  
  @JsonProperty("feedback")
  public @Nullable Optional<String> getFeedback() {
    return feedback;
  }

  @JsonProperty("feedback")
  public void setFeedback(@Nullable Optional<String> feedback) {
    this.feedback = feedback;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionBlankAnswerUpdate questionBlankAnswerUpdate = (QuestionBlankAnswerUpdate) o;
    return Objects.equals(this.questionBlankId, questionBlankAnswerUpdate.questionBlankId) &&
        Objects.equals(this.answerText, questionBlankAnswerUpdate.answerText) &&
        Objects.equals(this.feedback, questionBlankAnswerUpdate.feedback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionBlankId, answerText, feedback);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionBlankAnswerUpdate {\n");
    sb.append("    questionBlankId: ").append(toIndentedString(questionBlankId)).append("\n");
    sb.append("    answerText: ").append(toIndentedString(answerText)).append("\n");
    sb.append("    feedback: ").append(toIndentedString(feedback)).append("\n");
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

