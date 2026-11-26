// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.QuizAnswerInput;
import com.mysms.learning.lms.api.model.ViolationEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SubmitQuizRequest
 */

public class SubmitQuizRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String attemptToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid QuizAnswerInput> answers = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid ViolationEvent> violations = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String submissionReason;

  public SubmitQuizRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SubmitQuizRequest(String attemptToken, List<@Valid QuizAnswerInput> answers) {
    this.attemptToken = attemptToken;
    this.answers = answers;
  }

  public SubmitQuizRequest attemptToken(String attemptToken) {
    this.attemptToken = attemptToken;
    return this;
  }

  /**
   * From StartQuizAttempt; binds the submit to the delivered question set.
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

  public SubmitQuizRequest answers(List<@Valid QuizAnswerInput> answers) {
    this.answers = answers;
    return this;
  }

  public SubmitQuizRequest addAnswersItem(QuizAnswerInput answersItem) {
    if (this.answers == null) {
      this.answers = new ArrayList<>();
    }
    this.answers.add(answersItem);
    return this;
  }

  /**
   * Answers.
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

  public SubmitQuizRequest violations(List<@Valid ViolationEvent> violations) {
    this.violations = violations;
    return this;
  }

  public SubmitQuizRequest addViolationsItem(ViolationEvent violationsItem) {
    if (this.violations == null) {
      this.violations = new ArrayList<>();
    }
    this.violations.add(violationsItem);
    return this;
  }

  /**
   * Proctoring events.
   * @return violations
   */
  
  @JsonProperty("violations")
  public List<@Valid ViolationEvent> getViolations() {
    return violations;
  }

  @JsonProperty("violations")
  public void setViolations(List<@Valid ViolationEvent> violations) {
    this.violations = violations;
  }

  public SubmitQuizRequest submissionReason(@Nullable String submissionReason) {
    this.submissionReason = submissionReason;
    return this;
  }

  /**
   * manual / time_up / violations.
   * @return submissionReason
   */
  
  @JsonProperty("submissionReason")
  public @Nullable String getSubmissionReason() {
    return submissionReason;
  }

  @JsonProperty("submissionReason")
  public void setSubmissionReason(@Nullable String submissionReason) {
    this.submissionReason = submissionReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubmitQuizRequest submitQuizRequest = (SubmitQuizRequest) o;
    return Objects.equals(this.attemptToken, submitQuizRequest.attemptToken) &&
        Objects.equals(this.answers, submitQuizRequest.answers) &&
        Objects.equals(this.violations, submitQuizRequest.violations) &&
        Objects.equals(this.submissionReason, submitQuizRequest.submissionReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attemptToken, answers, violations, submissionReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubmitQuizRequest {\n");
    sb.append("    attemptToken: ").append(toIndentedString(attemptToken)).append("\n");
    sb.append("    answers: ").append(toIndentedString(answers)).append("\n");
    sb.append("    violations: ").append(toIndentedString(violations)).append("\n");
    sb.append("    submissionReason: ").append(toIndentedString(submissionReason)).append("\n");
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

