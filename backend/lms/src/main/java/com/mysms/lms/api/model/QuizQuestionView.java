// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mysms.lms.api.model.BlankView;
import com.mysms.lms.api.model.OptionView;
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
 * QuizQuestionView
 */

public class QuizQuestionView {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID quizQuestionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID questionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String questionText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String questionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean allowsMultiple;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid OptionView> options = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxSelections;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid BlankView> blanks = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid OptionView> matchLeft = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid OptionView> matchRight = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID hotspotImageFileId;

  /**
   * Hot-spot selection mode.
   */
  public enum HotspotSelectionEnum {
    SINGLE("single"),
    
    MULTIPLE("multiple");

    private final String value;

    HotspotSelectionEnum(String value) {
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
    public static HotspotSelectionEnum fromValue(String value) {
      for (HotspotSelectionEnum b : HotspotSelectionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable HotspotSelectionEnum hotspotSelection;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean caseSensitive;

  public QuizQuestionView() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuizQuestionView(UUID quizQuestionId, UUID questionId, String questionText, String questionType, Boolean allowsMultiple, String marks) {
    this.quizQuestionId = quizQuestionId;
    this.questionId = questionId;
    this.questionText = questionText;
    this.questionType = questionType;
    this.allowsMultiple = allowsMultiple;
    this.marks = marks;
  }

  public QuizQuestionView quizQuestionId(UUID quizQuestionId) {
    this.quizQuestionId = quizQuestionId;
    return this;
  }

  /**
   * Quiz question.
   * @return quizQuestionId
   */
  @NotNull @Valid 
  @JsonProperty("quizQuestionId")
  public UUID getQuizQuestionId() {
    return quizQuestionId;
  }

  @JsonProperty("quizQuestionId")
  public void setQuizQuestionId(UUID quizQuestionId) {
    this.quizQuestionId = quizQuestionId;
  }

  public QuizQuestionView questionId(UUID questionId) {
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

  public QuizQuestionView questionText(String questionText) {
    this.questionText = questionText;
    return this;
  }

  /**
   * Question.
   * @return questionText
   */
  @NotNull 
  @JsonProperty("questionText")
  public String getQuestionText() {
    return questionText;
  }

  @JsonProperty("questionText")
  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public QuizQuestionView questionType(String questionType) {
    this.questionType = questionType;
    return this;
  }

  /**
   * Type.
   * @return questionType
   */
  @NotNull 
  @JsonProperty("questionType")
  public String getQuestionType() {
    return questionType;
  }

  @JsonProperty("questionType")
  public void setQuestionType(String questionType) {
    this.questionType = questionType;
  }

  public QuizQuestionView allowsMultiple(Boolean allowsMultiple) {
    this.allowsMultiple = allowsMultiple;
    return this;
  }

  /**
   * Multiple answers allowed.
   * @return allowsMultiple
   */
  @NotNull 
  @JsonProperty("allowsMultiple")
  public Boolean getAllowsMultiple() {
    return allowsMultiple;
  }

  @JsonProperty("allowsMultiple")
  public void setAllowsMultiple(Boolean allowsMultiple) {
    this.allowsMultiple = allowsMultiple;
  }

  public QuizQuestionView marks(String marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Marks.
   * @return marks
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marks")
  public String getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(String marks) {
    this.marks = marks;
  }

  public QuizQuestionView options(List<@Valid OptionView> options) {
    this.options = options;
    return this;
  }

  public QuizQuestionView addOptionsItem(OptionView optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * Choices (correctness hidden).
   * @return options
   */
  
  @JsonProperty("options")
  public List<@Valid OptionView> getOptions() {
    return options;
  }

  @JsonProperty("options")
  public void setOptions(List<@Valid OptionView> options) {
    this.options = options;
  }

  public QuizQuestionView maxSelections(@Nullable Integer maxSelections) {
    this.maxSelections = maxSelections;
    return this;
  }

  /**
   * Maximum options that may be ticked.
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

  public QuizQuestionView blanks(List<@Valid BlankView> blanks) {
    this.blanks = blanks;
    return this;
  }

  public QuizQuestionView addBlanksItem(BlankView blanksItem) {
    if (this.blanks == null) {
      this.blanks = new ArrayList<>();
    }
    this.blanks.add(blanksItem);
    return this;
  }

  /**
   * Blanks of a fill-in-the-blanks question.
   * @return blanks
   */
  
  @JsonProperty("blanks")
  public List<@Valid BlankView> getBlanks() {
    return blanks;
  }

  @JsonProperty("blanks")
  public void setBlanks(List<@Valid BlankView> blanks) {
    this.blanks = blanks;
  }

  public QuizQuestionView matchLeft(List<@Valid OptionView> matchLeft) {
    this.matchLeft = matchLeft;
    return this;
  }

  public QuizQuestionView addMatchLeftItem(OptionView matchLeftItem) {
    if (this.matchLeft == null) {
      this.matchLeft = new ArrayList<>();
    }
    this.matchLeft.add(matchLeftItem);
    return this;
  }

  /**
   * Left column of a matching question.
   * @return matchLeft
   */
  
  @JsonProperty("matchLeft")
  public List<@Valid OptionView> getMatchLeft() {
    return matchLeft;
  }

  @JsonProperty("matchLeft")
  public void setMatchLeft(List<@Valid OptionView> matchLeft) {
    this.matchLeft = matchLeft;
  }

  public QuizQuestionView matchRight(List<@Valid OptionView> matchRight) {
    this.matchRight = matchRight;
    return this;
  }

  public QuizQuestionView addMatchRightItem(OptionView matchRightItem) {
    if (this.matchRight == null) {
      this.matchRight = new ArrayList<>();
    }
    this.matchRight.add(matchRightItem);
    return this;
  }

  /**
   * Right column of a matching question, shuffled.
   * @return matchRight
   */
  
  @JsonProperty("matchRight")
  public List<@Valid OptionView> getMatchRight() {
    return matchRight;
  }

  @JsonProperty("matchRight")
  public void setMatchRight(List<@Valid OptionView> matchRight) {
    this.matchRight = matchRight;
  }

  public QuizQuestionView hotspotImageFileId(@Nullable UUID hotspotImageFileId) {
    this.hotspotImageFileId = hotspotImageFileId;
    return this;
  }

  /**
   * Image of a hot-spot question.
   * @return hotspotImageFileId
   */
  @Valid 
  @JsonProperty("hotspotImageFileId")
  public @Nullable UUID getHotspotImageFileId() {
    return hotspotImageFileId;
  }

  @JsonProperty("hotspotImageFileId")
  public void setHotspotImageFileId(@Nullable UUID hotspotImageFileId) {
    this.hotspotImageFileId = hotspotImageFileId;
  }

  public QuizQuestionView hotspotSelection(@Nullable HotspotSelectionEnum hotspotSelection) {
    this.hotspotSelection = hotspotSelection;
    return this;
  }

  /**
   * Hot-spot selection mode.
   * @return hotspotSelection
   */
  
  @JsonProperty("hotspotSelection")
  public @Nullable HotspotSelectionEnum getHotspotSelection() {
    return hotspotSelection;
  }

  @JsonProperty("hotspotSelection")
  public void setHotspotSelection(@Nullable HotspotSelectionEnum hotspotSelection) {
    this.hotspotSelection = hotspotSelection;
  }

  public QuizQuestionView caseSensitive(@Nullable Boolean caseSensitive) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuizQuestionView quizQuestionView = (QuizQuestionView) o;
    return Objects.equals(this.quizQuestionId, quizQuestionView.quizQuestionId) &&
        Objects.equals(this.questionId, quizQuestionView.questionId) &&
        Objects.equals(this.questionText, quizQuestionView.questionText) &&
        Objects.equals(this.questionType, quizQuestionView.questionType) &&
        Objects.equals(this.allowsMultiple, quizQuestionView.allowsMultiple) &&
        Objects.equals(this.marks, quizQuestionView.marks) &&
        Objects.equals(this.options, quizQuestionView.options) &&
        Objects.equals(this.maxSelections, quizQuestionView.maxSelections) &&
        Objects.equals(this.blanks, quizQuestionView.blanks) &&
        Objects.equals(this.matchLeft, quizQuestionView.matchLeft) &&
        Objects.equals(this.matchRight, quizQuestionView.matchRight) &&
        Objects.equals(this.hotspotImageFileId, quizQuestionView.hotspotImageFileId) &&
        Objects.equals(this.hotspotSelection, quizQuestionView.hotspotSelection) &&
        Objects.equals(this.caseSensitive, quizQuestionView.caseSensitive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(quizQuestionId, questionId, questionText, questionType, allowsMultiple, marks, options, maxSelections, blanks, matchLeft, matchRight, hotspotImageFileId, hotspotSelection, caseSensitive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizQuestionView {\n");
    sb.append("    quizQuestionId: ").append(toIndentedString(quizQuestionId)).append("\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
    sb.append("    questionText: ").append(toIndentedString(questionText)).append("\n");
    sb.append("    questionType: ").append(toIndentedString(questionType)).append("\n");
    sb.append("    allowsMultiple: ").append(toIndentedString(allowsMultiple)).append("\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    maxSelections: ").append(toIndentedString(maxSelections)).append("\n");
    sb.append("    blanks: ").append(toIndentedString(blanks)).append("\n");
    sb.append("    matchLeft: ").append(toIndentedString(matchLeft)).append("\n");
    sb.append("    matchRight: ").append(toIndentedString(matchRight)).append("\n");
    sb.append("    hotspotImageFileId: ").append(toIndentedString(hotspotImageFileId)).append("\n");
    sb.append("    hotspotSelection: ").append(toIndentedString(hotspotSelection)).append("\n");
    sb.append("    caseSensitive: ").append(toIndentedString(caseSensitive)).append("\n");
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

