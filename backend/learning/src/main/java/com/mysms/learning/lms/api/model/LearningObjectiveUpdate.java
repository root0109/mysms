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
 * Fields of LearningObjective that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LearningObjectiveUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> objective;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> copiedFromId;

  public LearningObjectiveUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (course-level objective).
   * @return courseId
   */
  
  @JsonProperty("courseId")
  public @Nullable Optional<UUID> getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
  }

  public LearningObjectiveUpdate chapterId(@Nullable Optional<UUID> chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter (chapter-level objective; course or cohort chapter).
   * @return chapterId
   */
  
  @JsonProperty("chapterId")
  public @Nullable Optional<UUID> getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(@Nullable Optional<UUID> chapterId) {
    this.chapterId = chapterId;
  }

  public LearningObjectiveUpdate objective(@Nullable Optional<String> objective) {
    this.objective = objective;
    return this;
  }

  /**
   * Objective statement.
   * @return objective
   */
  
  @JsonProperty("objective")
  public @Nullable Optional<String> getObjective() {
    return objective;
  }

  @JsonProperty("objective")
  public void setObjective(@Nullable Optional<String> objective) {
    this.objective = objective;
  }

  public LearningObjectiveUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  public LearningObjectiveUpdate copiedFromId(@Nullable Optional<UUID> copiedFromId) {
    this.copiedFromId = copiedFromId;
    return this;
  }

  /**
   * Template objective this cohort copy was made from.
   * @return copiedFromId
   */
  
  @JsonProperty("copiedFromId")
  public @Nullable Optional<UUID> getCopiedFromId() {
    return copiedFromId;
  }

  @JsonProperty("copiedFromId")
  public void setCopiedFromId(@Nullable Optional<UUID> copiedFromId) {
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

