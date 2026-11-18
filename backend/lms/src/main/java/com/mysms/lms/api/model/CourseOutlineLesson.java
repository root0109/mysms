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
 * CourseOutlineLesson
 */

public class CourseOutlineLesson {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean includeInPreview;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String progressStatus;

  public CourseOutlineLesson() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CourseOutlineLesson(UUID lessonId, String title, Integer sortOrder, Boolean includeInPreview) {
    this.lessonId = lessonId;
    this.title = title;
    this.sortOrder = sortOrder;
    this.includeInPreview = includeInPreview;
  }

  public CourseOutlineLesson lessonId(UUID lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson.
   * @return lessonId
   */
  @NotNull @Valid 
  @JsonProperty("lessonId")
  public UUID getLessonId() {
    return lessonId;
  }

  @JsonProperty("lessonId")
  public void setLessonId(UUID lessonId) {
    this.lessonId = lessonId;
  }

  public CourseOutlineLesson title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public CourseOutlineLesson sortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
   * @return sortOrder
   */
  @NotNull 
  @JsonProperty("sortOrder")
  public Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public CourseOutlineLesson includeInPreview(Boolean includeInPreview) {
    this.includeInPreview = includeInPreview;
    return this;
  }

  /**
   * Previewable.
   * @return includeInPreview
   */
  @NotNull 
  @JsonProperty("includeInPreview")
  public Boolean getIncludeInPreview() {
    return includeInPreview;
  }

  @JsonProperty("includeInPreview")
  public void setIncludeInPreview(Boolean includeInPreview) {
    this.includeInPreview = includeInPreview;
  }

  public CourseOutlineLesson progressStatus(@Nullable String progressStatus) {
    this.progressStatus = progressStatus;
    return this;
  }

  /**
   * Caller progress.
   * @return progressStatus
   */
  
  @JsonProperty("progressStatus")
  public @Nullable String getProgressStatus() {
    return progressStatus;
  }

  @JsonProperty("progressStatus")
  public void setProgressStatus(@Nullable String progressStatus) {
    this.progressStatus = progressStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseOutlineLesson courseOutlineLesson = (CourseOutlineLesson) o;
    return Objects.equals(this.lessonId, courseOutlineLesson.lessonId) &&
        Objects.equals(this.title, courseOutlineLesson.title) &&
        Objects.equals(this.sortOrder, courseOutlineLesson.sortOrder) &&
        Objects.equals(this.includeInPreview, courseOutlineLesson.includeInPreview) &&
        Objects.equals(this.progressStatus, courseOutlineLesson.progressStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lessonId, title, sortOrder, includeInPreview, progressStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseOutlineLesson {\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    includeInPreview: ").append(toIndentedString(includeInPreview)).append("\n");
    sb.append("    progressStatus: ").append(toIndentedString(progressStatus)).append("\n");
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

