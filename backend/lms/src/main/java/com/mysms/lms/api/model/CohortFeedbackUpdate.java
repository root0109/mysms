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
 * Fields of CohortFeedback that can be updated (partial update; see updateMask).
 */

public class CohortFeedbackUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contentRating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherRating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String valueRating;

  public CohortFeedbackUpdate feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Comments.
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

  public CohortFeedbackUpdate contentRating(@Nullable String contentRating) {
    this.contentRating = contentRating;
    return this;
  }

  /**
   * Content rating (0-5).
   * @return contentRating
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("contentRating")
  public @Nullable String getContentRating() {
    return contentRating;
  }

  @JsonProperty("contentRating")
  public void setContentRating(@Nullable String contentRating) {
    this.contentRating = contentRating;
  }

  public CohortFeedbackUpdate teacherRating(@Nullable String teacherRating) {
    this.teacherRating = teacherRating;
    return this;
  }

  /**
   * Teacher rating (0-5).
   * @return teacherRating
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("teacherRating")
  public @Nullable String getTeacherRating() {
    return teacherRating;
  }

  @JsonProperty("teacherRating")
  public void setTeacherRating(@Nullable String teacherRating) {
    this.teacherRating = teacherRating;
  }

  public CohortFeedbackUpdate valueRating(@Nullable String valueRating) {
    this.valueRating = valueRating;
    return this;
  }

  /**
   * Value-for-money rating (0-5).
   * @return valueRating
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("valueRating")
  public @Nullable String getValueRating() {
    return valueRating;
  }

  @JsonProperty("valueRating")
  public void setValueRating(@Nullable String valueRating) {
    this.valueRating = valueRating;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortFeedbackUpdate cohortFeedbackUpdate = (CohortFeedbackUpdate) o;
    return Objects.equals(this.feedback, cohortFeedbackUpdate.feedback) &&
        Objects.equals(this.contentRating, cohortFeedbackUpdate.contentRating) &&
        Objects.equals(this.teacherRating, cohortFeedbackUpdate.teacherRating) &&
        Objects.equals(this.valueRating, cohortFeedbackUpdate.valueRating);
  }

  @Override
  public int hashCode() {
    return Objects.hash(feedback, contentRating, teacherRating, valueRating);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortFeedbackUpdate {\n");
    sb.append("    feedback: ").append(toIndentedString(feedback)).append("\n");
    sb.append("    contentRating: ").append(toIndentedString(contentRating)).append("\n");
    sb.append("    teacherRating: ").append(toIndentedString(teacherRating)).append("\n");
    sb.append("    valueRating: ").append(toIndentedString(valueRating)).append("\n");
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

