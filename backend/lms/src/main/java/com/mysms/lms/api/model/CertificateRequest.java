// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Student&#39;s booking of an evaluation slot for certification.
 */

public class CertificateRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingLinkMasked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID evaluatorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate requestDate;

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
  private String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingLink;

  /**
   * State. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    UPCOMING("upcoming"),
    
    COMPLETED("completed"),
    
    CANCELLED("cancelled");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

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

  public CertificateRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CertificateRequest(UUID courseId, UUID userId, LocalDate requestDate, String startTime, String endTime) {
    this.courseId = courseId;
    this.userId = userId;
    this.requestDate = requestDate;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public CertificateRequest meetingLinkMasked(@Nullable String meetingLinkMasked) {
    this.meetingLinkMasked = meetingLinkMasked;
    return this;
  }

  /**
   * Masked display form of the evaluation meeting link.
   * @return meetingLinkMasked
   */
  
  @JsonProperty("meetingLinkMasked")
  public @Nullable String getMeetingLinkMasked() {
    return meetingLinkMasked;
  }

  @JsonProperty("meetingLinkMasked")
  public void setMeetingLinkMasked(@Nullable String meetingLinkMasked) {
    this.meetingLinkMasked = meetingLinkMasked;
  }

  public CertificateRequest id(@Nullable UUID id) {
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

  public CertificateRequest courseId(UUID courseId) {
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

  public CertificateRequest userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public CertificateRequest cohortId(@Nullable UUID cohortId) {
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

  public CertificateRequest evaluatorUserId(@Nullable UUID evaluatorUserId) {
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

  public CertificateRequest requestDate(LocalDate requestDate) {
    this.requestDate = requestDate;
    return this;
  }

  /**
   * Evaluation date.
   * @return requestDate
   */
  @NotNull @Valid 
  @JsonProperty("requestDate")
  public LocalDate getRequestDate() {
    return requestDate;
  }

  @JsonProperty("requestDate")
  public void setRequestDate(LocalDate requestDate) {
    this.requestDate = requestDate;
  }

  public CertificateRequest weekday(@Nullable WeekdayEnum weekday) {
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

  public CertificateRequest startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Slot start.
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public CertificateRequest endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Slot end.
   * @return endTime
   */
  @NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public CertificateRequest timezone(@Nullable String timezone) {
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

  public CertificateRequest meetingLink(@Nullable String meetingLink) {
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

  public CertificateRequest status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State. Changed only through the workflow operations.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public CertificateRequest createTime(@Nullable OffsetDateTime createTime) {
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

  public CertificateRequest createdBy(@Nullable UUID createdBy) {
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

  public CertificateRequest updateTime(@Nullable OffsetDateTime updateTime) {
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

  public CertificateRequest updatedBy(@Nullable UUID updatedBy) {
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

  public CertificateRequest etag(@Nullable String etag) {
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
    CertificateRequest certificateRequest = (CertificateRequest) o;
    return Objects.equals(this.meetingLinkMasked, certificateRequest.meetingLinkMasked) &&
        Objects.equals(this.id, certificateRequest.id) &&
        Objects.equals(this.courseId, certificateRequest.courseId) &&
        Objects.equals(this.userId, certificateRequest.userId) &&
        Objects.equals(this.cohortId, certificateRequest.cohortId) &&
        Objects.equals(this.evaluatorUserId, certificateRequest.evaluatorUserId) &&
        Objects.equals(this.requestDate, certificateRequest.requestDate) &&
        Objects.equals(this.weekday, certificateRequest.weekday) &&
        Objects.equals(this.startTime, certificateRequest.startTime) &&
        Objects.equals(this.endTime, certificateRequest.endTime) &&
        Objects.equals(this.timezone, certificateRequest.timezone) &&
        Objects.equals(this.meetingLink, certificateRequest.meetingLink) &&
        Objects.equals(this.status, certificateRequest.status) &&
        Objects.equals(this.createTime, certificateRequest.createTime) &&
        Objects.equals(this.createdBy, certificateRequest.createdBy) &&
        Objects.equals(this.updateTime, certificateRequest.updateTime) &&
        Objects.equals(this.updatedBy, certificateRequest.updatedBy) &&
        Objects.equals(this.etag, certificateRequest.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(meetingLinkMasked, id, courseId, userId, cohortId, evaluatorUserId, requestDate, weekday, startTime, endTime, timezone, meetingLink, status, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CertificateRequest {\n");
    sb.append("    meetingLinkMasked: ").append(toIndentedString(meetingLinkMasked)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

