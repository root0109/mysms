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
 * Fields of QuestionBlankAnswer that can be updated (partial update; see updateMask).
 */

public class QuestionBlankAnswerUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID questionBlankId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String answerText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  public QuestionBlankAnswerUpdate questionBlankId(@Nullable UUID questionBlankId) {
    this.questionBlankId = questionBlankId;
    return this;
  }

  /**
   * Blank.
   * @return questionBlankId
   */
  @Valid 
  @JsonProperty("questionBlankId")
  public @Nullable UUID getQuestionBlankId() {
    return questionBlankId;
  }

  @JsonProperty("questionBlankId")
  public void setQuestionBlankId(@Nullable UUID questionBlankId) {
    this.questionBlankId = questionBlankId;
  }

  public QuestionBlankAnswerUpdate answerText(@Nullable String answerText) {
    this.answerText = answerText;
    return this;
  }

  /**
   * Accepted answer.
   * @return answerText
   */
  @Size(max = 500) 
  @JsonProperty("answerText")
  public @Nullable String getAnswerText() {
    return answerText;
  }

  @JsonProperty("answerText")
  public void setAnswerText(@Nullable String answerText) {
    this.answerText = answerText;
  }

  public QuestionBlankAnswerUpdate feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback when given.
   * @return feedback
   */
  
  @JsonProperty("feedback")
  public @Nullable String getFeedback() {
    return feedback;
  }

  @JsonProperty("feedback")
  public void setFeedback(@Nullable String feedback) {
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

