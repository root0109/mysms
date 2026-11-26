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
 * Fields of QuestionHotspotRegion that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionHotspotRegionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> questionHotspotId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> shape;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 1000) String> coordinates;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isCorrect;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> feedback;

  public QuestionHotspotRegionUpdate questionHotspotId(@Nullable Optional<UUID> questionHotspotId) {
    this.questionHotspotId = questionHotspotId;
    return this;
  }

  /**
   * Hot-spot image.
   * @return questionHotspotId
   */
  
  @JsonProperty("questionHotspotId")
  public @Nullable Optional<UUID> getQuestionHotspotId() {
    return questionHotspotId;
  }

  @JsonProperty("questionHotspotId")
  public void setQuestionHotspotId(@Nullable Optional<UUID> questionHotspotId) {
    this.questionHotspotId = questionHotspotId;
  }

  public QuestionHotspotRegionUpdate label(@Nullable Optional<String> label) {
    this.label = label;
    return this;
  }

  /**
   * Label.
   * @return label
   */
  
  @JsonProperty("label")
  public @Nullable Optional<String> getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable Optional<String> label) {
    this.label = label;
  }

  public QuestionHotspotRegionUpdate shape(@Nullable Optional<String> shape) {
    this.shape = shape;
    return this;
  }

  /**
   * Region shape (rect, circle, polygon).
   * @return shape
   */
  
  @JsonProperty("shape")
  public @Nullable Optional<String> getShape() {
    return shape;
  }

  @JsonProperty("shape")
  public void setShape(@Nullable Optional<String> shape) {
    this.shape = shape;
  }

  public QuestionHotspotRegionUpdate coordinates(@Nullable Optional<String> coordinates) {
    this.coordinates = coordinates;
    return this;
  }

  /**
   * Region coordinates.
   * @return coordinates
   */
  
  @JsonProperty("coordinates")
  public @Nullable Optional<String> getCoordinates() {
    return coordinates;
  }

  @JsonProperty("coordinates")
  public void setCoordinates(@Nullable Optional<String> coordinates) {
    this.coordinates = coordinates;
  }

  public QuestionHotspotRegionUpdate isCorrect(@Nullable Optional<Boolean> isCorrect) {
    this.isCorrect = isCorrect;
    return this;
  }

  /**
   * Correct region.
   * @return isCorrect
   */
  
  @JsonProperty("isCorrect")
  public @Nullable Optional<Boolean> getIsCorrect() {
    return isCorrect;
  }

  @JsonProperty("isCorrect")
  public void setIsCorrect(@Nullable Optional<Boolean> isCorrect) {
    this.isCorrect = isCorrect;
  }

  public QuestionHotspotRegionUpdate feedback(@Nullable Optional<String> feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback.
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

