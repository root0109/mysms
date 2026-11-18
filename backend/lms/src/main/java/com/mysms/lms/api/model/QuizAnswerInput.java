// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.BlankAnswer;
import com.mysms.lms.api.model.HotspotPoint;
import com.mysms.lms.api.model.MatchAnswer;
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
 * QuizAnswerInput
 */

public class QuizAnswerInput {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID questionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> selectedOptionIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String answerText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String numericValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid BlankAnswer> blankAnswers = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid MatchAnswer> matchAnswers = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid HotspotPoint> hotspotPoints = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime answeredAt;

  public QuizAnswerInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuizAnswerInput(UUID questionId) {
    this.questionId = questionId;
  }

  public QuizAnswerInput questionId(UUID questionId) {
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

  public QuizAnswerInput selectedOptionIds(List<UUID> selectedOptionIds) {
    this.selectedOptionIds = selectedOptionIds;
    return this;
  }

  public QuizAnswerInput addSelectedOptionIdsItem(UUID selectedOptionIdsItem) {
    if (this.selectedOptionIds == null) {
      this.selectedOptionIds = new ArrayList<>();
    }
    this.selectedOptionIds.add(selectedOptionIdsItem);
    return this;
  }

  /**
   * Chosen options (choices, true/false).
   * @return selectedOptionIds
   */
  
  @JsonProperty("selectedOptionIds")
  public List<UUID> getSelectedOptionIds() {
    return selectedOptionIds;
  }

  @JsonProperty("selectedOptionIds")
  public void setSelectedOptionIds(List<UUID> selectedOptionIds) {
    this.selectedOptionIds = selectedOptionIds;
  }

  public QuizAnswerInput answerText(@Nullable String answerText) {
    this.answerText = answerText;
    return this;
  }

  /**
   * Typed answer (short / open-ended / pattern).
   * @return answerText
   */
  
  @JsonProperty("answerText")
  public @Nullable String getAnswerText() {
    return answerText;
  }

  @JsonProperty("answerText")
  public void setAnswerText(@Nullable String answerText) {
    this.answerText = answerText;
  }

  public QuizAnswerInput numericValue(@Nullable String numericValue) {
    this.numericValue = numericValue;
    return this;
  }

  /**
   * Numeric answer.
   * @return numericValue
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("numericValue")
  public @Nullable String getNumericValue() {
    return numericValue;
  }

  @JsonProperty("numericValue")
  public void setNumericValue(@Nullable String numericValue) {
    this.numericValue = numericValue;
  }

  public QuizAnswerInput blankAnswers(List<@Valid BlankAnswer> blankAnswers) {
    this.blankAnswers = blankAnswers;
    return this;
  }

  public QuizAnswerInput addBlankAnswersItem(BlankAnswer blankAnswersItem) {
    if (this.blankAnswers == null) {
      this.blankAnswers = new ArrayList<>();
    }
    this.blankAnswers.add(blankAnswersItem);
    return this;
  }

  /**
   * Answers per blank.
   * @return blankAnswers
   */
  
  @JsonProperty("blankAnswers")
  public List<@Valid BlankAnswer> getBlankAnswers() {
    return blankAnswers;
  }

  @JsonProperty("blankAnswers")
  public void setBlankAnswers(List<@Valid BlankAnswer> blankAnswers) {
    this.blankAnswers = blankAnswers;
  }

  public QuizAnswerInput matchAnswers(List<@Valid MatchAnswer> matchAnswers) {
    this.matchAnswers = matchAnswers;
    return this;
  }

  public QuizAnswerInput addMatchAnswersItem(MatchAnswer matchAnswersItem) {
    if (this.matchAnswers == null) {
      this.matchAnswers = new ArrayList<>();
    }
    this.matchAnswers.add(matchAnswersItem);
    return this;
  }

  /**
   * Matched pairs.
   * @return matchAnswers
   */
  
  @JsonProperty("matchAnswers")
  public List<@Valid MatchAnswer> getMatchAnswers() {
    return matchAnswers;
  }

  @JsonProperty("matchAnswers")
  public void setMatchAnswers(List<@Valid MatchAnswer> matchAnswers) {
    this.matchAnswers = matchAnswers;
  }

  public QuizAnswerInput hotspotPoints(List<@Valid HotspotPoint> hotspotPoints) {
    this.hotspotPoints = hotspotPoints;
    return this;
  }

  public QuizAnswerInput addHotspotPointsItem(HotspotPoint hotspotPointsItem) {
    if (this.hotspotPoints == null) {
      this.hotspotPoints = new ArrayList<>();
    }
    this.hotspotPoints.add(hotspotPointsItem);
    return this;
  }

  /**
   * Points clicked on the image.
   * @return hotspotPoints
   */
  
  @JsonProperty("hotspotPoints")
  public List<@Valid HotspotPoint> getHotspotPoints() {
    return hotspotPoints;
  }

  @JsonProperty("hotspotPoints")
  public void setHotspotPoints(List<@Valid HotspotPoint> hotspotPoints) {
    this.hotspotPoints = hotspotPoints;
  }

  public QuizAnswerInput answeredAt(@Nullable OffsetDateTime answeredAt) {
    this.answeredAt = answeredAt;
    return this;
  }

  /**
   * Client time the answer was given (offline ordering).
   * @return answeredAt
   */
  @Valid 
  @JsonProperty("answeredAt")
  public @Nullable OffsetDateTime getAnsweredAt() {
    return answeredAt;
  }

  @JsonProperty("answeredAt")
  public void setAnsweredAt(@Nullable OffsetDateTime answeredAt) {
    this.answeredAt = answeredAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuizAnswerInput quizAnswerInput = (QuizAnswerInput) o;
    return Objects.equals(this.questionId, quizAnswerInput.questionId) &&
        Objects.equals(this.selectedOptionIds, quizAnswerInput.selectedOptionIds) &&
        Objects.equals(this.answerText, quizAnswerInput.answerText) &&
        Objects.equals(this.numericValue, quizAnswerInput.numericValue) &&
        Objects.equals(this.blankAnswers, quizAnswerInput.blankAnswers) &&
        Objects.equals(this.matchAnswers, quizAnswerInput.matchAnswers) &&
        Objects.equals(this.hotspotPoints, quizAnswerInput.hotspotPoints) &&
        Objects.equals(this.answeredAt, quizAnswerInput.answeredAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId, selectedOptionIds, answerText, numericValue, blankAnswers, matchAnswers, hotspotPoints, answeredAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizAnswerInput {\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
    sb.append("    selectedOptionIds: ").append(toIndentedString(selectedOptionIds)).append("\n");
    sb.append("    answerText: ").append(toIndentedString(answerText)).append("\n");
    sb.append("    numericValue: ").append(toIndentedString(numericValue)).append("\n");
    sb.append("    blankAnswers: ").append(toIndentedString(blankAnswers)).append("\n");
    sb.append("    matchAnswers: ").append(toIndentedString(matchAnswers)).append("\n");
    sb.append("    hotspotPoints: ").append(toIndentedString(hotspotPoints)).append("\n");
    sb.append("    answeredAt: ").append(toIndentedString(answeredAt)).append("\n");
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

