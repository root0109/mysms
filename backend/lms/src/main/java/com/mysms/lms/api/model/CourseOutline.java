// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.CourseOutlineChapter;
import com.mysms.lms.api.model.LmsCourse;
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
 * CourseOutline
 */

public class CourseOutline {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LmsCourse course;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CourseOutlineChapter> chapters = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String progressPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID enrollmentId;

  public CourseOutline() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CourseOutline(LmsCourse course) {
    this.course = course;
  }

  public CourseOutline course(LmsCourse course) {
    this.course = course;
    return this;
  }

  /**
   * Get course
   * @return course
   */
  @NotNull @Valid 
  @JsonProperty("course")
  public LmsCourse getCourse() {
    return course;
  }

  @JsonProperty("course")
  public void setCourse(LmsCourse course) {
    this.course = course;
  }

  public CourseOutline chapters(List<@Valid CourseOutlineChapter> chapters) {
    this.chapters = chapters;
    return this;
  }

  public CourseOutline addChaptersItem(CourseOutlineChapter chaptersItem) {
    if (this.chapters == null) {
      this.chapters = new ArrayList<>();
    }
    this.chapters.add(chaptersItem);
    return this;
  }

  /**
   * Chapters.
   * @return chapters
   */
  
  @JsonProperty("chapters")
  public List<@Valid CourseOutlineChapter> getChapters() {
    return chapters;
  }

  @JsonProperty("chapters")
  public void setChapters(List<@Valid CourseOutlineChapter> chapters) {
    this.chapters = chapters;
  }

  public CourseOutline progressPercent(@Nullable String progressPercent) {
    this.progressPercent = progressPercent;
    return this;
  }

  /**
   * Caller progress.
   * @return progressPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("progressPercent")
  public @Nullable String getProgressPercent() {
    return progressPercent;
  }

  @JsonProperty("progressPercent")
  public void setProgressPercent(@Nullable String progressPercent) {
    this.progressPercent = progressPercent;
  }

  public CourseOutline enrollmentId(@Nullable UUID enrollmentId) {
    this.enrollmentId = enrollmentId;
    return this;
  }

  /**
   * Caller enrolment.
   * @return enrollmentId
   */
  @Valid 
  @JsonProperty("enrollmentId")
  public @Nullable UUID getEnrollmentId() {
    return enrollmentId;
  }

  @JsonProperty("enrollmentId")
  public void setEnrollmentId(@Nullable UUID enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseOutline courseOutline = (CourseOutline) o;
    return Objects.equals(this.course, courseOutline.course) &&
        Objects.equals(this.chapters, courseOutline.chapters) &&
        Objects.equals(this.progressPercent, courseOutline.progressPercent) &&
        Objects.equals(this.enrollmentId, courseOutline.enrollmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(course, chapters, progressPercent, enrollmentId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseOutline {\n");
    sb.append("    course: ").append(toIndentedString(course)).append("\n");
    sb.append("    chapters: ").append(toIndentedString(chapters)).append("\n");
    sb.append("    progressPercent: ").append(toIndentedString(progressPercent)).append("\n");
    sb.append("    enrollmentId: ").append(toIndentedString(enrollmentId)).append("\n");
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

