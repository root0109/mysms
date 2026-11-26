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
 * Fields of CohortCourse that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortCourseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> evaluatorId;

  public CohortCourseUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
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

  public CohortCourseUpdate evaluatorId(@Nullable Optional<UUID> evaluatorId) {
    this.evaluatorId = evaluatorId;
    return this;
  }

  /**
   * Evaluator for this course.
   * @return evaluatorId
   */
  
  @JsonProperty("evaluatorId")
  public @Nullable Optional<UUID> getEvaluatorId() {
    return evaluatorId;
  }

  @JsonProperty("evaluatorId")
  public void setEvaluatorId(@Nullable Optional<UUID> evaluatorId) {
    this.evaluatorId = evaluatorId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortCourseUpdate cohortCourseUpdate = (CohortCourseUpdate) o;
    return Objects.equals(this.courseId, cohortCourseUpdate.courseId) &&
        Objects.equals(this.evaluatorId, cohortCourseUpdate.evaluatorId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, evaluatorId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortCourseUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    evaluatorId: ").append(toIndentedString(evaluatorId)).append("\n");
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

