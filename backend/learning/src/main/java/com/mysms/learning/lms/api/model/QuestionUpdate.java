// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Question that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> questionCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> questionText;

  /**
   * Type.
   */
  public enum QuestionTypeEnum {
    CHOICES("choices"),
    
    USER_INPUT("user_input"),
    
    OPEN_ENDED("open_ended"),
    
    TRUE_FALSE("true_false"),
    
    MATCHING("matching"),
    
    FILL_BLANK("fill_blank"),
    
    NUMERIC("numeric"),
    
    HOTSPOT("hotspot"),
    
    PATTERN("pattern");

    private final String value;

    QuestionTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static QuestionTypeEnum fromValue(String value) {
      for (QuestionTypeEnum b : QuestionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull QuestionTypeEnum> questionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowsMultiple;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> shuffleOptions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> optionLabelStyle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxSelections;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> difficulty;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> generalFeedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> correctFeedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> incorrectFeedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> modelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Boolean> trueFalseAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 50) String> numericAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 50) String> numericTolerance;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> numericIsRange;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> caseSensitive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowSuperscript;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowSubscript;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isRetired;

  public QuestionUpdate questionCode(@Nullable Optional<String> questionCode) {
    this.questionCode = questionCode;
    return this;
  }

  /**
   * Human-readable code.
   * @return questionCode
   */
  
  @JsonProperty("questionCode")
  public @Nullable Optional<String> getQuestionCode() {
    return questionCode;
  }

  @JsonProperty("questionCode")
  public void setQuestionCode(@Nullable Optional<String> questionCode) {
    this.questionCode = questionCode;
  }

  public QuestionUpdate questionText(@Nullable Optional<String> questionText) {
    this.questionText = questionText;
    return this;
  }

  /**
   * Question.
   * @return questionText
   */
  
  @JsonProperty("questionText")
  public @Nullable Optional<String> getQuestionText() {
    return questionText;
  }

  @JsonProperty("questionText")
  public void setQuestionText(@Nullable Optional<String> questionText) {
    this.questionText = questionText;
  }

  public QuestionUpdate questionType(@Nullable Optional<QuestionTypeEnum> questionType) {
    this.questionType = questionType;
    return this;
  }

  /**
   * Type.
   * @return questionType
   */
  
  @JsonProperty("questionType")
  public @Nullable Optional<QuestionTypeEnum> getQuestionType() {
    return questionType;
  }

  @JsonProperty("questionType")
  public void setQuestionType(@Nullable Optional<QuestionTypeEnum> questionType) {
    this.questionType = questionType;
  }

  public QuestionUpdate allowsMultiple(@Nullable Optional<Boolean> allowsMultiple) {
    this.allowsMultiple = allowsMultiple;
    return this;
  }

  /**
   * More than one choice may be correct.
   * @return allowsMultiple
   */
  
  @JsonProperty("allowsMultiple")
  public @Nullable Optional<Boolean> getAllowsMultiple() {
    return allowsMultiple;
  }

  @JsonProperty("allowsMultiple")
  public void setAllowsMultiple(@Nullable Optional<Boolean> allowsMultiple) {
    this.allowsMultiple = allowsMultiple;
  }

  public QuestionUpdate marks(@Nullable Optional<String> marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Default marks.
   * @return marks
   */
  
  @JsonProperty("marks")
  public @Nullable Optional<String> getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(@Nullable Optional<String> marks) {
    this.marks = marks;
  }

  public QuestionUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Short title shown in the question bank.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public QuestionUpdate shuffleOptions(@Nullable Optional<Boolean> shuffleOptions) {
    this.shuffleOptions = shuffleOptions;
    return this;
  }

  /**
   * Shuffle answer options per attempt.
   * @return shuffleOptions
   */
  
  @JsonProperty("shuffleOptions")
  public @Nullable Optional<Boolean> getShuffleOptions() {
    return shuffleOptions;
  }

  @JsonProperty("shuffleOptions")
  public void setShuffleOptions(@Nullable Optional<Boolean> shuffleOptions) {
    this.shuffleOptions = shuffleOptions;
  }

  public QuestionUpdate optionLabelStyle(@Nullable Optional<String> optionLabelStyle) {
    this.optionLabelStyle = optionLabelStyle;
    return this;
  }

  /**
   * Option labelling (A-B-C, a-b-c, 1-2-3, i-ii-iii).
   * @return optionLabelStyle
   */
  
  @JsonProperty("optionLabelStyle")
  public @Nullable Optional<String> getOptionLabelStyle() {
    return optionLabelStyle;
  }

  @JsonProperty("optionLabelStyle")
  public void setOptionLabelStyle(@Nullable Optional<String> optionLabelStyle) {
    this.optionLabelStyle = optionLabelStyle;
  }

  public QuestionUpdate maxSelections(@Nullable Optional<Integer> maxSelections) {
    this.maxSelections = maxSelections;
    return this;
  }

  /**
   * Maximum options a student may tick (multiple response).
   * @return maxSelections
   */
  
  @JsonProperty("maxSelections")
  public @Nullable Optional<Integer> getMaxSelections() {
    return maxSelections;
  }

  @JsonProperty("maxSelections")
  public void setMaxSelections(@Nullable Optional<Integer> maxSelections) {
    this.maxSelections = maxSelections;
  }

  public QuestionUpdate difficulty(@Nullable Optional<String> difficulty) {
    this.difficulty = difficulty;
    return this;
  }

  /**
   * Difficulty / complexity level.
   * @return difficulty
   */
  
  @JsonProperty("difficulty")
  public @Nullable Optional<String> getDifficulty() {
    return difficulty;
  }

  @JsonProperty("difficulty")
  public void setDifficulty(@Nullable Optional<String> difficulty) {
    this.difficulty = difficulty;
  }

  public QuestionUpdate generalFeedback(@Nullable Optional<String> generalFeedback) {
    this.generalFeedback = generalFeedback;
    return this;
  }

  /**
   * Feedback shown after answering.
   * @return generalFeedback
   */
  
  @JsonProperty("generalFeedback")
  public @Nullable Optional<String> getGeneralFeedback() {
    return generalFeedback;
  }

  @JsonProperty("generalFeedback")
  public void setGeneralFeedback(@Nullable Optional<String> generalFeedback) {
    this.generalFeedback = generalFeedback;
  }

  public QuestionUpdate correctFeedback(@Nullable Optional<String> correctFeedback) {
    this.correctFeedback = correctFeedback;
    return this;
  }

  /**
   * Feedback when correct.
   * @return correctFeedback
   */
  
  @JsonProperty("correctFeedback")
  public @Nullable Optional<String> getCorrectFeedback() {
    return correctFeedback;
  }

  @JsonProperty("correctFeedback")
  public void setCorrectFeedback(@Nullable Optional<String> correctFeedback) {
    this.correctFeedback = correctFeedback;
  }

  public QuestionUpdate incorrectFeedback(@Nullable Optional<String> incorrectFeedback) {
    this.incorrectFeedback = incorrectFeedback;
    return this;
  }

  /**
   * Feedback when incorrect.
   * @return incorrectFeedback
   */
  
  @JsonProperty("incorrectFeedback")
  public @Nullable Optional<String> getIncorrectFeedback() {
    return incorrectFeedback;
  }

  @JsonProperty("incorrectFeedback")
  public void setIncorrectFeedback(@Nullable Optional<String> incorrectFeedback) {
    this.incorrectFeedback = incorrectFeedback;
  }

  public QuestionUpdate modelAnswer(@Nullable Optional<String> modelAnswer) {
    this.modelAnswer = modelAnswer;
    return this;
  }

  /**
   * Model answer (open-ended / short answer).
   * @return modelAnswer
   */
  
  @JsonProperty("modelAnswer")
  public @Nullable Optional<String> getModelAnswer() {
    return modelAnswer;
  }

  @JsonProperty("modelAnswer")
  public void setModelAnswer(@Nullable Optional<String> modelAnswer) {
    this.modelAnswer = modelAnswer;
  }

  public QuestionUpdate trueFalseAnswer(@Nullable Optional<Boolean> trueFalseAnswer) {
    this.trueFalseAnswer = trueFalseAnswer;
    return this;
  }

  /**
   * Correct answer of a true/false question.
   * @return trueFalseAnswer
   */
  
  @JsonProperty("trueFalseAnswer")
  public @Nullable Optional<Boolean> getTrueFalseAnswer() {
    return trueFalseAnswer;
  }

  @JsonProperty("trueFalseAnswer")
  public void setTrueFalseAnswer(@Nullable Optional<Boolean> trueFalseAnswer) {
    this.trueFalseAnswer = trueFalseAnswer;
  }

  public QuestionUpdate numericAnswer(@Nullable Optional<String> numericAnswer) {
    this.numericAnswer = numericAnswer;
    return this;
  }

  /**
   * Correct numeric answer (or range start).
   * @return numericAnswer
   */
  
  @JsonProperty("numericAnswer")
  public @Nullable Optional<String> getNumericAnswer() {
    return numericAnswer;
  }

  @JsonProperty("numericAnswer")
  public void setNumericAnswer(@Nullable Optional<String> numericAnswer) {
    this.numericAnswer = numericAnswer;
  }

  public QuestionUpdate numericTolerance(@Nullable Optional<String> numericTolerance) {
    this.numericTolerance = numericTolerance;
    return this;
  }

  /**
   * Accepted error (or range end).
   * @return numericTolerance
   */
  
  @JsonProperty("numericTolerance")
  public @Nullable Optional<String> getNumericTolerance() {
    return numericTolerance;
  }

  @JsonProperty("numericTolerance")
  public void setNumericTolerance(@Nullable Optional<String> numericTolerance) {
    this.numericTolerance = numericTolerance;
  }

  public QuestionUpdate numericIsRange(@Nullable Optional<Boolean> numericIsRange) {
    this.numericIsRange = numericIsRange;
    return this;
  }

  /**
   * numeric_answer..numeric_tolerance is a range instead of value ± tolerance.
   * @return numericIsRange
   */
  
  @JsonProperty("numericIsRange")
  public @Nullable Optional<Boolean> getNumericIsRange() {
    return numericIsRange;
  }

  @JsonProperty("numericIsRange")
  public void setNumericIsRange(@Nullable Optional<Boolean> numericIsRange) {
    this.numericIsRange = numericIsRange;
  }

  public QuestionUpdate caseSensitive(@Nullable Optional<Boolean> caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /**
   * Text answers are case sensitive.
   * @return caseSensitive
   */
  
  @JsonProperty("caseSensitive")
  public @Nullable Optional<Boolean> getCaseSensitive() {
    return caseSensitive;
  }

  @JsonProperty("caseSensitive")
  public void setCaseSensitive(@Nullable Optional<Boolean> caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public QuestionUpdate allowSuperscript(@Nullable Optional<Boolean> allowSuperscript) {
    this.allowSuperscript = allowSuperscript;
    return this;
  }

  /**
   * Answer editor offers superscript.
   * @return allowSuperscript
   */
  
  @JsonProperty("allowSuperscript")
  public @Nullable Optional<Boolean> getAllowSuperscript() {
    return allowSuperscript;
  }

  @JsonProperty("allowSuperscript")
  public void setAllowSuperscript(@Nullable Optional<Boolean> allowSuperscript) {
    this.allowSuperscript = allowSuperscript;
  }

  public QuestionUpdate allowSubscript(@Nullable Optional<Boolean> allowSubscript) {
    this.allowSubscript = allowSubscript;
    return this;
  }

  /**
   * Answer editor offers subscript.
   * @return allowSubscript
   */
  
  @JsonProperty("allowSubscript")
  public @Nullable Optional<Boolean> getAllowSubscript() {
    return allowSubscript;
  }

  @JsonProperty("allowSubscript")
  public void setAllowSubscript(@Nullable Optional<Boolean> allowSubscript) {
    this.allowSubscript = allowSubscript;
  }

  public QuestionUpdate isRetired(@Nullable Optional<Boolean> isRetired) {
    this.isRetired = isRetired;
    return this;
  }

  /**
   * Removed from the bank (kept for past attempts).
   * @return isRetired
   */
  
  @JsonProperty("isRetired")
  public @Nullable Optional<Boolean> getIsRetired() {
    return isRetired;
  }

  @JsonProperty("isRetired")
  public void setIsRetired(@Nullable Optional<Boolean> isRetired) {
    this.isRetired = isRetired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionUpdate questionUpdate = (QuestionUpdate) o;
    return Objects.equals(this.questionCode, questionUpdate.questionCode) &&
        Objects.equals(this.questionText, questionUpdate.questionText) &&
        Objects.equals(this.questionType, questionUpdate.questionType) &&
        Objects.equals(this.allowsMultiple, questionUpdate.allowsMultiple) &&
        Objects.equals(this.marks, questionUpdate.marks) &&
        Objects.equals(this.title, questionUpdate.title) &&
        Objects.equals(this.shuffleOptions, questionUpdate.shuffleOptions) &&
        Objects.equals(this.optionLabelStyle, questionUpdate.optionLabelStyle) &&
        Objects.equals(this.maxSelections, questionUpdate.maxSelections) &&
        Objects.equals(this.difficulty, questionUpdate.difficulty) &&
        Objects.equals(this.generalFeedback, questionUpdate.generalFeedback) &&
        Objects.equals(this.correctFeedback, questionUpdate.correctFeedback) &&
        Objects.equals(this.incorrectFeedback, questionUpdate.incorrectFeedback) &&
        Objects.equals(this.modelAnswer, questionUpdate.modelAnswer) &&
        Objects.equals(this.trueFalseAnswer, questionUpdate.trueFalseAnswer) &&
        Objects.equals(this.numericAnswer, questionUpdate.numericAnswer) &&
        Objects.equals(this.numericTolerance, questionUpdate.numericTolerance) &&
        Objects.equals(this.numericIsRange, questionUpdate.numericIsRange) &&
        Objects.equals(this.caseSensitive, questionUpdate.caseSensitive) &&
        Objects.equals(this.allowSuperscript, questionUpdate.allowSuperscript) &&
        Objects.equals(this.allowSubscript, questionUpdate.allowSubscript) &&
        Objects.equals(this.isRetired, questionUpdate.isRetired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionCode, questionText, questionType, allowsMultiple, marks, title, shuffleOptions, optionLabelStyle, maxSelections, difficulty, generalFeedback, correctFeedback, incorrectFeedback, modelAnswer, trueFalseAnswer, numericAnswer, numericTolerance, numericIsRange, caseSensitive, allowSuperscript, allowSubscript, isRetired);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionUpdate {\n");
    sb.append("    questionCode: ").append(toIndentedString(questionCode)).append("\n");
    sb.append("    questionText: ").append(toIndentedString(questionText)).append("\n");
    sb.append("    questionType: ").append(toIndentedString(questionType)).append("\n");
    sb.append("    allowsMultiple: ").append(toIndentedString(allowsMultiple)).append("\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    shuffleOptions: ").append(toIndentedString(shuffleOptions)).append("\n");
    sb.append("    optionLabelStyle: ").append(toIndentedString(optionLabelStyle)).append("\n");
    sb.append("    maxSelections: ").append(toIndentedString(maxSelections)).append("\n");
    sb.append("    difficulty: ").append(toIndentedString(difficulty)).append("\n");
    sb.append("    generalFeedback: ").append(toIndentedString(generalFeedback)).append("\n");
    sb.append("    correctFeedback: ").append(toIndentedString(correctFeedback)).append("\n");
    sb.append("    incorrectFeedback: ").append(toIndentedString(incorrectFeedback)).append("\n");
    sb.append("    modelAnswer: ").append(toIndentedString(modelAnswer)).append("\n");
    sb.append("    trueFalseAnswer: ").append(toIndentedString(trueFalseAnswer)).append("\n");
    sb.append("    numericAnswer: ").append(toIndentedString(numericAnswer)).append("\n");
    sb.append("    numericTolerance: ").append(toIndentedString(numericTolerance)).append("\n");
    sb.append("    numericIsRange: ").append(toIndentedString(numericIsRange)).append("\n");
    sb.append("    caseSensitive: ").append(toIndentedString(caseSensitive)).append("\n");
    sb.append("    allowSuperscript: ").append(toIndentedString(allowSuperscript)).append("\n");
    sb.append("    allowSubscript: ").append(toIndentedString(allowSubscript)).append("\n");
    sb.append("    isRetired: ").append(toIndentedString(isRetired)).append("\n");
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

