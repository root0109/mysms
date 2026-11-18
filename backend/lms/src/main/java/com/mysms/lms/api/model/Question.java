// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Question bank item.
 */

public class Question {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID supersededById;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String questionCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String questionText;

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
  private @Nullable QuestionTypeEnum questionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowsMultiple;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean shuffleOptions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String optionLabelStyle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxSelections;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String difficulty;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String generalFeedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String correctFeedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String incorrectFeedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String modelAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean trueFalseAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String numericAnswer;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String numericTolerance;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean numericIsRange;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean caseSensitive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowSuperscript;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowSubscript;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isRetired;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Question version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Version number; published versions are immutable, edits create a new version.
   * @return version
   */
  
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public Question supersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
    return this;
  }

  /**
   * Newer version that replaced this one.
   * @return supersededById
   */
  @Valid 
  @JsonProperty("supersededById")
  public @Nullable UUID getSupersededById() {
    return supersededById;
  }

  @JsonProperty("supersededById")
  public void setSupersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
  }

  public Question publishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
    return this;
  }

  /**
   * When this version was published (absent = draft).
   * @return publishedAt
   */
  @Valid 
  @JsonProperty("publishedAt")
  public @Nullable OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  @JsonProperty("publishedAt")
  public void setPublishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  public Question id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public Question questionCode(@Nullable String questionCode) {
    this.questionCode = questionCode;
    return this;
  }

  /**
   * Human-readable code.
   * @return questionCode
   */
  @Size(max = 30) 
  @JsonProperty("questionCode")
  public @Nullable String getQuestionCode() {
    return questionCode;
  }

  @JsonProperty("questionCode")
  public void setQuestionCode(@Nullable String questionCode) {
    this.questionCode = questionCode;
  }

  public Question questionText(@Nullable String questionText) {
    this.questionText = questionText;
    return this;
  }

  /**
   * Question.
   * @return questionText
   */
  
  @JsonProperty("questionText")
  public @Nullable String getQuestionText() {
    return questionText;
  }

  @JsonProperty("questionText")
  public void setQuestionText(@Nullable String questionText) {
    this.questionText = questionText;
  }

  public Question questionType(@Nullable QuestionTypeEnum questionType) {
    this.questionType = questionType;
    return this;
  }

  /**
   * Type.
   * @return questionType
   */
  
  @JsonProperty("questionType")
  public @Nullable QuestionTypeEnum getQuestionType() {
    return questionType;
  }

  @JsonProperty("questionType")
  public void setQuestionType(@Nullable QuestionTypeEnum questionType) {
    this.questionType = questionType;
  }

  public Question allowsMultiple(@Nullable Boolean allowsMultiple) {
    this.allowsMultiple = allowsMultiple;
    return this;
  }

  /**
   * More than one choice may be correct.
   * @return allowsMultiple
   */
  
  @JsonProperty("allowsMultiple")
  public @Nullable Boolean getAllowsMultiple() {
    return allowsMultiple;
  }

  @JsonProperty("allowsMultiple")
  public void setAllowsMultiple(@Nullable Boolean allowsMultiple) {
    this.allowsMultiple = allowsMultiple;
  }

  public Question marks(@Nullable String marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Default marks.
   * @return marks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marks")
  public @Nullable String getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(@Nullable String marks) {
    this.marks = marks;
  }

  public Question title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Short title shown in the question bank.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public Question shuffleOptions(@Nullable Boolean shuffleOptions) {
    this.shuffleOptions = shuffleOptions;
    return this;
  }

  /**
   * Shuffle answer options per attempt.
   * @return shuffleOptions
   */
  
  @JsonProperty("shuffleOptions")
  public @Nullable Boolean getShuffleOptions() {
    return shuffleOptions;
  }

  @JsonProperty("shuffleOptions")
  public void setShuffleOptions(@Nullable Boolean shuffleOptions) {
    this.shuffleOptions = shuffleOptions;
  }

  public Question optionLabelStyle(@Nullable String optionLabelStyle) {
    this.optionLabelStyle = optionLabelStyle;
    return this;
  }

  /**
   * Option labelling (A-B-C, a-b-c, 1-2-3, i-ii-iii).
   * @return optionLabelStyle
   */
  @Size(max = 20) 
  @JsonProperty("optionLabelStyle")
  public @Nullable String getOptionLabelStyle() {
    return optionLabelStyle;
  }

  @JsonProperty("optionLabelStyle")
  public void setOptionLabelStyle(@Nullable String optionLabelStyle) {
    this.optionLabelStyle = optionLabelStyle;
  }

  public Question maxSelections(@Nullable Integer maxSelections) {
    this.maxSelections = maxSelections;
    return this;
  }

  /**
   * Maximum options a student may tick (multiple response).
   * @return maxSelections
   */
  
  @JsonProperty("maxSelections")
  public @Nullable Integer getMaxSelections() {
    return maxSelections;
  }

  @JsonProperty("maxSelections")
  public void setMaxSelections(@Nullable Integer maxSelections) {
    this.maxSelections = maxSelections;
  }

  public Question difficulty(@Nullable String difficulty) {
    this.difficulty = difficulty;
    return this;
  }

  /**
   * Difficulty / complexity level.
   * @return difficulty
   */
  @Size(max = 20) 
  @JsonProperty("difficulty")
  public @Nullable String getDifficulty() {
    return difficulty;
  }

  @JsonProperty("difficulty")
  public void setDifficulty(@Nullable String difficulty) {
    this.difficulty = difficulty;
  }

  public Question generalFeedback(@Nullable String generalFeedback) {
    this.generalFeedback = generalFeedback;
    return this;
  }

  /**
   * Feedback shown after answering.
   * @return generalFeedback
   */
  
  @JsonProperty("generalFeedback")
  public @Nullable String getGeneralFeedback() {
    return generalFeedback;
  }

  @JsonProperty("generalFeedback")
  public void setGeneralFeedback(@Nullable String generalFeedback) {
    this.generalFeedback = generalFeedback;
  }

  public Question correctFeedback(@Nullable String correctFeedback) {
    this.correctFeedback = correctFeedback;
    return this;
  }

  /**
   * Feedback when correct.
   * @return correctFeedback
   */
  
  @JsonProperty("correctFeedback")
  public @Nullable String getCorrectFeedback() {
    return correctFeedback;
  }

  @JsonProperty("correctFeedback")
  public void setCorrectFeedback(@Nullable String correctFeedback) {
    this.correctFeedback = correctFeedback;
  }

  public Question incorrectFeedback(@Nullable String incorrectFeedback) {
    this.incorrectFeedback = incorrectFeedback;
    return this;
  }

  /**
   * Feedback when incorrect.
   * @return incorrectFeedback
   */
  
  @JsonProperty("incorrectFeedback")
  public @Nullable String getIncorrectFeedback() {
    return incorrectFeedback;
  }

  @JsonProperty("incorrectFeedback")
  public void setIncorrectFeedback(@Nullable String incorrectFeedback) {
    this.incorrectFeedback = incorrectFeedback;
  }

  public Question modelAnswer(@Nullable String modelAnswer) {
    this.modelAnswer = modelAnswer;
    return this;
  }

  /**
   * Model answer (open-ended / short answer).
   * @return modelAnswer
   */
  
  @JsonProperty("modelAnswer")
  public @Nullable String getModelAnswer() {
    return modelAnswer;
  }

  @JsonProperty("modelAnswer")
  public void setModelAnswer(@Nullable String modelAnswer) {
    this.modelAnswer = modelAnswer;
  }

  public Question trueFalseAnswer(@Nullable Boolean trueFalseAnswer) {
    this.trueFalseAnswer = trueFalseAnswer;
    return this;
  }

  /**
   * Correct answer of a true/false question.
   * @return trueFalseAnswer
   */
  
  @JsonProperty("trueFalseAnswer")
  public @Nullable Boolean getTrueFalseAnswer() {
    return trueFalseAnswer;
  }

  @JsonProperty("trueFalseAnswer")
  public void setTrueFalseAnswer(@Nullable Boolean trueFalseAnswer) {
    this.trueFalseAnswer = trueFalseAnswer;
  }

  public Question numericAnswer(@Nullable String numericAnswer) {
    this.numericAnswer = numericAnswer;
    return this;
  }

  /**
   * Correct numeric answer (or range start).
   * @return numericAnswer
   */
  @Size(max = 50) 
  @JsonProperty("numericAnswer")
  public @Nullable String getNumericAnswer() {
    return numericAnswer;
  }

  @JsonProperty("numericAnswer")
  public void setNumericAnswer(@Nullable String numericAnswer) {
    this.numericAnswer = numericAnswer;
  }

  public Question numericTolerance(@Nullable String numericTolerance) {
    this.numericTolerance = numericTolerance;
    return this;
  }

  /**
   * Accepted error (or range end).
   * @return numericTolerance
   */
  @Size(max = 50) 
  @JsonProperty("numericTolerance")
  public @Nullable String getNumericTolerance() {
    return numericTolerance;
  }

  @JsonProperty("numericTolerance")
  public void setNumericTolerance(@Nullable String numericTolerance) {
    this.numericTolerance = numericTolerance;
  }

  public Question numericIsRange(@Nullable Boolean numericIsRange) {
    this.numericIsRange = numericIsRange;
    return this;
  }

  /**
   * numeric_answer..numeric_tolerance is a range instead of value ± tolerance.
   * @return numericIsRange
   */
  
  @JsonProperty("numericIsRange")
  public @Nullable Boolean getNumericIsRange() {
    return numericIsRange;
  }

  @JsonProperty("numericIsRange")
  public void setNumericIsRange(@Nullable Boolean numericIsRange) {
    this.numericIsRange = numericIsRange;
  }

  public Question caseSensitive(@Nullable Boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /**
   * Text answers are case sensitive.
   * @return caseSensitive
   */
  
  @JsonProperty("caseSensitive")
  public @Nullable Boolean getCaseSensitive() {
    return caseSensitive;
  }

  @JsonProperty("caseSensitive")
  public void setCaseSensitive(@Nullable Boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public Question allowSuperscript(@Nullable Boolean allowSuperscript) {
    this.allowSuperscript = allowSuperscript;
    return this;
  }

  /**
   * Answer editor offers superscript.
   * @return allowSuperscript
   */
  
  @JsonProperty("allowSuperscript")
  public @Nullable Boolean getAllowSuperscript() {
    return allowSuperscript;
  }

  @JsonProperty("allowSuperscript")
  public void setAllowSuperscript(@Nullable Boolean allowSuperscript) {
    this.allowSuperscript = allowSuperscript;
  }

  public Question allowSubscript(@Nullable Boolean allowSubscript) {
    this.allowSubscript = allowSubscript;
    return this;
  }

  /**
   * Answer editor offers subscript.
   * @return allowSubscript
   */
  
  @JsonProperty("allowSubscript")
  public @Nullable Boolean getAllowSubscript() {
    return allowSubscript;
  }

  @JsonProperty("allowSubscript")
  public void setAllowSubscript(@Nullable Boolean allowSubscript) {
    this.allowSubscript = allowSubscript;
  }

  public Question isRetired(@Nullable Boolean isRetired) {
    this.isRetired = isRetired;
    return this;
  }

  /**
   * Removed from the bank (kept for past attempts).
   * @return isRetired
   */
  
  @JsonProperty("isRetired")
  public @Nullable Boolean getIsRetired() {
    return isRetired;
  }

  @JsonProperty("isRetired")
  public void setIsRetired(@Nullable Boolean isRetired) {
    this.isRetired = isRetired;
  }

  public Question createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Question createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Question updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Question updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Question etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Question question = (Question) o;
    return Objects.equals(this.version, question.version) &&
        Objects.equals(this.supersededById, question.supersededById) &&
        Objects.equals(this.publishedAt, question.publishedAt) &&
        Objects.equals(this.id, question.id) &&
        Objects.equals(this.questionCode, question.questionCode) &&
        Objects.equals(this.questionText, question.questionText) &&
        Objects.equals(this.questionType, question.questionType) &&
        Objects.equals(this.allowsMultiple, question.allowsMultiple) &&
        Objects.equals(this.marks, question.marks) &&
        Objects.equals(this.title, question.title) &&
        Objects.equals(this.shuffleOptions, question.shuffleOptions) &&
        Objects.equals(this.optionLabelStyle, question.optionLabelStyle) &&
        Objects.equals(this.maxSelections, question.maxSelections) &&
        Objects.equals(this.difficulty, question.difficulty) &&
        Objects.equals(this.generalFeedback, question.generalFeedback) &&
        Objects.equals(this.correctFeedback, question.correctFeedback) &&
        Objects.equals(this.incorrectFeedback, question.incorrectFeedback) &&
        Objects.equals(this.modelAnswer, question.modelAnswer) &&
        Objects.equals(this.trueFalseAnswer, question.trueFalseAnswer) &&
        Objects.equals(this.numericAnswer, question.numericAnswer) &&
        Objects.equals(this.numericTolerance, question.numericTolerance) &&
        Objects.equals(this.numericIsRange, question.numericIsRange) &&
        Objects.equals(this.caseSensitive, question.caseSensitive) &&
        Objects.equals(this.allowSuperscript, question.allowSuperscript) &&
        Objects.equals(this.allowSubscript, question.allowSubscript) &&
        Objects.equals(this.isRetired, question.isRetired) &&
        Objects.equals(this.createTime, question.createTime) &&
        Objects.equals(this.createdBy, question.createdBy) &&
        Objects.equals(this.updateTime, question.updateTime) &&
        Objects.equals(this.updatedBy, question.updatedBy) &&
        Objects.equals(this.etag, question.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, supersededById, publishedAt, id, questionCode, questionText, questionType, allowsMultiple, marks, title, shuffleOptions, optionLabelStyle, maxSelections, difficulty, generalFeedback, correctFeedback, incorrectFeedback, modelAnswer, trueFalseAnswer, numericAnswer, numericTolerance, numericIsRange, caseSensitive, allowSuperscript, allowSubscript, isRetired, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Question {\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    supersededById: ").append(toIndentedString(supersededById)).append("\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

