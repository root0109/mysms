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
 * Fields of LearningObjective that can be updated (partial update; see updateMask).
 */

public class LearningObjectiveUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String objective;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID copiedFromId;

  public LearningObjectiveUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (course-level objective).
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public LearningObjectiveUpdate chapterId(@Nullable UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter (chapter-level objective; course or cohort chapter).
   * @return chapterId
   */
  @Valid 
  @JsonProperty("chapterId")
  public @Nullable UUID getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(@Nullable UUID chapterId) {
    this.chapterId = chapterId;
  }

  public LearningObjectiveUpdate objective(@Nullable String objective) {
    this.objective = objective;
    return this;
  }

  /**
   * Objective statement.
   * @return objective
   */
  
  @JsonProperty("objective")
  public @Nullable String getObjective() {
    return objective;
  }

  @JsonProperty("objective")
  public void setObjective(@Nullable String objective) {
    this.objective = objective;
  }

  public LearningObjectiveUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public LearningObjectiveUpdate copiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
    return this;
  }

  /**
   * Template objective this cohort copy was made from.
   * @return copiedFromId
   */
  @Valid 
  @JsonProperty("copiedFromId")
  public @Nullable UUID getCopiedFromId() {
    return copiedFromId;
  }

  @JsonProperty("copiedFromId")
  public void setCopiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LearningObjectiveUpdate learningObjectiveUpdate = (LearningObjectiveUpdate) o;
    return Objects.equals(this.courseId, learningObjectiveUpdate.courseId) &&
        Objects.equals(this.chapterId, learningObjectiveUpdate.chapterId) &&
        Objects.equals(this.objective, learningObjectiveUpdate.objective) &&
        Objects.equals(this.sortOrder, learningObjectiveUpdate.sortOrder) &&
        Objects.equals(this.copiedFromId, learningObjectiveUpdate.copiedFromId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, chapterId, objective, sortOrder, copiedFromId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LearningObjectiveUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
    sb.append("    objective: ").append(toIndentedString(objective)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    copiedFromId: ").append(toIndentedString(copiedFromId)).append("\n");
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

