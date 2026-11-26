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
 * AnswerGrade
 */

public class AnswerGrade {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID questionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  public AnswerGrade() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AnswerGrade(UUID questionId, String marks) {
    this.questionId = questionId;
    this.marks = marks;
  }

  public AnswerGrade questionId(UUID questionId) {
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

  public AnswerGrade marks(String marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Marks awarded.
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

  public AnswerGrade feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback on the answer.
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
    AnswerGrade answerGrade = (AnswerGrade) o;
    return Objects.equals(this.questionId, answerGrade.questionId) &&
        Objects.equals(this.marks, answerGrade.marks) &&
        Objects.equals(this.feedback, answerGrade.feedback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionId, marks, feedback);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnswerGrade {\n");
    sb.append("    questionId: ").append(toIndentedString(questionId)).append("\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
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

