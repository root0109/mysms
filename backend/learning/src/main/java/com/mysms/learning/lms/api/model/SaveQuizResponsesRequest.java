// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.QuizAnswerInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SaveQuizResponsesRequest
 */

public class SaveQuizResponsesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String attemptToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String clientSequence;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid QuizAnswerInput> answers = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer timeSpentSeconds;

  public SaveQuizResponsesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SaveQuizResponsesRequest(String attemptToken, String clientSequence, List<@Valid QuizAnswerInput> answers) {
    this.attemptToken = attemptToken;
    this.clientSequence = clientSequence;
    this.answers = answers;
  }

  public SaveQuizResponsesRequest attemptToken(String attemptToken) {
    this.attemptToken = attemptToken;
    return this;
  }

  /**
   * From StartQuizAttempt; binds the save to this delivery.
   * @return attemptToken
   */
  @NotNull 
  @JsonProperty("attemptToken")
  public String getAttemptToken() {
    return attemptToken;
  }

  @JsonProperty("attemptToken")
  public void setAttemptToken(String attemptToken) {
    this.attemptToken = attemptToken;
  }

  public SaveQuizResponsesRequest clientSequence(String clientSequence) {
    this.clientSequence = clientSequence;
    return this;
  }

  /**
   * Monotonic counter per attempt on the device; saves with a lower sequence than the last stored one are ignored.
   * @return clientSequence
   */
  @NotNull @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("clientSequence")
  public String getClientSequence() {
    return clientSequence;
  }

  @JsonProperty("clientSequence")
  public void setClientSequence(String clientSequence) {
    this.clientSequence = clientSequence;
  }

  public SaveQuizResponsesRequest answers(List<@Valid QuizAnswerInput> answers) {
    this.answers = answers;
    return this;
  }

  public SaveQuizResponsesRequest addAnswersItem(QuizAnswerInput answersItem) {
    if (this.answers == null) {
      this.answers = new ArrayList<>();
    }
    this.answers.add(answersItem);
    return this;
  }

  /**
   * Answers changed since the last save (answeredAt orders them).
   * @return answers
   */
  @NotNull 
  @JsonProperty("answers")
  public List<@Valid QuizAnswerInput> getAnswers() {
    return answers;
  }

  @JsonProperty("answers")
  public void setAnswers(List<@Valid QuizAnswerInput> answers) {
    this.answers = answers;
  }

  public SaveQuizResponsesRequest timeSpentSeconds(@Nullable Integer timeSpentSeconds) {
    this.timeSpentSeconds = timeSpentSeconds;
    return this;
  }

  /**
   * Time spent so far.
   * @return timeSpentSeconds
   */
  
  @JsonProperty("timeSpentSeconds")
  public @Nullable Integer getTimeSpentSeconds() {
    return timeSpentSeconds;
  }

  @JsonProperty("timeSpentSeconds")
  public void setTimeSpentSeconds(@Nullable Integer timeSpentSeconds) {
    this.timeSpentSeconds = timeSpentSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SaveQuizResponsesRequest saveQuizResponsesRequest = (SaveQuizResponsesRequest) o;
    return Objects.equals(this.attemptToken, saveQuizResponsesRequest.attemptToken) &&
        Objects.equals(this.clientSequence, saveQuizResponsesRequest.clientSequence) &&
        Objects.equals(this.answers, saveQuizResponsesRequest.answers) &&
        Objects.equals(this.timeSpentSeconds, saveQuizResponsesRequest.timeSpentSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attemptToken, clientSequence, answers, timeSpentSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SaveQuizResponsesRequest {\n");
    sb.append("    attemptToken: ").append(toIndentedString(attemptToken)).append("\n");
    sb.append("    clientSequence: ").append(toIndentedString(clientSequence)).append("\n");
    sb.append("    answers: ").append(toIndentedString(answers)).append("\n");
    sb.append("    timeSpentSeconds: ").append(toIndentedString(timeSpentSeconds)).append("\n");
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

