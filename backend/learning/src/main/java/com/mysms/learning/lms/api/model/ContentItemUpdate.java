// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ContentItem that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ContentItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> cohortId;

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
  private @Nullable Optional<@NotNull ItemKindEnum> itemKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> discussionPromptId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> participationActivityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> webResourceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> ebookId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> externalToolResourceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> liveClassId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 1000) String> linkUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isAlwaysVisible;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> visibleFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> visibleUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> visibleUntilCohortEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> attemptFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> attemptUntilCohortEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> visibleStartDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> visibleStartTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> visibleEndDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> visibleEndTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> attemptStartDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> attemptStartTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> attemptEndDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> attemptEndTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> lateSubmissionAllowed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> lateSubmissionDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isGraded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> points;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> weightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> gradingCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> rubricId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> plagiarismCheckRequired;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showInCalendar;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowDelete;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowEditDetails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowEditDates;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowEditGrading;

  public ContentItemUpdate exerciseId(@Nullable Optional<UUID> exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Programming exercise for itemKind=exercise.
   * @return exerciseId
   */
  
  @JsonProperty("exerciseId")
  public @Nullable Optional<UUID> getExerciseId() {
    return exerciseId;
  }

  @JsonProperty("exerciseId")
  public void setExerciseId(@Nullable Optional<UUID> exerciseId) {
    this.exerciseId = exerciseId;
  }

  public ContentItemUpdate chapterId(@Nullable Optional<UUID> chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter (folder) the item is placed in.
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

  public ContentItemUpdate courseId(@Nullable Optional<UUID> courseId) {
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

  public ContentItemUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort (NULL = course template item).
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public ContentItemUpdate itemKind(@Nullable Optional<ItemKindEnum> itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Kind of item (from the fixed content-type catalogue).
   * @return itemKind
   */
  
  @JsonProperty("itemKind")
  public @Nullable Optional<ItemKindEnum> getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(@Nullable Optional<ItemKindEnum> itemKind) {
    this.itemKind = itemKind;
  }

  public ContentItemUpdate lessonId(@Nullable Optional<UUID> lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson (item_kind = lesson).
   * @return lessonId
   */
  
  @JsonProperty("lessonId")
  public @Nullable Optional<UUID> getLessonId() {
    return lessonId;
  }

  @JsonProperty("lessonId")
  public void setLessonId(@Nullable Optional<UUID> lessonId) {
    this.lessonId = lessonId;
  }

  public ContentItemUpdate fileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Study material file (item_kind = file).
   * @return fileId
   */
  
  @JsonProperty("fileId")
  public @Nullable Optional<UUID> getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
  }

  public ContentItemUpdate assignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
    return this;
  }

  /**
   * Assignment.
   * @return assignmentId
   */
  
  @JsonProperty("assignmentId")
  public @Nullable Optional<UUID> getAssignmentId() {
    return assignmentId;
  }

  @JsonProperty("assignmentId")
  public void setAssignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
  }

  public ContentItemUpdate quizId(@Nullable Optional<UUID> quizId) {
    this.quizId = quizId;
    return this;
  }

  /**
   * Quiz.
   * @return quizId
   */
  
  @JsonProperty("quizId")
  public @Nullable Optional<UUID> getQuizId() {
    return quizId;
  }

  @JsonProperty("quizId")
  public void setQuizId(@Nullable Optional<UUID> quizId) {
    this.quizId = quizId;
  }

  public ContentItemUpdate discussionPromptId(@Nullable Optional<UUID> discussionPromptId) {
    this.discussionPromptId = discussionPromptId;
    return this;
  }

  /**
   * Discussion prompt (graded discussion).
   * @return discussionPromptId
   */
  
  @JsonProperty("discussionPromptId")
  public @Nullable Optional<UUID> getDiscussionPromptId() {
    return discussionPromptId;
  }

  @JsonProperty("discussionPromptId")
  public void setDiscussionPromptId(@Nullable Optional<UUID> discussionPromptId) {
    this.discussionPromptId = discussionPromptId;
  }

  public ContentItemUpdate participationActivityId(@Nullable Optional<UUID> participationActivityId) {
    this.participationActivityId = participationActivityId;
    return this;
  }

  /**
   * Participation activity.
   * @return participationActivityId
   */
  
  @JsonProperty("participationActivityId")
  public @Nullable Optional<UUID> getParticipationActivityId() {
    return participationActivityId;
  }

  @JsonProperty("participationActivityId")
  public void setParticipationActivityId(@Nullable Optional<UUID> participationActivityId) {
    this.participationActivityId = participationActivityId;
  }

  public ContentItemUpdate webResourceId(@Nullable Optional<UUID> webResourceId) {
    this.webResourceId = webResourceId;
    return this;
  }

  /**
   * Web resource.
   * @return webResourceId
   */
  
  @JsonProperty("webResourceId")
  public @Nullable Optional<UUID> getWebResourceId() {
    return webResourceId;
  }

  @JsonProperty("webResourceId")
  public void setWebResourceId(@Nullable Optional<UUID> webResourceId) {
    this.webResourceId = webResourceId;
  }

  public ContentItemUpdate ebookId(@Nullable Optional<UUID> ebookId) {
    this.ebookId = ebookId;
    return this;
  }

  /**
   * E-book.
   * @return ebookId
   */
  
  @JsonProperty("ebookId")
  public @Nullable Optional<UUID> getEbookId() {
    return ebookId;
  }

  @JsonProperty("ebookId")
  public void setEbookId(@Nullable Optional<UUID> ebookId) {
    this.ebookId = ebookId;
  }

  public ContentItemUpdate externalToolResourceId(@Nullable Optional<UUID> externalToolResourceId) {
    this.externalToolResourceId = externalToolResourceId;
    return this;
  }

  /**
   * External tool resource.
   * @return externalToolResourceId
   */
  
  @JsonProperty("externalToolResourceId")
  public @Nullable Optional<UUID> getExternalToolResourceId() {
    return externalToolResourceId;
  }

  @JsonProperty("externalToolResourceId")
  public void setExternalToolResourceId(@Nullable Optional<UUID> externalToolResourceId) {
    this.externalToolResourceId = externalToolResourceId;
  }

  public ContentItemUpdate liveClassId(@Nullable Optional<UUID> liveClassId) {
    this.liveClassId = liveClassId;
    return this;
  }

  /**
   * Live class.
   * @return liveClassId
   */
  
  @JsonProperty("liveClassId")
  public @Nullable Optional<UUID> getLiveClassId() {
    return liveClassId;
  }

  @JsonProperty("liveClassId")
  public void setLiveClassId(@Nullable Optional<UUID> liveClassId) {
    this.liveClassId = liveClassId;
  }

  public ContentItemUpdate linkUrl(@Nullable Optional<String> linkUrl) {
    this.linkUrl = linkUrl;
    return this;
  }

  /**
   * URL (item_kind = link).
   * @return linkUrl
   */
  
  @JsonProperty("linkUrl")
  public @Nullable Optional<String> getLinkUrl() {
    return linkUrl;
  }

  @JsonProperty("linkUrl")
  public void setLinkUrl(@Nullable Optional<String> linkUrl) {
    this.linkUrl = linkUrl;
  }

  public ContentItemUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title override shown in the outline.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public ContentItemUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position in the chapter.
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

  public ContentItemUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Item active (cohort copies can be deactivated).
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public ContentItemUpdate isAlwaysVisible(@Nullable Optional<Boolean> isAlwaysVisible) {
    this.isAlwaysVisible = isAlwaysVisible;
    return this;
  }

  /**
   * Visible without a date window.
   * @return isAlwaysVisible
   */
  
  @JsonProperty("isAlwaysVisible")
  public @Nullable Optional<Boolean> getIsAlwaysVisible() {
    return isAlwaysVisible;
  }

  @JsonProperty("isAlwaysVisible")
  public void setIsAlwaysVisible(@Nullable Optional<Boolean> isAlwaysVisible) {
    this.isAlwaysVisible = isAlwaysVisible;
  }

  public ContentItemUpdate visibleFrom(@Nullable Optional<OffsetDateTime> visibleFrom) {
    this.visibleFrom = visibleFrom;
    return this;
  }

  /**
   * Cohort: visible from.
   * @return visibleFrom
   */
  
  @JsonProperty("visibleFrom")
  public @Nullable Optional<OffsetDateTime> getVisibleFrom() {
    return visibleFrom;
  }

  @JsonProperty("visibleFrom")
  public void setVisibleFrom(@Nullable Optional<OffsetDateTime> visibleFrom) {
    this.visibleFrom = visibleFrom;
  }

  public ContentItemUpdate visibleUntil(@Nullable Optional<OffsetDateTime> visibleUntil) {
    this.visibleUntil = visibleUntil;
    return this;
  }

  /**
   * Cohort: visible until.
   * @return visibleUntil
   */
  
  @JsonProperty("visibleUntil")
  public @Nullable Optional<OffsetDateTime> getVisibleUntil() {
    return visibleUntil;
  }

  @JsonProperty("visibleUntil")
  public void setVisibleUntil(@Nullable Optional<OffsetDateTime> visibleUntil) {
    this.visibleUntil = visibleUntil;
  }

  public ContentItemUpdate visibleUntilCohortEnd(@Nullable Optional<Boolean> visibleUntilCohortEnd) {
    this.visibleUntilCohortEnd = visibleUntilCohortEnd;
    return this;
  }

  /**
   * Visible until the cohort ends.
   * @return visibleUntilCohortEnd
   */
  
  @JsonProperty("visibleUntilCohortEnd")
  public @Nullable Optional<Boolean> getVisibleUntilCohortEnd() {
    return visibleUntilCohortEnd;
  }

  @JsonProperty("visibleUntilCohortEnd")
  public void setVisibleUntilCohortEnd(@Nullable Optional<Boolean> visibleUntilCohortEnd) {
    this.visibleUntilCohortEnd = visibleUntilCohortEnd;
  }

  public ContentItemUpdate attemptFrom(@Nullable Optional<OffsetDateTime> attemptFrom) {
    this.attemptFrom = attemptFrom;
    return this;
  }

  /**
   * Cohort: attempts / submissions open.
   * @return attemptFrom
   */
  
  @JsonProperty("attemptFrom")
  public @Nullable Optional<OffsetDateTime> getAttemptFrom() {
    return attemptFrom;
  }

  @JsonProperty("attemptFrom")
  public void setAttemptFrom(@Nullable Optional<OffsetDateTime> attemptFrom) {
    this.attemptFrom = attemptFrom;
  }

  public ContentItemUpdate dueAt(@Nullable Optional<OffsetDateTime> dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Cohort: due date.
   * @return dueAt
   */
  
  @JsonProperty("dueAt")
  public @Nullable Optional<OffsetDateTime> getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable Optional<OffsetDateTime> dueAt) {
    this.dueAt = dueAt;
  }

  public ContentItemUpdate attemptUntilCohortEnd(@Nullable Optional<Boolean> attemptUntilCohortEnd) {
    this.attemptUntilCohortEnd = attemptUntilCohortEnd;
    return this;
  }

  /**
   * Attempts allowed until the cohort ends.
   * @return attemptUntilCohortEnd
   */
  
  @JsonProperty("attemptUntilCohortEnd")
  public @Nullable Optional<Boolean> getAttemptUntilCohortEnd() {
    return attemptUntilCohortEnd;
  }

  @JsonProperty("attemptUntilCohortEnd")
  public void setAttemptUntilCohortEnd(@Nullable Optional<Boolean> attemptUntilCohortEnd) {
    this.attemptUntilCohortEnd = attemptUntilCohortEnd;
  }

  public ContentItemUpdate visibleStartDay(@Nullable Optional<Integer> visibleStartDay) {
    this.visibleStartDay = visibleStartDay;
    return this;
  }

  /**
   * Template: visible from day N of the cohort.
   * @return visibleStartDay
   */
  
  @JsonProperty("visibleStartDay")
  public @Nullable Optional<Integer> getVisibleStartDay() {
    return visibleStartDay;
  }

  @JsonProperty("visibleStartDay")
  public void setVisibleStartDay(@Nullable Optional<Integer> visibleStartDay) {
    this.visibleStartDay = visibleStartDay;
  }

  public ContentItemUpdate visibleStartTime(@Nullable Optional<String> visibleStartTime) {
    this.visibleStartTime = visibleStartTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return visibleStartTime
   */
  
  @JsonProperty("visibleStartTime")
  public @Nullable Optional<String> getVisibleStartTime() {
    return visibleStartTime;
  }

  @JsonProperty("visibleStartTime")
  public void setVisibleStartTime(@Nullable Optional<String> visibleStartTime) {
    this.visibleStartTime = visibleStartTime;
  }

  public ContentItemUpdate visibleEndDay(@Nullable Optional<Integer> visibleEndDay) {
    this.visibleEndDay = visibleEndDay;
    return this;
  }

  /**
   * Template: visible until day N.
   * @return visibleEndDay
   */
  
  @JsonProperty("visibleEndDay")
  public @Nullable Optional<Integer> getVisibleEndDay() {
    return visibleEndDay;
  }

  @JsonProperty("visibleEndDay")
  public void setVisibleEndDay(@Nullable Optional<Integer> visibleEndDay) {
    this.visibleEndDay = visibleEndDay;
  }

  public ContentItemUpdate visibleEndTime(@Nullable Optional<String> visibleEndTime) {
    this.visibleEndTime = visibleEndTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return visibleEndTime
   */
  
  @JsonProperty("visibleEndTime")
  public @Nullable Optional<String> getVisibleEndTime() {
    return visibleEndTime;
  }

  @JsonProperty("visibleEndTime")
  public void setVisibleEndTime(@Nullable Optional<String> visibleEndTime) {
    this.visibleEndTime = visibleEndTime;
  }

  public ContentItemUpdate attemptStartDay(@Nullable Optional<Integer> attemptStartDay) {
    this.attemptStartDay = attemptStartDay;
    return this;
  }

  /**
   * Template: attempts open on day N.
   * @return attemptStartDay
   */
  
  @JsonProperty("attemptStartDay")
  public @Nullable Optional<Integer> getAttemptStartDay() {
    return attemptStartDay;
  }

  @JsonProperty("attemptStartDay")
  public void setAttemptStartDay(@Nullable Optional<Integer> attemptStartDay) {
    this.attemptStartDay = attemptStartDay;
  }

  public ContentItemUpdate attemptStartTime(@Nullable Optional<String> attemptStartTime) {
    this.attemptStartTime = attemptStartTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return attemptStartTime
   */
  
  @JsonProperty("attemptStartTime")
  public @Nullable Optional<String> getAttemptStartTime() {
    return attemptStartTime;
  }

  @JsonProperty("attemptStartTime")
  public void setAttemptStartTime(@Nullable Optional<String> attemptStartTime) {
    this.attemptStartTime = attemptStartTime;
  }

  public ContentItemUpdate attemptEndDay(@Nullable Optional<Integer> attemptEndDay) {
    this.attemptEndDay = attemptEndDay;
    return this;
  }

  /**
   * Template: due on day N.
   * @return attemptEndDay
   */
  
  @JsonProperty("attemptEndDay")
  public @Nullable Optional<Integer> getAttemptEndDay() {
    return attemptEndDay;
  }

  @JsonProperty("attemptEndDay")
  public void setAttemptEndDay(@Nullable Optional<Integer> attemptEndDay) {
    this.attemptEndDay = attemptEndDay;
  }

  public ContentItemUpdate attemptEndTime(@Nullable Optional<String> attemptEndTime) {
    this.attemptEndTime = attemptEndTime;
    return this;
  }

  /**
   * Template: time on that day.
   * @return attemptEndTime
   */
  
  @JsonProperty("attemptEndTime")
  public @Nullable Optional<String> getAttemptEndTime() {
    return attemptEndTime;
  }

  @JsonProperty("attemptEndTime")
  public void setAttemptEndTime(@Nullable Optional<String> attemptEndTime) {
    this.attemptEndTime = attemptEndTime;
  }

  public ContentItemUpdate lateSubmissionAllowed(@Nullable Optional<Boolean> lateSubmissionAllowed) {
    this.lateSubmissionAllowed = lateSubmissionAllowed;
    return this;
  }

  /**
   * Late submissions accepted.
   * @return lateSubmissionAllowed
   */
  
  @JsonProperty("lateSubmissionAllowed")
  public @Nullable Optional<Boolean> getLateSubmissionAllowed() {
    return lateSubmissionAllowed;
  }

  @JsonProperty("lateSubmissionAllowed")
  public void setLateSubmissionAllowed(@Nullable Optional<Boolean> lateSubmissionAllowed) {
    this.lateSubmissionAllowed = lateSubmissionAllowed;
  }

  public ContentItemUpdate lateSubmissionDays(@Nullable Optional<Integer> lateSubmissionDays) {
    this.lateSubmissionDays = lateSubmissionDays;
    return this;
  }

  /**
   * Days late submissions are accepted.
   * @return lateSubmissionDays
   */
  
  @JsonProperty("lateSubmissionDays")
  public @Nullable Optional<Integer> getLateSubmissionDays() {
    return lateSubmissionDays;
  }

  @JsonProperty("lateSubmissionDays")
  public void setLateSubmissionDays(@Nullable Optional<Integer> lateSubmissionDays) {
    this.lateSubmissionDays = lateSubmissionDays;
  }

  public ContentItemUpdate isGraded(@Nullable Optional<Boolean> isGraded) {
    this.isGraded = isGraded;
    return this;
  }

  /**
   * Counts in the gradebook.
   * @return isGraded
   */
  
  @JsonProperty("isGraded")
  public @Nullable Optional<Boolean> getIsGraded() {
    return isGraded;
  }

  @JsonProperty("isGraded")
  public void setIsGraded(@Nullable Optional<Boolean> isGraded) {
    this.isGraded = isGraded;
  }

  public ContentItemUpdate points(@Nullable Optional<String> points) {
    this.points = points;
    return this;
  }

  /**
   * Maximum points.
   * @return points
   */
  
  @JsonProperty("points")
  public @Nullable Optional<String> getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(@Nullable Optional<String> points) {
    this.points = points;
  }

  public ContentItemUpdate weightPercent(@Nullable Optional<String> weightPercent) {
    this.weightPercent = weightPercent;
    return this;
  }

  /**
   * Weight inside its grading category.
   * @return weightPercent
   */
  
  @JsonProperty("weightPercent")
  public @Nullable Optional<String> getWeightPercent() {
    return weightPercent;
  }

  @JsonProperty("weightPercent")
  public void setWeightPercent(@Nullable Optional<String> weightPercent) {
    this.weightPercent = weightPercent;
  }

  public ContentItemUpdate gradingCategoryId(@Nullable Optional<UUID> gradingCategoryId) {
    this.gradingCategoryId = gradingCategoryId;
    return this;
  }

  /**
   * Gradebook category.
   * @return gradingCategoryId
   */
  
  @JsonProperty("gradingCategoryId")
  public @Nullable Optional<UUID> getGradingCategoryId() {
    return gradingCategoryId;
  }

  @JsonProperty("gradingCategoryId")
  public void setGradingCategoryId(@Nullable Optional<UUID> gradingCategoryId) {
    this.gradingCategoryId = gradingCategoryId;
  }

  public ContentItemUpdate rubricId(@Nullable Optional<UUID> rubricId) {
    this.rubricId = rubricId;
    return this;
  }

  /**
   * Rubric used for grading.
   * @return rubricId
   */
  
  @JsonProperty("rubricId")
  public @Nullable Optional<UUID> getRubricId() {
    return rubricId;
  }

  @JsonProperty("rubricId")
  public void setRubricId(@Nullable Optional<UUID> rubricId) {
    this.rubricId = rubricId;
  }

  public ContentItemUpdate plagiarismCheckRequired(@Nullable Optional<Boolean> plagiarismCheckRequired) {
    this.plagiarismCheckRequired = plagiarismCheckRequired;
    return this;
  }

  /**
   * Submissions are sent for plagiarism checking.
   * @return plagiarismCheckRequired
   */
  
  @JsonProperty("plagiarismCheckRequired")
  public @Nullable Optional<Boolean> getPlagiarismCheckRequired() {
    return plagiarismCheckRequired;
  }

  @JsonProperty("plagiarismCheckRequired")
  public void setPlagiarismCheckRequired(@Nullable Optional<Boolean> plagiarismCheckRequired) {
    this.plagiarismCheckRequired = plagiarismCheckRequired;
  }

  public ContentItemUpdate showInCalendar(@Nullable Optional<Boolean> showInCalendar) {
    this.showInCalendar = showInCalendar;
    return this;
  }

  /**
   * Dates appear in students' calendars.
   * @return showInCalendar
   */
  
  @JsonProperty("showInCalendar")
  public @Nullable Optional<Boolean> getShowInCalendar() {
    return showInCalendar;
  }

  @JsonProperty("showInCalendar")
  public void setShowInCalendar(@Nullable Optional<Boolean> showInCalendar) {
    this.showInCalendar = showInCalendar;
  }

  public ContentItemUpdate allowDelete(@Nullable Optional<Boolean> allowDelete) {
    this.allowDelete = allowDelete;
    return this;
  }

  /**
   * Cohort teachers may remove the item.
   * @return allowDelete
   */
  
  @JsonProperty("allowDelete")
  public @Nullable Optional<Boolean> getAllowDelete() {
    return allowDelete;
  }

  @JsonProperty("allowDelete")
  public void setAllowDelete(@Nullable Optional<Boolean> allowDelete) {
    this.allowDelete = allowDelete;
  }

  public ContentItemUpdate allowEditDetails(@Nullable Optional<Boolean> allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
    return this;
  }

  /**
   * Cohort teachers may edit details.
   * @return allowEditDetails
   */
  
  @JsonProperty("allowEditDetails")
  public @Nullable Optional<Boolean> getAllowEditDetails() {
    return allowEditDetails;
  }

  @JsonProperty("allowEditDetails")
  public void setAllowEditDetails(@Nullable Optional<Boolean> allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
  }

  public ContentItemUpdate allowEditDates(@Nullable Optional<Boolean> allowEditDates) {
    this.allowEditDates = allowEditDates;
    return this;
  }

  /**
   * Cohort teachers may change dates.
   * @return allowEditDates
   */
  
  @JsonProperty("allowEditDates")
  public @Nullable Optional<Boolean> getAllowEditDates() {
    return allowEditDates;
  }

  @JsonProperty("allowEditDates")
  public void setAllowEditDates(@Nullable Optional<Boolean> allowEditDates) {
    this.allowEditDates = allowEditDates;
  }

  public ContentItemUpdate allowEditGrading(@Nullable Optional<Boolean> allowEditGrading) {
    this.allowEditGrading = allowEditGrading;
    return this;
  }

  /**
   * Cohort teachers may change grading settings.
   * @return allowEditGrading
   */
  
  @JsonProperty("allowEditGrading")
  public @Nullable Optional<Boolean> getAllowEditGrading() {
    return allowEditGrading;
  }

  @JsonProperty("allowEditGrading")
  public void setAllowEditGrading(@Nullable Optional<Boolean> allowEditGrading) {
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

