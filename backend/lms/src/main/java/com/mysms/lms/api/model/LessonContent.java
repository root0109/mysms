// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.Lesson;
import com.mysms.lms.api.model.LessonActivity;
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
 * LessonContent
 */

public class LessonContent {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Lesson lesson;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid LessonActivity> activities = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID previousLessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID nextLessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String progressStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean canAccess;

  public LessonContent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LessonContent(Lesson lesson, Boolean canAccess) {
    this.lesson = lesson;
    this.canAccess = canAccess;
  }

  public LessonContent lesson(Lesson lesson) {
    this.lesson = lesson;
    return this;
  }

  /**
   * Get lesson
   * @return lesson
   */
  @NotNull @Valid 
  @JsonProperty("lesson")
  public Lesson getLesson() {
    return lesson;
  }

  @JsonProperty("lesson")
  public void setLesson(Lesson lesson) {
    this.lesson = lesson;
  }

  public LessonContent activities(List<@Valid LessonActivity> activities) {
    this.activities = activities;
    return this;
  }

  public LessonContent addActivitiesItem(LessonActivity activitiesItem) {
    if (this.activities == null) {
      this.activities = new ArrayList<>();
    }
    this.activities.add(activitiesItem);
    return this;
  }

  /**
   * Embedded activities.
   * @return activities
   */
  
  @JsonProperty("activities")
  public List<@Valid LessonActivity> getActivities() {
    return activities;
  }

  @JsonProperty("activities")
  public void setActivities(List<@Valid LessonActivity> activities) {
    this.activities = activities;
  }

  public LessonContent previousLessonId(@Nullable UUID previousLessonId) {
    this.previousLessonId = previousLessonId;
    return this;
  }

  /**
   * Previous lesson.
   * @return previousLessonId
   */
  @Valid 
  @JsonProperty("previousLessonId")
  public @Nullable UUID getPreviousLessonId() {
    return previousLessonId;
  }

  @JsonProperty("previousLessonId")
  public void setPreviousLessonId(@Nullable UUID previousLessonId) {
    this.previousLessonId = previousLessonId;
  }

  public LessonContent nextLessonId(@Nullable UUID nextLessonId) {
    this.nextLessonId = nextLessonId;
    return this;
  }

  /**
   * Next lesson.
   * @return nextLessonId
   */
  @Valid 
  @JsonProperty("nextLessonId")
  public @Nullable UUID getNextLessonId() {
    return nextLessonId;
  }

  @JsonProperty("nextLessonId")
  public void setNextLessonId(@Nullable UUID nextLessonId) {
    this.nextLessonId = nextLessonId;
  }

  public LessonContent progressStatus(@Nullable String progressStatus) {
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

  public LessonContent canAccess(Boolean canAccess) {
    this.canAccess = canAccess;
    return this;
  }

  /**
   * Caller may open it.
   * @return canAccess
   */
  @NotNull 
  @JsonProperty("canAccess")
  public Boolean getCanAccess() {
    return canAccess;
  }

  @JsonProperty("canAccess")
  public void setCanAccess(Boolean canAccess) {
    this.canAccess = canAccess;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonContent lessonContent = (LessonContent) o;
    return Objects.equals(this.lesson, lessonContent.lesson) &&
        Objects.equals(this.activities, lessonContent.activities) &&
        Objects.equals(this.previousLessonId, lessonContent.previousLessonId) &&
        Objects.equals(this.nextLessonId, lessonContent.nextLessonId) &&
        Objects.equals(this.progressStatus, lessonContent.progressStatus) &&
        Objects.equals(this.canAccess, lessonContent.canAccess);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lesson, activities, previousLessonId, nextLessonId, progressStatus, canAccess);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonContent {\n");
    sb.append("    lesson: ").append(toIndentedString(lesson)).append("\n");
    sb.append("    activities: ").append(toIndentedString(activities)).append("\n");
    sb.append("    previousLessonId: ").append(toIndentedString(previousLessonId)).append("\n");
    sb.append("    nextLessonId: ").append(toIndentedString(nextLessonId)).append("\n");
    sb.append("    progressStatus: ").append(toIndentedString(progressStatus)).append("\n");
    sb.append("    canAccess: ").append(toIndentedString(canAccess)).append("\n");
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

