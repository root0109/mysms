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
 * Item placed in a chapter (folder) of a course template or of a cohort: lesson, file, assignment, quiz, discussion, participation activity, web resource, e-book, external tool, live class or link — with visibility, attempt window and grading settings.
 */

public class ContentItem {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID courseId;

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
    
    LINK("link"),
    
    EXERCISE("exercise");

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
  private ItemKindEnum itemKind;

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID copiedFromId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isOriginal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String viewCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String likeCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String ratingTotal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer ratingCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public ContentItem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ContentItem(UUID chapterId, UUID courseId, ItemKindEnum itemKind) {
    this.chapterId = chapterId;
    this.courseId = courseId;
    this.itemKind = itemKind;
  }

  public ContentItem exerciseId(@Nullable UUID exerciseId) {
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

  public ContentItem id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public ContentItem chapterId(UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter (folder) the item is placed in.
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

  public ContentItem courseId(UUID courseId) {
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

  public ContentItem cohortId(@Nullable UUID cohortId) {
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

  public ContentItem itemKind(ItemKindEnum itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Kind of item (from the fixed content-type catalogue).
   * @return itemKind
   */
  @NotNull 
  @JsonProperty("itemKind")
  public ItemKindEnum getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(ItemKindEnum itemKind) {
    this.itemKind = itemKind;
  }

  public ContentItem lessonId(@Nullable UUID lessonId) {
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

  public ContentItem fileId(@Nullable UUID fileId) {
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

  public ContentItem assignmentId(@Nullable UUID assignmentId) {
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

  public ContentItem quizId(@Nullable UUID quizId) {
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

  public ContentItem discussionPromptId(@Nullable UUID discussionPromptId) {
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

  public ContentItem participationActivityId(@Nullable UUID participationActivityId) {
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

  public ContentItem webResourceId(@Nullable UUID webResourceId) {
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

  public ContentItem ebookId(@Nullable UUID ebookId) {
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

  public ContentItem externalToolResourceId(@Nullable UUID externalToolResourceId) {
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

  public ContentItem liveClassId(@Nullable UUID liveClassId) {
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

  public ContentItem linkUrl(@Nullable String linkUrl) {
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

  public ContentItem title(@Nullable String title) {
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

  public ContentItem sortOrder(@Nullable Integer sortOrder) {
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

  public ContentItem isActive(@Nullable Boolean isActive) {
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

  public ContentItem isAlwaysVisible(@Nullable Boolean isAlwaysVisible) {
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

  public ContentItem visibleFrom(@Nullable OffsetDateTime visibleFrom) {
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

  public ContentItem visibleUntil(@Nullable OffsetDateTime visibleUntil) {
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

  public ContentItem visibleUntilCohortEnd(@Nullable Boolean visibleUntilCohortEnd) {
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

  public ContentItem attemptFrom(@Nullable OffsetDateTime attemptFrom) {
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

  public ContentItem dueAt(@Nullable OffsetDateTime dueAt) {
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

  public ContentItem attemptUntilCohortEnd(@Nullable Boolean attemptUntilCohortEnd) {
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

  public ContentItem visibleStartDay(@Nullable Integer visibleStartDay) {
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

  public ContentItem visibleStartTime(@Nullable String visibleStartTime) {
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

  public ContentItem visibleEndDay(@Nullable Integer visibleEndDay) {
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

  public ContentItem visibleEndTime(@Nullable String visibleEndTime) {
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

  public ContentItem attemptStartDay(@Nullable Integer attemptStartDay) {
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

  public ContentItem attemptStartTime(@Nullable String attemptStartTime) {
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

  public ContentItem attemptEndDay(@Nullable Integer attemptEndDay) {
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

  public ContentItem attemptEndTime(@Nullable String attemptEndTime) {
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

  public ContentItem lateSubmissionAllowed(@Nullable Boolean lateSubmissionAllowed) {
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

  public ContentItem lateSubmissionDays(@Nullable Integer lateSubmissionDays) {
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

  public ContentItem isGraded(@Nullable Boolean isGraded) {
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

  public ContentItem points(@Nullable String points) {
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

  public ContentItem weightPercent(@Nullable String weightPercent) {
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

  public ContentItem gradingCategoryId(@Nullable UUID gradingCategoryId) {
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

  public ContentItem rubricId(@Nullable UUID rubricId) {
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

  public ContentItem plagiarismCheckRequired(@Nullable Boolean plagiarismCheckRequired) {
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

  public ContentItem showInCalendar(@Nullable Boolean showInCalendar) {
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

  public ContentItem allowDelete(@Nullable Boolean allowDelete) {
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

  public ContentItem allowEditDetails(@Nullable Boolean allowEditDetails) {
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

  public ContentItem allowEditDates(@Nullable Boolean allowEditDates) {
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

  public ContentItem allowEditGrading(@Nullable Boolean allowEditGrading) {
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

  public ContentItem copiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
    return this;
  }

  /**
   * Template item this cohort copy was made from.
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

  public ContentItem isOriginal(@Nullable Boolean isOriginal) {
    this.isOriginal = isOriginal;
    return this;
  }

  /**
   * Created in this course/cohort (not copied).
   * @return isOriginal
   */
  
  @JsonProperty("isOriginal")
  public @Nullable Boolean getIsOriginal() {
    return isOriginal;
  }

  @JsonProperty("isOriginal")
  public void setIsOriginal(@Nullable Boolean isOriginal) {
    this.isOriginal = isOriginal;
  }

  public ContentItem viewCount(@Nullable String viewCount) {
    this.viewCount = viewCount;
    return this;
  }

  /**
   * Cached views.
   * @return viewCount
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("viewCount")
  public @Nullable String getViewCount() {
    return viewCount;
  }

  @JsonProperty("viewCount")
  public void setViewCount(@Nullable String viewCount) {
    this.viewCount = viewCount;
  }

  public ContentItem likeCount(@Nullable String likeCount) {
    this.likeCount = likeCount;
    return this;
  }

  /**
   * Cached likes.
   * @return likeCount
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("likeCount")
  public @Nullable String getLikeCount() {
    return likeCount;
  }

  @JsonProperty("likeCount")
  public void setLikeCount(@Nullable String likeCount) {
    this.likeCount = likeCount;
  }

  public ContentItem ratingTotal(@Nullable String ratingTotal) {
    this.ratingTotal = ratingTotal;
    return this;
  }

  /**
   * Cached sum of ratings.
   * @return ratingTotal
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("ratingTotal")
  public @Nullable String getRatingTotal() {
    return ratingTotal;
  }

  @JsonProperty("ratingTotal")
  public void setRatingTotal(@Nullable String ratingTotal) {
    this.ratingTotal = ratingTotal;
  }

  public ContentItem ratingCount(@Nullable Integer ratingCount) {
    this.ratingCount = ratingCount;
    return this;
  }

  /**
   * Cached number of ratings.
   * @return ratingCount
   */
  
  @JsonProperty("ratingCount")
  public @Nullable Integer getRatingCount() {
    return ratingCount;
  }

  @JsonProperty("ratingCount")
  public void setRatingCount(@Nullable Integer ratingCount) {
    this.ratingCount = ratingCount;
  }

  public ContentItem createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public ContentItem createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public ContentItem updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public ContentItem updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public ContentItem etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentItem contentItem = (ContentItem) o;
    return Objects.equals(this.exerciseId, contentItem.exerciseId) &&
        Objects.equals(this.id, contentItem.id) &&
        Objects.equals(this.chapterId, contentItem.chapterId) &&
        Objects.equals(this.courseId, contentItem.courseId) &&
        Objects.equals(this.cohortId, contentItem.cohortId) &&
        Objects.equals(this.itemKind, contentItem.itemKind) &&
        Objects.equals(this.lessonId, contentItem.lessonId) &&
        Objects.equals(this.fileId, contentItem.fileId) &&
        Objects.equals(this.assignmentId, contentItem.assignmentId) &&
        Objects.equals(this.quizId, contentItem.quizId) &&
        Objects.equals(this.discussionPromptId, contentItem.discussionPromptId) &&
        Objects.equals(this.participationActivityId, contentItem.participationActivityId) &&
        Objects.equals(this.webResourceId, contentItem.webResourceId) &&
        Objects.equals(this.ebookId, contentItem.ebookId) &&
        Objects.equals(this.externalToolResourceId, contentItem.externalToolResourceId) &&
        Objects.equals(this.liveClassId, contentItem.liveClassId) &&
        Objects.equals(this.linkUrl, contentItem.linkUrl) &&
        Objects.equals(this.title, contentItem.title) &&
        Objects.equals(this.sortOrder, contentItem.sortOrder) &&
        Objects.equals(this.isActive, contentItem.isActive) &&
        Objects.equals(this.isAlwaysVisible, contentItem.isAlwaysVisible) &&
        Objects.equals(this.visibleFrom, contentItem.visibleFrom) &&
        Objects.equals(this.visibleUntil, contentItem.visibleUntil) &&
        Objects.equals(this.visibleUntilCohortEnd, contentItem.visibleUntilCohortEnd) &&
        Objects.equals(this.attemptFrom, contentItem.attemptFrom) &&
        Objects.equals(this.dueAt, contentItem.dueAt) &&
        Objects.equals(this.attemptUntilCohortEnd, contentItem.attemptUntilCohortEnd) &&
        Objects.equals(this.visibleStartDay, contentItem.visibleStartDay) &&
        Objects.equals(this.visibleStartTime, contentItem.visibleStartTime) &&
        Objects.equals(this.visibleEndDay, contentItem.visibleEndDay) &&
        Objects.equals(this.visibleEndTime, contentItem.visibleEndTime) &&
        Objects.equals(this.attemptStartDay, contentItem.attemptStartDay) &&
        Objects.equals(this.attemptStartTime, contentItem.attemptStartTime) &&
        Objects.equals(this.attemptEndDay, contentItem.attemptEndDay) &&
        Objects.equals(this.attemptEndTime, contentItem.attemptEndTime) &&
        Objects.equals(this.lateSubmissionAllowed, contentItem.lateSubmissionAllowed) &&
        Objects.equals(this.lateSubmissionDays, contentItem.lateSubmissionDays) &&
        Objects.equals(this.isGraded, contentItem.isGraded) &&
        Objects.equals(this.points, contentItem.points) &&
        Objects.equals(this.weightPercent, contentItem.weightPercent) &&
        Objects.equals(this.gradingCategoryId, contentItem.gradingCategoryId) &&
        Objects.equals(this.rubricId, contentItem.rubricId) &&
        Objects.equals(this.plagiarismCheckRequired, contentItem.plagiarismCheckRequired) &&
        Objects.equals(this.showInCalendar, contentItem.showInCalendar) &&
        Objects.equals(this.allowDelete, contentItem.allowDelete) &&
        Objects.equals(this.allowEditDetails, contentItem.allowEditDetails) &&
        Objects.equals(this.allowEditDates, contentItem.allowEditDates) &&
        Objects.equals(this.allowEditGrading, contentItem.allowEditGrading) &&
        Objects.equals(this.copiedFromId, contentItem.copiedFromId) &&
        Objects.equals(this.isOriginal, contentItem.isOriginal) &&
        Objects.equals(this.viewCount, contentItem.viewCount) &&
        Objects.equals(this.likeCount, contentItem.likeCount) &&
        Objects.equals(this.ratingTotal, contentItem.ratingTotal) &&
        Objects.equals(this.ratingCount, contentItem.ratingCount) &&
        Objects.equals(this.createTime, contentItem.createTime) &&
        Objects.equals(this.createdBy, contentItem.createdBy) &&
        Objects.equals(this.updateTime, contentItem.updateTime) &&
        Objects.equals(this.updatedBy, contentItem.updatedBy) &&
        Objects.equals(this.etag, contentItem.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exerciseId, id, chapterId, courseId, cohortId, itemKind, lessonId, fileId, assignmentId, quizId, discussionPromptId, participationActivityId, webResourceId, ebookId, externalToolResourceId, liveClassId, linkUrl, title, sortOrder, isActive, isAlwaysVisible, visibleFrom, visibleUntil, visibleUntilCohortEnd, attemptFrom, dueAt, attemptUntilCohortEnd, visibleStartDay, visibleStartTime, visibleEndDay, visibleEndTime, attemptStartDay, attemptStartTime, attemptEndDay, attemptEndTime, lateSubmissionAllowed, lateSubmissionDays, isGraded, points, weightPercent, gradingCategoryId, rubricId, plagiarismCheckRequired, showInCalendar, allowDelete, allowEditDetails, allowEditDates, allowEditGrading, copiedFromId, isOriginal, viewCount, likeCount, ratingTotal, ratingCount, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentItem {\n");
    sb.append("    exerciseId: ").append(toIndentedString(exerciseId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    sb.append("    copiedFromId: ").append(toIndentedString(copiedFromId)).append("\n");
    sb.append("    isOriginal: ").append(toIndentedString(isOriginal)).append("\n");
    sb.append("    viewCount: ").append(toIndentedString(viewCount)).append("\n");
    sb.append("    likeCount: ").append(toIndentedString(likeCount)).append("\n");
    sb.append("    ratingTotal: ").append(toIndentedString(ratingTotal)).append("\n");
    sb.append("    ratingCount: ").append(toIndentedString(ratingCount)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

