// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * CourseProgress
 */

public class CourseProgress {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String progressPercent;

  public CourseProgress() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CourseProgress(UUID courseId, String progressPercent) {
    this.courseId = courseId;
    this.progressPercent = progressPercent;
  }

  public CourseProgress courseId(UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
   * @return courseId
   */
  @NotNull @Valid 
  @JsonProperty("courseId")
  public UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  public CourseProgress progressPercent(String progressPercent) {
    this.progressPercent = progressPercent;
    return this;
  }

  /**
   * Completion.
   * @return progressPercent
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("progressPercent")
  public String getProgressPercent() {
    return progressPercent;
  }

  @JsonProperty("progressPercent")
  public void setProgressPercent(String progressPercent) {
    this.progressPercent = progressPercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseProgress courseProgress = (CourseProgress) o;
    return Objects.equals(this.courseId, courseProgress.courseId) &&
        Objects.equals(this.progressPercent, courseProgress.progressPercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, progressPercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseProgress {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    progressPercent: ").append(toIndentedString(progressPercent)).append("\n");
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

