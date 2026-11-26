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
 * Fields of CohortFeedback that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortFeedbackUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> feedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> contentRating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> teacherRating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> valueRating;

  public CohortFeedbackUpdate feedback(@Nullable Optional<String> feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Comments.
   * @return feedback
   */
  
  @JsonProperty("feedback")
  public @Nullable Optional<String> getFeedback() {
    return feedback;
  }

  @JsonProperty("feedback")
  public void setFeedback(@Nullable Optional<String> feedback) {
    this.feedback = feedback;
  }

  public CohortFeedbackUpdate contentRating(@Nullable Optional<String> contentRating) {
    this.contentRating = contentRating;
    return this;
  }

  /**
   * Content rating (0-5).
   * @return contentRating
   */
  
  @JsonProperty("contentRating")
  public @Nullable Optional<String> getContentRating() {
    return contentRating;
  }

  @JsonProperty("contentRating")
  public void setContentRating(@Nullable Optional<String> contentRating) {
    this.contentRating = contentRating;
  }

  public CohortFeedbackUpdate teacherRating(@Nullable Optional<String> teacherRating) {
    this.teacherRating = teacherRating;
    return this;
  }

  /**
   * Teacher rating (0-5).
   * @return teacherRating
   */
  
  @JsonProperty("teacherRating")
  public @Nullable Optional<String> getTeacherRating() {
    return teacherRating;
  }

  @JsonProperty("teacherRating")
  public void setTeacherRating(@Nullable Optional<String> teacherRating) {
    this.teacherRating = teacherRating;
  }

  public CohortFeedbackUpdate valueRating(@Nullable Optional<String> valueRating) {
    this.valueRating = valueRating;
    return this;
  }

  /**
   * Value-for-money rating (0-5).
   * @return valueRating
   */
  
  @JsonProperty("valueRating")
  public @Nullable Optional<String> getValueRating() {
    return valueRating;
  }

  @JsonProperty("valueRating")
  public void setValueRating(@Nullable Optional<String> valueRating) {
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

