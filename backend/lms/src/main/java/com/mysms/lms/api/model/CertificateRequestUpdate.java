// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CertificateRequest that can be updated (partial update; see updateMask).
 */

public class CertificateRequestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID evaluatorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate requestDate;

  /**
   * Day.
   */
  public enum WeekdayEnum {
    MONDAY("monday"),
    
    TUESDAY("tuesday"),
    
    WEDNESDAY("wednesday"),
    
    THURSDAY("thursday"),
    
    FRIDAY("friday"),
    
    SATURDAY("saturday"),
    
    SUNDAY("sunday");

    private final String value;

    WeekdayEnum(String value) {
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
    public static WeekdayEnum fromValue(String value) {
      for (WeekdayEnum b : WeekdayEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable WeekdayEnum weekday;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingLink;

  public CertificateRequestUpdate courseId(@Nullable UUID courseId) {
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

  public CertificateRequestUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public CertificateRequestUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
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

  public CertificateRequestUpdate evaluatorUserId(@Nullable UUID evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
    return this;
  }

  /**
   * Evaluator.
   * @return evaluatorUserId
   */
  @Valid 
  @JsonProperty("evaluatorUserId")
  public @Nullable UUID getEvaluatorUserId() {
    return evaluatorUserId;
  }

  @JsonProperty("evaluatorUserId")
  public void setEvaluatorUserId(@Nullable UUID evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
  }

  public CertificateRequestUpdate requestDate(@Nullable LocalDate requestDate) {
    this.requestDate = requestDate;
    return this;
  }

  /**
   * Evaluation date.
   * @return requestDate
   */
  @Valid 
  @JsonProperty("requestDate")
  public @Nullable LocalDate getRequestDate() {
    return requestDate;
  }

  @JsonProperty("requestDate")
  public void setRequestDate(@Nullable LocalDate requestDate) {
    this.requestDate = requestDate;
  }

  public CertificateRequestUpdate weekday(@Nullable WeekdayEnum weekday) {
    this.weekday = weekday;
    return this;
  }

  /**
   * Day.
   * @return weekday
   */
  
  @JsonProperty("weekday")
  public @Nullable WeekdayEnum getWeekday() {
    return weekday;
  }

  @JsonProperty("weekday")
  public void setWeekday(@Nullable WeekdayEnum weekday) {
    this.weekday = weekday;
  }

  public CertificateRequestUpdate startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Slot start.
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

  public CertificateRequestUpdate endTime(@Nullable String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Slot end.
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

  public CertificateRequestUpdate timezone(@Nullable String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone.
   * @return timezone
   */
  @Size(max = 64) 
  @JsonProperty("timezone")
  public @Nullable String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable String timezone) {
    this.timezone = timezone;
  }

  public CertificateRequestUpdate meetingLink(@Nullable String meetingLink) {
    this.meetingLink = meetingLink;
    return this;
  }

  /**
   * Meeting link.
   * @return meetingLink
   */
  @Size(max = 500) 
  @JsonProperty("meetingLink")
  public @Nullable String getMeetingLink() {
    return meetingLink;
  }

  @JsonProperty("meetingLink")
  public void setMeetingLink(@Nullable String meetingLink) {
    this.meetingLink = meetingLink;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CertificateRequestUpdate certificateRequestUpdate = (CertificateRequestUpdate) o;
    return Objects.equals(this.courseId, certificateRequestUpdate.courseId) &&
        Objects.equals(this.userId, certificateRequestUpdate.userId) &&
        Objects.equals(this.cohortId, certificateRequestUpdate.cohortId) &&
        Objects.equals(this.evaluatorUserId, certificateRequestUpdate.evaluatorUserId) &&
        Objects.equals(this.requestDate, certificateRequestUpdate.requestDate) &&
        Objects.equals(this.weekday, certificateRequestUpdate.weekday) &&
        Objects.equals(this.startTime, certificateRequestUpdate.startTime) &&
        Objects.equals(this.endTime, certificateRequestUpdate.endTime) &&
        Objects.equals(this.timezone, certificateRequestUpdate.timezone) &&
        Objects.equals(this.meetingLink, certificateRequestUpdate.meetingLink);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, userId, cohortId, evaluatorUserId, requestDate, weekday, startTime, endTime, timezone, meetingLink);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CertificateRequestUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    evaluatorUserId: ").append(toIndentedString(evaluatorUserId)).append("\n");
    sb.append("    requestDate: ").append(toIndentedString(requestDate)).append("\n");
    sb.append("    weekday: ").append(toIndentedString(weekday)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    meetingLink: ").append(toIndentedString(meetingLink)).append("\n");
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

