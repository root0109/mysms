// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of TimetableEntry that can be updated (partial update; see updateMask).
 */

public class TimetableEntryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID timetableTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID quizId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID exerciseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID liveClassId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate entryDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer dayNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isMilestone;

  public TimetableEntryUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort (when on a cohort).
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

  public TimetableEntryUpdate timetableTemplateId(@Nullable UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template (when on a template).
   * @return timetableTemplateId
   */
  @Valid 
  @JsonProperty("timetableTemplateId")
  public @Nullable UUID getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(@Nullable UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public TimetableEntryUpdate lessonId(@Nullable UUID lessonId) {
    this.lessonId = lessonId;
    return this;
  }

  /**
   * Lesson.
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

  public TimetableEntryUpdate quizId(@Nullable UUID quizId) {
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

  public TimetableEntryUpdate assignmentId(@Nullable UUID assignmentId) {
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

  public TimetableEntryUpdate exerciseId(@Nullable UUID exerciseId) {
    this.exerciseId = exerciseId;
    return this;
  }

  /**
   * Exercise.
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

  public TimetableEntryUpdate liveClassId(@Nullable UUID liveClassId) {
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

  public TimetableEntryUpdate entryDate(@Nullable LocalDate entryDate) {
    this.entryDate = entryDate;
    return this;
  }

  /**
   * Date (cohort timetables).
   * @return entryDate
   */
  @Valid 
  @JsonProperty("entryDate")
  public @Nullable LocalDate getEntryDate() {
    return entryDate;
  }

  @JsonProperty("entryDate")
  public void setEntryDate(@Nullable LocalDate entryDate) {
    this.entryDate = entryDate;
  }

  public TimetableEntryUpdate dayNumber(@Nullable Integer dayNumber) {
    this.dayNumber = dayNumber;
    return this;
  }

  /**
   * Day offset from the start (templates).
   * @return dayNumber
   */
  
  @JsonProperty("dayNumber")
  public @Nullable Integer getDayNumber() {
    return dayNumber;
  }

  @JsonProperty("dayNumber")
  public void setDayNumber(@Nullable Integer dayNumber) {
    this.dayNumber = dayNumber;
  }

  public TimetableEntryUpdate startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start.
   * @return startTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public @Nullable String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable String startTime) {
    this.startTime = startTime;
  }

  public TimetableEntryUpdate endTime(@Nullable String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * End.
   * @return endTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("endTime")
  public @Nullable String getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable String endTime) {
    this.endTime = endTime;
  }

  public TimetableEntryUpdate durationMinutes(@Nullable String durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Duration.
   * @return durationMinutes
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?s$") 
  @JsonProperty("durationMinutes")
  public @Nullable String getDurationMinutes() {
    return durationMinutes;
  }

  @JsonProperty("durationMinutes")
  public void setDurationMinutes(@Nullable String durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public TimetableEntryUpdate isMilestone(@Nullable Boolean isMilestone) {
    this.isMilestone = isMilestone;
    return this;
  }

  /**
   * Marks a milestone.
   * @return isMilestone
   */
  
  @JsonProperty("isMilestone")
  public @Nullable Boolean getIsMilestone() {
    return isMilestone;
  }

  @JsonProperty("isMilestone")
  public void setIsMilestone(@Nullable Boolean isMilestone) {
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

