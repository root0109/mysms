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
 * Fields of QuestionHotspotRegion that can be updated (partial update; see updateMask).
 */

public class QuestionHotspotRegionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID questionHotspotId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String shape;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String coordinates;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isCorrect;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  public QuestionHotspotRegionUpdate questionHotspotId(@Nullable UUID questionHotspotId) {
    this.questionHotspotId = questionHotspotId;
    return this;
  }

  /**
   * Hot-spot image.
   * @return questionHotspotId
   */
  @Valid 
  @JsonProperty("questionHotspotId")
  public @Nullable UUID getQuestionHotspotId() {
    return questionHotspotId;
  }

  @JsonProperty("questionHotspotId")
  public void setQuestionHotspotId(@Nullable UUID questionHotspotId) {
    this.questionHotspotId = questionHotspotId;
  }

  public QuestionHotspotRegionUpdate label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Label.
   * @return label
   */
  @Size(max = 200) 
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public QuestionHotspotRegionUpdate shape(@Nullable String shape) {
    this.shape = shape;
    return this;
  }

  /**
   * Region shape (rect, circle, polygon).
   * @return shape
   */
  @Size(max = 40) 
  @JsonProperty("shape")
  public @Nullable String getShape() {
    return shape;
  }

  @JsonProperty("shape")
  public void setShape(@Nullable String shape) {
    this.shape = shape;
  }

  public QuestionHotspotRegionUpdate coordinates(@Nullable String coordinates) {
    this.coordinates = coordinates;
    return this;
  }

  /**
   * Region coordinates.
   * @return coordinates
   */
  @Size(max = 1000) 
  @JsonProperty("coordinates")
  public @Nullable String getCoordinates() {
    return coordinates;
  }

  @JsonProperty("coordinates")
  public void setCoordinates(@Nullable String coordinates) {
    this.coordinates = coordinates;
  }

  public QuestionHotspotRegionUpdate isCorrect(@Nullable Boolean isCorrect) {
    this.isCorrect = isCorrect;
    return this;
  }

  /**
   * Correct region.
   * @return isCorrect
   */
  
  @JsonProperty("isCorrect")
  public @Nullable Boolean getIsCorrect() {
    return isCorrect;
  }

  @JsonProperty("isCorrect")
  public void setIsCorrect(@Nullable Boolean isCorrect) {
    this.isCorrect = isCorrect;
  }

  public QuestionHotspotRegionUpdate feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback.
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
    QuestionHotspotRegionUpdate questionHotspotRegionUpdate = (QuestionHotspotRegionUpdate) o;
    return Objects.equals(this.questionHotspotId, questionHotspotRegionUpdate.questionHotspotId) &&
        Objects.equals(this.label, questionHotspotRegionUpdate.label) &&
        Objects.equals(this.shape, questionHotspotRegionUpdate.shape) &&
        Objects.equals(this.coordinates, questionHotspotRegionUpdate.coordinates) &&
        Objects.equals(this.isCorrect, questionHotspotRegionUpdate.isCorrect) &&
        Objects.equals(this.feedback, questionHotspotRegionUpdate.feedback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionHotspotId, label, shape, coordinates, isCorrect, feedback);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionHotspotRegionUpdate {\n");
    sb.append("    questionHotspotId: ").append(toIndentedString(questionHotspotId)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    shape: ").append(toIndentedString(shape)).append("\n");
    sb.append("    coordinates: ").append(toIndentedString(coordinates)).append("\n");
    sb.append("    isCorrect: ").append(toIndentedString(isCorrect)).append("\n");
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

