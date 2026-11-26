// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of TimetableEntry that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class TimetableEntryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> timetableTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> liveClassId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> entryDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> dayNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?s$") String> durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isMilestone;

  public TimetableEntryUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort (when on a cohort).
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

  public TimetableEntryUpdate timetableTemplateId(@Nullable Optional<UUID> timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template (when on a template).
   * @return timetableTemplateId
   */
  
  @JsonProperty("timetableTemplateId")
  public @Nullable Optional<UUID> getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(@Nullable Optional<UUID> timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public TimetableEntryUpdate lessonId(@Nullable Optional<UUID> lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson.
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

  public TimetableEntryUpdate quizId(@Nullable Optional<UUID> quizId) {
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

  public TimetableEntryUpdate assignmentId(@Nullable Optional<UUID> assignmentId) {
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

  public TimetableEntryUpdate exerciseId(@Nullable Optional<UUID> exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Exercise.
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

  public TimetableEntryUpdate liveClassId(@Nullable Optional<UUID> liveClassId) {
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

  public TimetableEntryUpdate entryDate(@Nullable Optional<LocalDate> entryDate) {
    this.entryDate = entryDate;
    return this;
  }

  /**
   * Date (cohort timetables).
   * @return entryDate
   */
  
  @JsonProperty("entryDate")
  public @Nullable Optional<LocalDate> getEntryDate() {
    return entryDate;
  }

  @JsonProperty("entryDate")
  public void setEntryDate(@Nullable Optional<LocalDate> entryDate) {
    this.entryDate = entryDate;
  }

  public TimetableEntryUpdate dayNumber(@Nullable Optional<Integer> dayNumber) {
    this.dayNumber = dayNumber;
    return this;
  }

  /**
   * Day offset from the start (templates).
   * @return dayNumber
   */
  
  @JsonProperty("dayNumber")
  public @Nullable Optional<Integer> getDayNumber() {
    return dayNumber;
  }

  @JsonProperty("dayNumber")
  public void setDayNumber(@Nullable Optional<Integer> dayNumber) {
    this.dayNumber = dayNumber;
  }

  public TimetableEntryUpdate startTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start.
   * @return startTime
   */
  
  @JsonProperty("startTime")
  public @Nullable Optional<String> getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
  }

  public TimetableEntryUpdate endTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * End.
   * @return endTime
   */
  
  @JsonProperty("endTime")
  public @Nullable Optional<String> getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
  }

  public TimetableEntryUpdate durationMinutes(@Nullable Optional<String> durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Duration.
   * @return durationMinutes
   */
  
  @JsonProperty("durationMinutes")
  public @Nullable Optional<String> getDurationMinutes() {
    return durationMinutes;
  }

  @JsonProperty("durationMinutes")
  public void setDurationMinutes(@Nullable Optional<String> durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public TimetableEntryUpdate isMilestone(@Nullable Optional<Boolean> isMilestone) {
    this.isMilestone = isMilestone;
    return this;
  }

  /**
   * Marks a milestone.
   * @return isMilestone
   */
  
  @JsonProperty("isMilestone")
  public @Nullable Optional<Boolean> getIsMilestone() {
    return isMilestone;
  }

  @JsonProperty("isMilestone")
  public void setIsMilestone(@Nullable Optional<Boolean> isMilestone) {
    this.isMilestone = isMilestone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimetableEntryUpdate timetableEntryUpdate = (TimetableEntryUpdate) o;
    return Objects.equals(this.cohortId, timetableEntryUpdate.cohortId) &&
        Objects.equals(this.timetableTemplateId, timetableEntryUpdate.timetableTemplateId) &&
        Objects.equals(this.lessonId, timetableEntryUpdate.lessonId) &&
        Objects.equals(this.quizId, timetableEntryUpdate.quizId) &&
        Objects.equals(this.assignmentId, timetableEntryUpdate.assignmentId) &&
        Objects.equals(this.exerciseId, timetableEntryUpdate.exerciseId) &&
        Objects.equals(this.liveClassId, timetableEntryUpdate.liveClassId) &&
        Objects.equals(this.entryDate, timetableEntryUpdate.entryDate) &&
        Objects.equals(this.dayNumber, timetableEntryUpdate.dayNumber) &&
        Objects.equals(this.startTime, timetableEntryUpdate.startTime) &&
        Objects.equals(this.endTime, timetableEntryUpdate.endTime) &&
        Objects.equals(this.durationMinutes, timetableEntryUpdate.durationMinutes) &&
        Objects.equals(this.isMilestone, timetableEntryUpdate.isMilestone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, timetableTemplateId, lessonId, quizId, assignmentId, exerciseId, liveClassId, entryDate, dayNumber, startTime, endTime, durationMinutes, isMilestone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimetableEntryUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    timetableTemplateId: ").append(toIndentedString(timetableTemplateId)).append("\n");
    sb.append("    lessonId: ").append(toIndentedString(lessonId)).append("\n");
    sb.append("    quizId: ").append(toIndentedString(quizId)).append("\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    exerciseId: ").append(toIndentedString(exerciseId)).append("\n");
    sb.append("    liveClassId: ").append(toIndentedString(liveClassId)).append("\n");
    sb.append("    entryDate: ").append(toIndentedString(entryDate)).append("\n");
    sb.append("    dayNumber: ").append(toIndentedString(dayNumber)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    isMilestone: ").append(toIndentedString(isMilestone)).append("\n");
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

