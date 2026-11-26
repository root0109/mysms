// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.Quiz;
import com.mysms.learning.lms.api.model.QuizQuestionView;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * QuizAttempt
 */

public class QuizAttempt {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Quiz quiz;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid QuizQuestionView> questions = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer attemptsUsed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String attemptToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID quizSubmissionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer attemptNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime deadlineAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime serverTime;

  public QuizAttempt() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuizAttempt(Quiz quiz, List<@Valid QuizQuestionView> questions, Integer attemptsUsed, String attemptToken, UUID quizSubmissionId) {
    this.quiz = quiz;
    this.questions = questions;
    this.attemptsUsed = attemptsUsed;
    this.attemptToken = attemptToken;
    this.quizSubmissionId = quizSubmissionId;
  }

  public QuizAttempt quiz(Quiz quiz) {
    this.quiz = quiz;
    return this;
  }

  /**
   * Get quiz
   * @return quiz
   */
  @NotNull @Valid 
  @JsonProperty("quiz")
  public Quiz getQuiz() {
    return quiz;
  }

  @JsonProperty("quiz")
  public void setQuiz(Quiz quiz) {
    this.quiz = quiz;
  }

  public QuizAttempt questions(List<@Valid QuizQuestionView> questions) {
    this.questions = questions;
    return this;
  }

  public QuizAttempt addQuestionsItem(QuizQuestionView questionsItem) {
    if (this.questions == null) {
      this.questions = new ArrayList<>();
    }
    this.questions.add(questionsItem);
    return this;
  }

  /**
   * Questions in delivery order.
   * @return questions
   */
  @NotNull 
  @JsonProperty("questions")
  public List<@Valid QuizQuestionView> getQuestions() {
    return questions;
  }

  @JsonProperty("questions")
  public void setQuestions(List<@Valid QuizQuestionView> questions) {
    this.questions = questions;
  }

  public QuizAttempt attemptsUsed(Integer attemptsUsed) {
    this.attemptsUsed = attemptsUsed;
    return this;
  }

  /**
   * Previous attempts.
   * @return attemptsUsed
   */
  @NotNull 
  @JsonProperty("attemptsUsed")
  public Integer getAttemptsUsed() {
    return attemptsUsed;
  }

  @JsonProperty("attemptsUsed")
  public void setAttemptsUsed(Integer attemptsUsed) {
    this.attemptsUsed = attemptsUsed;
  }

  public QuizAttempt attemptToken(String attemptToken) {
    this.attemptToken = attemptToken;
    return this;
  }

  /**
   * Token binding the submission to this delivery.
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

  public QuizAttempt quizSubmissionId(UUID quizSubmissionId) {
    this.quizSubmissionId = quizSubmissionId;
    return this;
  }

  /**
   * Submission row this attempt writes into (autosave + submit).
   * @return quizSubmissionId
   */
  @NotNull @Valid 
  @JsonProperty("quizSubmissionId")
  public UUID getQuizSubmissionId() {
    return quizSubmissionId;
  }

  @JsonProperty("quizSubmissionId")
  public void setQuizSubmissionId(UUID quizSubmissionId) {
    this.quizSubmissionId = quizSubmissionId;
  }

  public QuizAttempt attemptNumber(@Nullable Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
    return this;
  }

  /**
   * 1-based attempt number of this attempt.
   * @return attemptNumber
   */
  
  @JsonProperty("attemptNumber")
  public @Nullable Integer getAttemptNumber() {
    return attemptNumber;
  }

  @JsonProperty("attemptNumber")
  public void setAttemptNumber(@Nullable Integer attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public QuizAttempt deadlineAt(@Nullable OffsetDateTime deadlineAt) {
    this.deadlineAt = deadlineAt;
    return this;
  }

  /**
   * Server deadline for this attempt (duration and placement windows applied).
   * @return deadlineAt
   */
  @Valid 
  @JsonProperty("deadlineAt")
  public @Nullable OffsetDateTime getDeadlineAt() {
    return deadlineAt;
  }

  @JsonProperty("deadlineAt")
  public void setDeadlineAt(@Nullable OffsetDateTime deadlineAt) {
    this.deadlineAt = deadlineAt;
  }

  public QuizAttempt serverTime(@Nullable OffsetDateTime serverTime) {
    this.serverTime = serverTime;
    return this;
  }

  /**
   * Server clock at delivery, so clients can compute remaining time without trusting the device clock.
   * @return serverTime
   */
  @Valid 
  @JsonProperty("serverTime")
  public @Nullable OffsetDateTime getServerTime() {
    return serverTime;
  }

  @JsonProperty("serverTime")
  public void setServerTime(@Nullable OffsetDateTime serverTime) {
    this.serverTime = serverTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuizAttempt quizAttempt = (QuizAttempt) o;
    return Objects.equals(this.quiz, quizAttempt.quiz) &&
        Objects.equals(this.questions, quizAttempt.questions) &&
        Objects.equals(this.attemptsUsed, quizAttempt.attemptsUsed) &&
        Objects.equals(this.attemptToken, quizAttempt.attemptToken) &&
        Objects.equals(this.quizSubmissionId, quizAttempt.quizSubmissionId) &&
        Objects.equals(this.attemptNumber, quizAttempt.attemptNumber) &&
        Objects.equals(this.deadlineAt, quizAttempt.deadlineAt) &&
        Objects.equals(this.serverTime, quizAttempt.serverTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(quiz, questions, attemptsUsed, attemptToken, quizSubmissionId, attemptNumber, deadlineAt, serverTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizAttempt {\n");
    sb.append("    quiz: ").append(toIndentedString(quiz)).append("\n");
    sb.append("    questions: ").append(toIndentedString(questions)).append("\n");
    sb.append("    attemptsUsed: ").append(toIndentedString(attemptsUsed)).append("\n");
    sb.append("    attemptToken: ").append(toIndentedString(attemptToken)).append("\n");
    sb.append("    quizSubmissionId: ").append(toIndentedString(quizSubmissionId)).append("\n");
    sb.append("    attemptNumber: ").append(toIndentedString(attemptNumber)).append("\n");
    sb.append("    deadlineAt: ").append(toIndentedString(deadlineAt)).append("\n");
    sb.append("    serverTime: ").append(toIndentedString(serverTime)).append("\n");
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

