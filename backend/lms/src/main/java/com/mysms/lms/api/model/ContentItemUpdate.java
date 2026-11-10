// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ContentItem that can be updated (partial update; see updateMask).
 */

public class ContentItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  /**
   * Kind of item (from the fixed content-type catalogue).
   */
  public enum ItemKindEnum {
    LESSON("lesson"),
    
    FILE("file"),
    
    ASSIGNMENT("assignment"),
    
    QUIZ("quiz"),
    
    DISCUSSION("discussion"),
    
    PARTICIPATION("participation"),
    
    WEB_RESOURCE("web_resource"),
    
    EBOOK("ebook"),
    
    EXTERNAL_TOOL("external_tool"),
    
    LIVE_CLASS("live_class"),
    
    LINK("link");

    private final String value;

    ItemKindEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ItemKindEnum fromValue(String value) {
      for (ItemKindEnum b : ItemKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ItemKindEnum itemKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID discussionPromptId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID participationActivityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID webResourceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ebookId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID externalToolResourceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID liveClassId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String linkUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isAlwaysVisible;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime visibleFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime visibleUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean visibleUntilCohortEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime attemptFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean attemptUntilCohortEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer visibleStartDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String visibleStartTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer visibleEndDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String visibleEndTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer attemptStartDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String attemptStartTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer attemptEndDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String attemptEndTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean lateSubmissionAllowed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer lateSubmissionDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isGraded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String points;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String weightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradingCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID rubricId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean plagiarismCheckRequired;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showInCalendar;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowDelete;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowEditDetails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowEditDates;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowEditGrading;

  public ContentItemUpdate exerciseId(@Nullable UUID exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Programming exercise for itemKind=exercise.
   * @return exerciseId
   */
  @Valid 
  @JsonProperty("exerciseId")
  public @Nullable UUID getExerciseId() {
    return exerciseId;
  }

  @JsonProperty("exerciseId")
  public void setExerciseId(@Nullable UUID exerciseId) {
    this.exerciseId = exerciseId;
  }

  public ContentItemUpdate chapterId(@Nullable UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter (folder) the item is placed in.
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

  public ContentItemUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
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

  public ContentItemUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort (NULL = course template item).
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public ContentItemUpdate itemKind(@Nullable ItemKindEnum itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Kind of item (from the fixed content-type catalogue).
   * @return itemKind
   */
  
  @JsonProperty("itemKind")
  public @Nullable ItemKindEnum getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(@Nullable ItemKindEnum itemKind) {
    this.itemKind = itemKind;
  }

  public ContentItemUpdate lessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson (item_kind = lesson).
   * @return lessonId
   */
  @Valid 
  @JsonProperty("lessonId")
  public @Nullable UUID getLessonId() {
    return lessonId;
  }

  @JsonProperty("lessonId")
  public void setLessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
  }

  public ContentItemUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Study material file (item_kind = file).
   * @return fileId
   */
  @Valid 
  @JsonProperty("fileId")
  public @Nullable UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable UUID fileId) {
    this.fileId = fileId;
  }

  public ContentItemUpdate assignmentId(@Nullable UUID assignmentId) {
    this.assignmentId = assignmentId;
    return this;
  }

  /**
   * Assignment.
   * @return assignmentId
   */
  @Valid 
  @JsonProperty("assignmentId")
  public @Nullable UUID getAssignmentId() {
    return assignmentId;
  }

  @JsonProperty("assignmentId")
  public void setAssignmentId(@Nullable UUID assignmentId) {
    this.assignmentId = assignmentId;
  }

  public ContentItemUpdate quizId(@Nullable UUID quizId) {
    this.quizId = quizId;
    return this;
  }

  /**
   * Quiz.
   * @return quizId
   */
  @Valid 
  @JsonProperty("quizId")
  public @Nullable UUID getQuizId() {
    return quizId;
  }

  @JsonProperty("quizId")
  public void setQuizId(@Nullable UUID quizId) {
    this.quizId = quizId;
  }

  public ContentItemUpdate discussionPromptId(@Nullable UUID discussionPromptId) {
    this.discussionPromptId = discussionPromptId;
    return this;
  }

  /**
   * Discussion prompt (graded discussion).
   * @return discussionPromptId
   */
  @Valid 
  @JsonProperty("discussionPromptId")
  public @Nullable UUID getDiscussionPromptId() {
    return discussionPromptId;
  }

  @JsonProperty("discussionPromptId")
  public void setDiscussionPromptId(@Nullable UUID discussionPromptId) {
    this.discussionPromptId = discussionPromptId;
  }

  public ContentItemUpdate participationActivityId(@Nullable UUID participationActivityId) {
    this.participationActivityId = participationActivityId;
    return this;
  }

  /**
   * Participation activity.
   * @return participationActivityId
   */
  @Valid 
  @JsonProperty("participationActivityId")
  public @Nullable UUID getParticipationActivityId() {
    return participationActivityId;
  }

  @JsonProperty("participationActivityId")
  public void setParticipationActivityId(@Nullable UUID participationActivityId) {
    this.participationActivityId = participationActivityId;
  }

  public ContentItemUpdate webResourceId(@Nullable UUID webResourceId) {
    this.webResourceId = webResourceId;
    return this;
  }

  /**
   * Web resource.
   * @return webResourceId
   */
  @Valid 
  @JsonProperty("webResourceId")
  public @Nullable UUID getWebResourceId() {
    return webResourceId;
  }

  @JsonProperty("webResourceId")
  public void setWebResourceId(@Nullable UUID webResourceId) {
    this.webResourceId = webResourceId;
  }

  public ContentItemUpdate ebookId(@Nullable UUID ebookId) {
    this.ebookId = ebookId;
    return this;
  }

  /**
   * E-book.
   * @return ebookId
   */
  @Valid 
  @JsonProperty("ebookId")
  public @Nullable UUID getEbookId() {
    return ebookId;
  }

  @JsonProperty("ebookId")
  public void setEbookId(@Nullable UUID ebookId) {
    this.ebookId = ebookId;
  }

  public ContentItemUpdate externalToolResourceId(@Nullable UUID externalToolResourceId) {
    this.externalToolResourceId = externalToolResourceId;
    return this;
  }

  /**
   * External tool resource.
   * @return externalToolResourceId
   */
  @Valid 
  @JsonProperty("externalToolResourceId")
  public @Nullable UUID getExternalToolResourceId() {
    return externalToolResourceId;
  }

  @JsonProperty("externalToolResourceId")
  public void setExternalToolResourceId(@Nullable UUID externalToolResourceId) {
    this.externalToolResourceId = externalToolResourceId;
  }

  public ContentItemUpdate liveClassId(@Nullable UUID liveClassId) {
    this.liveClassId = liveClassId;
    return this;
  }

  /**
   * Live class.
   * @return liveClassId
   */
  @Valid 
  @JsonProperty("liveClassId")
  public @Nullable UUID getLiveClassId() {
    return liveClassId;
  }

  @JsonProperty("liveClassId")
  public void setLiveClassId(@Nullable UUID liveClassId) {
    this.liveClassId = liveClassId;
  }

  public ContentItemUpdate linkUrl(@Nullable String linkUrl) {
    this.linkUrl = linkUrl;
    return this;
  }

  /**
   * URL (item_kind = link).
   * @return linkUrl
   */
  @Size(max = 1000) 
  @JsonProperty("linkUrl")
  public @Nullable String getLinkUrl() {
    return linkUrl;
  }

  @JsonProperty("linkUrl")
  public void setLinkUrl(@Nullable String linkUrl) {
    this.linkUrl = linkUrl;
  }

  public ContentItemUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title override shown in the outline.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public ContentItemUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position in the chapter.
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

  public ContentItemUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Item active (cohort copies can be deactivated).
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public ContentItemUpdate isAlwaysVisible(@Nullable Boolean isAlwaysVisible) {
    this.isAlwaysVisible = isAlwaysVisible;
    return this;
  }

  /**
   * Visible without a date window.
   * @return isAlwaysVisible
   */
  
  @JsonProperty("isAlwaysVisible")
  public @Nullable Boolean getIsAlwaysVisible() {
    return isAlwaysVisible;
  }

  @JsonProperty("isAlwaysVisible")
  public void setIsAlwaysVisible(@Nullable Boolean isAlwaysVisible) {
    this.isAlwaysVisible = isAlwaysVisible;
  }

  public ContentItemUpdate visibleFrom(@Nullable OffsetDateTime visibleFrom) {
    this.visibleFrom = visibleFrom;
    return this;
  }

  /**
   * Cohort: visible from.
   * @return visibleFrom
   */
  @Valid 
  @JsonProperty("visibleFrom")
  public @Nullable OffsetDateTime getVisibleFrom() {
    return visibleFrom;
  }

  @JsonProperty("visibleFrom")
  public void setVisibleFrom(@Nullable OffsetDateTime visibleFrom) {
    this.visibleFrom = visibleFrom;
  }

  public ContentItemUpdate visibleUntil(@Nullable OffsetDateTime visibleUntil) {
    this.visibleUntil = visibleUntil;
    return this;
  }

  /**
   * Cohort: visible until.
   * @return visibleUntil
   */
  @Valid 
  @JsonProperty("visibleUntil")
  public @Nullable OffsetDateTime getVisibleUntil() {
    return visibleUntil;
  }

  @JsonProperty("visibleUntil")
  public void setVisibleUntil(@Nullable OffsetDateTime visibleUntil) {
    this.visibleUntil = visibleUntil;
  }

  public ContentItemUpdate visibleUntilCohortEnd(@Nullable Boolean visibleUntilCohortEnd) {
    this.visibleUntilCohortEnd = visibleUntilCohortEnd;
    return this;
  }

  /**
   * Visible until the cohort ends.
   * @return visibleUntilCohortEnd
   */
  
  @JsonProperty("visibleUntilCohortEnd")
  public @Nullable Boolean getVisibleUntilCohortEnd() {
    return visibleUntilCohortEnd;
  }

  @JsonProperty("visibleUntilCohortEnd")
  public void setVisibleUntilCohortEnd(@Nullable Boolean visibleUntilCohortEnd) {
    this.visibleUntilCohortEnd = visibleUntilCohortEnd;
  }

  public ContentItemUpdate attemptFrom(@Nullable OffsetDateTime attemptFrom) {
    this.attemptFrom = attemptFrom;
    return this;
  }

  /**
   * Cohort: attempts / submissions open.
   * @return attemptFrom
   */
  @Valid 
  @JsonProperty("attemptFrom")
  public @Nullable OffsetDateTime getAttemptFrom() {
    return attemptFrom;
  }

  @JsonProperty("attemptFrom")
  public void setAttemptFrom(@Nullable OffsetDateTime attemptFrom) {
    this.attemptFrom = attemptFrom;
  }

  public ContentItemUpdate dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Cohort: due date.
   * @return dueAt
   */
  @Valid 
  @JsonProperty("dueAt")
  public @Nullable OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public ContentItemUpdate attemptUntilCohortEnd(@Nullable Boolean attemptUntilCohortEnd) {
    this.attemptUntilCohortEnd = attemptUntilCohortEnd;
    return this;
  }

  /**
   * Attempts allowed until the cohort ends.
   * @return attemptUntilCohortEnd
   */
  
  @JsonProperty("attemptUntilCohortEnd")
  public @Nullable Boolean getAttemptUntilCohortEnd() {
    return attemptUntilCohortEnd;
  }

  @JsonProperty("attemptUntilCohortEnd")
  public void setAttemptUntilCohortEnd(@Nullable Boolean attemptUntilCohortEnd) {
    this.attemptUntilCohortEnd = attemptUntilCohortEnd;
  }

  public ContentItemUpdate visibleStartDay(@Nullable Integer visibleStartDay) {
    this.visibleStartDay = visibleStartDay;
    return this;
  }

  /**
   * Template: visible from day N of the cohort.
   * @return visibleStartDay
   */
  
  @JsonProperty("visibleStartDay")
  public @Nullable Integer getVisibleStartDay() {
    return visibleStartDay;
  }

  @JsonProperty("visibleStartDay")
  public void setVisibleStartDay(@Nullable Integer visibleStartDay) {
    this.visibleStartDay = visibleStartDay;
  }

  public ContentItemUpdate visibleStartTime(@Nullable String visibleStartTime) {
    this.visibleStartTime = visibleStartTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return visibleStartTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("visibleStartTime")
  public @Nullable String getVisibleStartTime() {
    return visibleStartTime;
  }

  @JsonProperty("visibleStartTime")
  public void setVisibleStartTime(@Nullable String visibleStartTime) {
    this.visibleStartTime = visibleStartTime;
  }

  public ContentItemUpdate visibleEndDay(@Nullable Integer visibleEndDay) {
    this.visibleEndDay = visibleEndDay;
    return this;
  }

  /**
   * Template: visible until day N.
   * @return visibleEndDay
   */
  
  @JsonProperty("visibleEndDay")
  public @Nullable Integer getVisibleEndDay() {
    return visibleEndDay;
  }

  @JsonProperty("visibleEndDay")
  public void setVisibleEndDay(@Nullable Integer visibleEndDay) {
    this.visibleEndDay = visibleEndDay;
  }

  public ContentItemUpdate visibleEndTime(@Nullable String visibleEndTime) {
    this.visibleEndTime = visibleEndTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return visibleEndTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("visibleEndTime")
  public @Nullable String getVisibleEndTime() {
    return visibleEndTime;
  }

  @JsonProperty("visibleEndTime")
  public void setVisibleEndTime(@Nullable String visibleEndTime) {
    this.visibleEndTime = visibleEndTime;
  }

  public ContentItemUpdate attemptStartDay(@Nullable Integer attemptStartDay) {
    this.attemptStartDay = attemptStartDay;
    return this;
  }

  /**
   * Template: attempts open on day N.
   * @return attemptStartDay
   */
  
  @JsonProperty("attemptStartDay")
  public @Nullable Integer getAttemptStartDay() {
    return attemptStartDay;
  }

  @JsonProperty("attemptStartDay")
  public void setAttemptStartDay(@Nullable Integer attemptStartDay) {
    this.attemptStartDay = attemptStartDay;
  }

  public ContentItemUpdate attemptStartTime(@Nullable String attemptStartTime) {
    this.attemptStartTime = attemptStartTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return attemptStartTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("attemptStartTime")
  public @Nullable String getAttemptStartTime() {
    return attemptStartTime;
  }

  @JsonProperty("attemptStartTime")
  public void setAttemptStartTime(@Nullable String attemptStartTime) {
    this.attemptStartTime = attemptStartTime;
  }

  public ContentItemUpdate attemptEndDay(@Nullable Integer attemptEndDay) {
    this.attemptEndDay = attemptEndDay;
    return this;
  }

  /**
   * Template: due on day N.
   * @return attemptEndDay
   */
  
  @JsonProperty("attemptEndDay")
  public @Nullable Integer getAttemptEndDay() {
    return attemptEndDay;
  }

  @JsonProperty("attemptEndDay")
  public void setAttemptEndDay(@Nullable Integer attemptEndDay) {
    this.attemptEndDay = attemptEndDay;
  }

  public ContentItemUpdate attemptEndTime(@Nullable String attemptEndTime) {
    this.attemptEndTime = attemptEndTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return attemptEndTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("attemptEndTime")
  public @Nullable String getAttemptEndTime() {
    return attemptEndTime;
  }

  @JsonProperty("attemptEndTime")
  public void setAttemptEndTime(@Nullable String attemptEndTime) {
    this.attemptEndTime = attemptEndTime;
  }

  public ContentItemUpdate lateSubmissionAllowed(@Nullable Boolean lateSubmissionAllowed) {
    this.lateSubmissionAllowed = lateSubmissionAllowed;
    return this;
  }

  /**
   * Late submissions accepted.
   * @return lateSubmissionAllowed
   */
  
  @JsonProperty("lateSubmissionAllowed")
  public @Nullable Boolean getLateSubmissionAllowed() {
    return lateSubmissionAllowed;
  }

  @JsonProperty("lateSubmissionAllowed")
  public void setLateSubmissionAllowed(@Nullable Boolean lateSubmissionAllowed) {
    this.lateSubmissionAllowed = lateSubmissionAllowed;
  }

  public ContentItemUpdate lateSubmissionDays(@Nullable Integer lateSubmissionDays) {
    this.lateSubmissionDays = lateSubmissionDays;
    return this;
  }

  /**
   * Days late submissions are accepted.
   * @return lateSubmissionDays
   */
  
  @JsonProperty("lateSubmissionDays")
  public @Nullable Integer getLateSubmissionDays() {
    return lateSubmissionDays;
  }

  @JsonProperty("lateSubmissionDays")
  public void setLateSubmissionDays(@Nullable Integer lateSubmissionDays) {
    this.lateSubmissionDays = lateSubmissionDays;
  }

  public ContentItemUpdate isGraded(@Nullable Boolean isGraded) {
    this.isGraded = isGraded;
    return this;
  }

  /**
   * Counts in the gradebook.
   * @return isGraded
   */
  
  @JsonProperty("isGraded")
  public @Nullable Boolean getIsGraded() {
    return isGraded;
  }

  @JsonProperty("isGraded")
  public void setIsGraded(@Nullable Boolean isGraded) {
    this.isGraded = isGraded;
  }

  public ContentItemUpdate points(@Nullable String points) {
    this.points = points;
    return this;
  }

  /**
   * Maximum points.
   * @return points
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("points")
  public @Nullable String getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(@Nullable String points) {
    this.points = points;
  }

  public ContentItemUpdate weightPercent(@Nullable String weightPercent) {
    this.weightPercent = weightPercent;
    return this;
  }

  /**
   * Weight inside its grading category.
   * @return weightPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("weightPercent")
  public @Nullable String getWeightPercent() {
    return weightPercent;
  }

  @JsonProperty("weightPercent")
  public void setWeightPercent(@Nullable String weightPercent) {
    this.weightPercent = weightPercent;
  }

  public ContentItemUpdate gradingCategoryId(@Nullable UUID gradingCategoryId) {
    this.gradingCategoryId = gradingCategoryId;
    return this;
  }

  /**
   * Gradebook category.
   * @return gradingCategoryId
   */
  @Valid 
  @JsonProperty("gradingCategoryId")
  public @Nullable UUID getGradingCategoryId() {
    return gradingCategoryId;
  }

  @JsonProperty("gradingCategoryId")
  public void setGradingCategoryId(@Nullable UUID gradingCategoryId) {
    this.gradingCategoryId = gradingCategoryId;
  }

  public ContentItemUpdate rubricId(@Nullable UUID rubricId) {
    this.rubricId = rubricId;
    return this;
  }

  /**
   * Rubric used for grading.
   * @return rubricId
   */
  @Valid 
  @JsonProperty("rubricId")
  public @Nullable UUID getRubricId() {
    return rubricId;
  }

  @JsonProperty("rubricId")
  public void setRubricId(@Nullable UUID rubricId) {
    this.rubricId = rubricId;
  }

  public ContentItemUpdate plagiarismCheckRequired(@Nullable Boolean plagiarismCheckRequired) {
    this.plagiarismCheckRequired = plagiarismCheckRequired;
    return this;
  }

  /**
   * Submissions are sent for plagiarism checking.
   * @return plagiarismCheckRequired
   */
  
  @JsonProperty("plagiarismCheckRequired")
  public @Nullable Boolean getPlagiarismCheckRequired() {
    return plagiarismCheckRequired;
  }

  @JsonProperty("plagiarismCheckRequired")
  public void setPlagiarismCheckRequired(@Nullable Boolean plagiarismCheckRequired) {
    this.plagiarismCheckRequired = plagiarismCheckRequired;
  }

  public ContentItemUpdate showInCalendar(@Nullable Boolean showInCalendar) {
    this.showInCalendar = showInCalendar;
    return this;
  }

  /**
   * Dates appear in students' calendars.
   * @return showInCalendar
   */
  
  @JsonProperty("showInCalendar")
  public @Nullable Boolean getShowInCalendar() {
    return showInCalendar;
  }

  @JsonProperty("showInCalendar")
  public void setShowInCalendar(@Nullable Boolean showInCalendar) {
    this.showInCalendar = showInCalendar;
  }

  public ContentItemUpdate allowDelete(@Nullable Boolean allowDelete) {
    this.allowDelete = allowDelete;
    return this;
  }

  /**
   * Cohort teachers may remove the item.
   * @return allowDelete
   */
  
  @JsonProperty("allowDelete")
  public @Nullable Boolean getAllowDelete() {
    return allowDelete;
  }

  @JsonProperty("allowDelete")
  public void setAllowDelete(@Nullable Boolean allowDelete) {
    this.allowDelete = allowDelete;
  }

  public ContentItemUpdate allowEditDetails(@Nullable Boolean allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
    return this;
  }

  /**
   * Cohort teachers may edit details.
   * @return allowEditDetails
   */
  
  @JsonProperty("allowEditDetails")
  public @Nullable Boolean getAllowEditDetails() {
    return allowEditDetails;
  }

  @JsonProperty("allowEditDetails")
  public void setAllowEditDetails(@Nullable Boolean allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
  }

  public ContentItemUpdate allowEditDates(@Nullable Boolean allowEditDates) {
    this.allowEditDates = allowEditDates;
    return this;
  }

  /**
   * Cohort teachers may change dates.
   * @return allowEditDates
   */
  
  @JsonProperty("allowEditDates")
  public @Nullable Boolean getAllowEditDates() {
    return allowEditDates;
  }

  @JsonProperty("allowEditDates")
  public void setAllowEditDates(@Nullable Boolean allowEditDates) {
    this.allowEditDates = allowEditDates;
  }

  public ContentItemUpdate allowEditGrading(@Nullable Boolean allowEditGrading) {
    this.allowEditGrading = allowEditGrading;
    return this;
  }

  /**
   * Cohort teachers may change grading settings.
   * @return allowEditGrading
   */
  
  @JsonProperty("allowEditGrading")
  public @Nullable Boolean getAllowEditGrading() {
    return allowEditGrading;
  }

  @JsonProperty("allowEditGrading")
  public void setAllowEditGrading(@Nullable Boolean allowEditGrading) {
    this.allowEditGrading = allowEditGrading;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentItemUpdate contentItemUpdate = (ContentItemUpdate) o;
    return Objects.equals(this.exerciseId, contentItemUpdate.exerciseId) &&
        Objects.equals(this.chapterId, contentItemUpdate.chapterId) &&
        Objects.equals(this.courseId, contentItemUpdate.courseId) &&
        Objects.equals(this.cohortId, contentItemUpdate.cohortId) &&
        Objects.equals(this.itemKind, contentItemUpdate.itemKind) &&
        Objects.equals(this.lessonId, contentItemUpdate.lessonId) &&
        Objects.equals(this.fileId, contentItemUpdate.fileId) &&
        Objects.equals(this.assignmentId, contentItemUpdate.assignmentId) &&
        Objects.equals(this.quizId, contentItemUpdate.quizId) &&
        Objects.equals(this.discussionPromptId, contentItemUpdate.discussionPromptId) &&
        Objects.equals(this.participationActivityId, contentItemUpdate.participationActivityId) &&
        Objects.equals(this.webResourceId, contentItemUpdate.webResourceId) &&
        Objects.equals(this.ebookId, contentItemUpdate.ebookId) &&
        Objects.equals(this.externalToolResourceId, contentItemUpdate.externalToolResourceId) &&
        Objects.equals(this.liveClassId, contentItemUpdate.liveClassId) &&
        Objects.equals(this.linkUrl, contentItemUpdate.linkUrl) &&
        Objects.equals(this.title, contentItemUpdate.title) &&
        Objects.equals(this.sortOrder, contentItemUpdate.sortOrder) &&
        Objects.equals(this.isActive, contentItemUpdate.isActive) &&
        Objects.equals(this.isAlwaysVisible, contentItemUpdate.isAlwaysVisible) &&
        Objects.equals(this.visibleFrom, contentItemUpdate.visibleFrom) &&
        Objects.equals(this.visibleUntil, contentItemUpdate.visibleUntil) &&
        Objects.equals(this.visibleUntilCohortEnd, contentItemUpdate.visibleUntilCohortEnd) &&
        Objects.equals(this.attemptFrom, contentItemUpdate.attemptFrom) &&
        Objects.equals(this.dueAt, contentItemUpdate.dueAt) &&
        Objects.equals(this.attemptUntilCohortEnd, contentItemUpdate.attemptUntilCohortEnd) &&
        Objects.equals(this.visibleStartDay, contentItemUpdate.visibleStartDay) &&
        Objects.equals(this.visibleStartTime, contentItemUpdate.visibleStartTime) &&
        Objects.equals(this.visibleEndDay, contentItemUpdate.visibleEndDay) &&
        Objects.equals(this.visibleEndTime, contentItemUpdate.visibleEndTime) &&
        Objects.equals(this.attemptStartDay, contentItemUpdate.attemptStartDay) &&
        Objects.equals(this.attemptStartTime, contentItemUpdate.attemptStartTime) &&
        Objects.equals(this.attemptEndDay, contentItemUpdate.attemptEndDay) &&
        Objects.equals(this.attemptEndTime, contentItemUpdate.attemptEndTime) &&
        Objects.equals(this.lateSubmissionAllowed, contentItemUpdate.lateSubmissionAllowed) &&
        Objects.equals(this.lateSubmissionDays, contentItemUpdate.lateSubmissionDays) &&
        Objects.equals(this.isGraded, contentItemUpdate.isGraded) &&
        Objects.equals(this.points, contentItemUpdate.points) &&
        Objects.equals(this.weightPercent, contentItemUpdate.weightPercent) &&
        Objects.equals(this.gradingCategoryId, contentItemUpdate.gradingCategoryId) &&
        Objects.equals(this.rubricId, contentItemUpdate.rubricId) &&
        Objects.equals(this.plagiarismCheckRequired, contentItemUpdate.plagiarismCheckRequired) &&
        Objects.equals(this.showInCalendar, contentItemUpdate.showInCalendar) &&
        Objects.equals(this.allowDelete, contentItemUpdate.allowDelete) &&
        Objects.equals(this.allowEditDetails, contentItemUpdate.allowEditDetails) &&
        Objects.equals(this.allowEditDates, contentItemUpdate.allowEditDates) &&
        Objects.equals(this.allowEditGrading, contentItemUpdate.allowEditGrading);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exerciseId, chapterId, courseId, cohortId, itemKind, lessonId, fileId, assignmentId, quizId, discussionPromptId, participationActivityId, webResourceId, ebookId, externalToolResourceId, liveClassId, linkUrl, title, sortOrder, isActive, isAlwaysVisible, visibleFrom, visibleUntil, visibleUntilCohortEnd, attemptFrom, dueAt, attemptUntilCohortEnd, visibleStartDay, visibleStartTime, visibleEndDay, visibleEndTime, attemptStartDay, attemptStartTime, attemptEndDay, attemptEndTime, lateSubmissionAllowed, lateSubmissionDays, isGraded, points, weightPercent, gradingCategoryId, rubricId, plagiarismCheckRequired, showInCalendar, allowDelete, allowEditDetails, allowEditDates, allowEditGrading);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentItemUpdate {\n");
    sb.append("    exerciseId: ").append(toIndentedString(exerciseId)).append("\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    itemKind: ").append(toIndentedString(itemKind)).append("\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    quizId: ").append(toIndentedString(quizId)).append("\n");
    sb.append("    discussionPromptId: ").append(toIndentedString(discussionPromptId)).append("\n");
    sb.append("    participationActivityId: ").append(toIndentedString(participationActivityId)).append("\n");
    sb.append("    webResourceId: ").append(toIndentedString(webResourceId)).append("\n");
    sb.append("    ebookId: ").append(toIndentedString(ebookId)).append("\n");
    sb.append("    externalToolResourceId: ").append(toIndentedString(externalToolResourceId)).append("\n");
    sb.append("    liveClassId: ").append(toIndentedString(liveClassId)).append("\n");
    sb.append("    linkUrl: ").append(toIndentedString(linkUrl)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    isAlwaysVisible: ").append(toIndentedString(isAlwaysVisible)).append("\n");
    sb.append("    visibleFrom: ").append(toIndentedString(visibleFrom)).append("\n");
    sb.append("    visibleUntil: ").append(toIndentedString(visibleUntil)).append("\n");
    sb.append("    visibleUntilCohortEnd: ").append(toIndentedString(visibleUntilCohortEnd)).append("\n");
    sb.append("    attemptFrom: ").append(toIndentedString(attemptFrom)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    attemptUntilCohortEnd: ").append(toIndentedString(attemptUntilCohortEnd)).append("\n");
    sb.append("    visibleStartDay: ").append(toIndentedString(visibleStartDay)).append("\n");
    sb.append("    visibleStartTime: ").append(toIndentedString(visibleStartTime)).append("\n");
    sb.append("    visibleEndDay: ").append(toIndentedString(visibleEndDay)).append("\n");
    sb.append("    visibleEndTime: ").append(toIndentedString(visibleEndTime)).append("\n");
    sb.append("    attemptStartDay: ").append(toIndentedString(attemptStartDay)).append("\n");
    sb.append("    attemptStartTime: ").append(toIndentedString(attemptStartTime)).append("\n");
    sb.append("    attemptEndDay: ").append(toIndentedString(attemptEndDay)).append("\n");
    sb.append("    attemptEndTime: ").append(toIndentedString(attemptEndTime)).append("\n");
    sb.append("    lateSubmissionAllowed: ").append(toIndentedString(lateSubmissionAllowed)).append("\n");
    sb.append("    lateSubmissionDays: ").append(toIndentedString(lateSubmissionDays)).append("\n");
    sb.append("    isGraded: ").append(toIndentedString(isGraded)).append("\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
    sb.append("    weightPercent: ").append(toIndentedString(weightPercent)).append("\n");
    sb.append("    gradingCategoryId: ").append(toIndentedString(gradingCategoryId)).append("\n");
    sb.append("    rubricId: ").append(toIndentedString(rubricId)).append("\n");
    sb.append("    plagiarismCheckRequired: ").append(toIndentedString(plagiarismCheckRequired)).append("\n");
    sb.append("    showInCalendar: ").append(toIndentedString(showInCalendar)).append("\n");
    sb.append("    allowDelete: ").append(toIndentedString(allowDelete)).append("\n");
    sb.append("    allowEditDetails: ").append(toIndentedString(allowEditDetails)).append("\n");
    sb.append("    allowEditDates: ").append(toIndentedString(allowEditDates)).append("\n");
    sb.append("    allowEditGrading: ").append(toIndentedString(allowEditGrading)).append("\n");
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

