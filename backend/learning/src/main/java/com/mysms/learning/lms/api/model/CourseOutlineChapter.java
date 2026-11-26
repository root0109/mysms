// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.CourseOutlineLesson;
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
 * CourseOutlineChapter
 */

public class CourseOutlineChapter {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean isScormPackage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CourseOutlineLesson> lessons = new ArrayList<>();

  public CourseOutlineChapter() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CourseOutlineChapter(UUID chapterId, String title, Integer sortOrder, Boolean isScormPackage) {
    this.chapterId = chapterId;
    this.title = title;
    this.sortOrder = sortOrder;
    this.isScormPackage = isScormPackage;
  }

  public CourseOutlineChapter chapterId(UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter.
   * @return chapterId
   */
  @NotNull @Valid 
  @JsonProperty("chapterId")
  public UUID getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(UUID chapterId) {
    this.chapterId = chapterId;
  }

  public CourseOutlineChapter title(String title) {
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

  public CourseOutlineChapter sortOrder(Integer sortOrder) {
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

  public CourseOutlineChapter isScormPackage(Boolean isScormPackage) {
    this.isScormPackage = isScormPackage;
    return this;
  }

  /**
   * SCORM.
   * @return isScormPackage
   */
  @NotNull 
  @JsonProperty("isScormPackage")
  public Boolean getIsScormPackage() {
    return isScormPackage;
  }

  @JsonProperty("isScormPackage")
  public void setIsScormPackage(Boolean isScormPackage) {
    this.isScormPackage = isScormPackage;
  }

  public CourseOutlineChapter lessons(List<@Valid CourseOutlineLesson> lessons) {
    this.lessons = lessons;
    return this;
  }

  public CourseOutlineChapter addLessonsItem(CourseOutlineLesson lessonsItem) {
    if (this.lessons == null) {
      this.lessons = new ArrayList<>();
    }
    this.lessons.add(lessonsItem);
    return this;
  }

  /**
   * Lessons.
   * @return lessons
   */
  
  @JsonProperty("lessons")
  public List<@Valid CourseOutlineLesson> getLessons() {
    return lessons;
  }

  @JsonProperty("lessons")
  public void setLessons(List<@Valid CourseOutlineLesson> lessons) {
    this.lessons = lessons;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseOutlineChapter courseOutlineChapter = (CourseOutlineChapter) o;
    return Objects.equals(this.chapterId, courseOutlineChapter.chapterId) &&
        Objects.equals(this.title, courseOutlineChapter.title) &&
        Objects.equals(this.sortOrder, courseOutlineChapter.sortOrder) &&
        Objects.equals(this.isScormPackage, courseOutlineChapter.isScormPackage) &&
        Objects.equals(this.lessons, courseOutlineChapter.lessons);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chapterId, title, sortOrder, isScormPackage, lessons);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseOutlineChapter {\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isScormPackage: ").append(toIndentedString(isScormPackage)).append("\n");
    sb.append("    lessons: ").append(toIndentedString(lessons)).append("\n");
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

