// Owned API contract of timetable.
package com.mysms.academics.timetable.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Session that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SessionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> periodId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> weekday;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> roomId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> sessionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> color;

  public SessionUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public SessionUpdate periodId(@Nullable Optional<UUID> periodId) {
    this.periodId = periodId;
    return this;
  }

  /**
   * Period.
   * @return periodId
   */
  
  @JsonProperty("periodId")
  public @Nullable Optional<UUID> getPeriodId() {
    return periodId;
  }

  @JsonProperty("periodId")
  public void setPeriodId(@Nullable Optional<UUID> periodId) {
    this.periodId = periodId;
  }

  public SessionUpdate startAt(@Nullable Optional<OffsetDateTime> startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Start.
   * @return startAt
   */
  
  @JsonProperty("startAt")
  public @Nullable Optional<OffsetDateTime> getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(@Nullable Optional<OffsetDateTime> startAt) {
    this.startAt = startAt;
  }

  public SessionUpdate endAt(@Nullable Optional<OffsetDateTime> endAt) {
    this.endAt = endAt;
    return this;
  }

  /**
   * End.
   * @return endAt
   */
  
  @JsonProperty("endAt")
  public @Nullable Optional<OffsetDateTime> getEndAt() {
    return endAt;
  }

  @JsonProperty("endAt")
  public void setEndAt(@Nullable Optional<OffsetDateTime> endAt) {
    this.endAt = endAt;
  }

  public SessionUpdate weekday(@Nullable Optional<Integer> weekday) {
    this.weekday = weekday;
    return this;
  }

  /**
   * ISO weekday (1 = Monday).
   * @return weekday
   */
  
  @JsonProperty("weekday")
  public @Nullable Optional<Integer> getWeekday() {
    return weekday;
  }

  @JsonProperty("weekday")
  public void setWeekday(@Nullable Optional<Integer> weekday) {
    this.weekday = weekday;
  }

  public SessionUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section (K-12).
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public SessionUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (higher education).
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

  public SessionUpdate batchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (higher education).
   * @return batchId
   */
  
  @JsonProperty("batchId")
  public @Nullable Optional<UUID> getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
  }

  public SessionUpdate subjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject taught.
   * @return subjectId
   */
  
  @JsonProperty("subjectId")
  public @Nullable Optional<UUID> getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
  }

  public SessionUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Teacher.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public SessionUpdate roomId(@Nullable Optional<UUID> roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Room.
   * @return roomId
   */
  
  @JsonProperty("roomId")
  public @Nullable Optional<UUID> getRoomId() {
    return roomId;
  }

  @JsonProperty("roomId")
  public void setRoomId(@Nullable Optional<UUID> roomId) {
    this.roomId = roomId;
  }

  public SessionUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Display title.
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

  public SessionUpdate sessionType(@Nullable Optional<String> sessionType) {
    this.sessionType = sessionType;
    return this;
  }

  /**
   * Session type (lecture, lab, tutorial...).
   * @return sessionType
   */
  
  @JsonProperty("sessionType")
  public @Nullable Optional<String> getSessionType() {
    return sessionType;
  }

  @JsonProperty("sessionType")
  public void setSessionType(@Nullable Optional<String> sessionType) {
    this.sessionType = sessionType;
  }

  public SessionUpdate color(@Nullable Optional<Integer> color) {
    this.color = color;
    return this;
  }

  /**
   * Calendar colour index.
   * @return color
   */
  
  @JsonProperty("color")
  public @Nullable Optional<Integer> getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable Optional<Integer> color) {
    this.color = color;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SessionUpdate sessionUpdate = (SessionUpdate) o;
    return Objects.equals(this.institutionId, sessionUpdate.institutionId) &&
        Objects.equals(this.periodId, sessionUpdate.periodId) &&
        Objects.equals(this.startAt, sessionUpdate.startAt) &&
        Objects.equals(this.endAt, sessionUpdate.endAt) &&
        Objects.equals(this.weekday, sessionUpdate.weekday) &&
        Objects.equals(this.classSectionId, sessionUpdate.classSectionId) &&
        Objects.equals(this.courseId, sessionUpdate.courseId) &&
        Objects.equals(this.batchId, sessionUpdate.batchId) &&
        Objects.equals(this.subjectId, sessionUpdate.subjectId) &&
        Objects.equals(this.staffId, sessionUpdate.staffId) &&
        Objects.equals(this.roomId, sessionUpdate.roomId) &&
        Objects.equals(this.title, sessionUpdate.title) &&
        Objects.equals(this.sessionType, sessionUpdate.sessionType) &&
        Objects.equals(this.color, sessionUpdate.color);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, periodId, startAt, endAt, weekday, classSectionId, courseId, batchId, subjectId, staffId, roomId, title, sessionType, color);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SessionUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    periodId: ").append(toIndentedString(periodId)).append("\n");
    sb.append("    startAt: ").append(toIndentedString(startAt)).append("\n");
    sb.append("    endAt: ").append(toIndentedString(endAt)).append("\n");
    sb.append("    weekday: ").append(toIndentedString(weekday)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    sessionType: ").append(toIndentedString(sessionType)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
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

