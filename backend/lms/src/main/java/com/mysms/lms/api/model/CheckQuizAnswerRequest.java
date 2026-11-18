// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.QuizAnswerInput;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CheckQuizAnswerRequest
 */

public class CheckQuizAnswerRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID questionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private QuizAnswerInput answer;

  public CheckQuizAnswerRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckQuizAnswerRequest(UUID questionId, QuizAnswerInput answer) {
    this.questionId = questionId;
    this.answer = answer;
  }

  public CheckQuizAnswerRequest questionId(UUID questionId) {
    this.questionId = questionId;
    return this;
  }

  /**
   * Question.
   * @return questionId
   */
  @NotNull @Valid 
  @JsonProperty("questionId")
  public UUID getQuestionId() {
    return questionId;
  }

  @JsonProperty("questionId")
  public void setQuestionId(UUID questionId) {
    this.questionId = questionId;
  }

  public CheckQuizAnswerRequest answer(QuizAnswerInput answer) {
    this.answer = answer;
    return this;
  }

  /**
   * Get answer
   * @return answer
   */
  @NotNull @Valid 
  @JsonProperty("answer")
  public QuizAnswerInput getAnswer() {
    return answer;
  }

  @JsonProperty("answer")
  public void setAnswer(QuizAnswerInput answer) {
    this.answer = answer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckQuizAnswerRequest checkQuizAnswerRequest = (CheckQuizAnswerRequest) o;
    return Objects.equals(this.questionId, checkQuizAnswerRequest.questionId) &&
        Objects.equals(this.answer, checkQuizAnswerRequest.answer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId, answer);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckQuizAnswerRequest {\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
    sb.append("    answer: ").append(toIndentedString(answer)).append("\n");
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

