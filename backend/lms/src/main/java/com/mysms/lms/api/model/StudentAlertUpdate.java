// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of StudentAlert that can be updated (partial update; see updateMask).
 */

public class StudentAlertUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String metricCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime sentAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String details;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime readAt;

  public StudentAlertUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student concerned.
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

  public StudentAlertUpdate cohortId(@Nullable UUID cohortId) {
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

  public StudentAlertUpdate courseId(@Nullable UUID courseId) {
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

  public StudentAlertUpdate metricCode(@Nullable String metricCode) {
    this.metricCode = metricCode;
    return this;
  }

  /**
   * Analytics metric / chart that triggered the alert.
   * @return metricCode
   */
  @Size(max = 100) 
  @JsonProperty("metricCode")
  public @Nullable String getMetricCode() {
    return metricCode;
  }

  @JsonProperty("metricCode")
  public void setMetricCode(@Nullable String metricCode) {
    this.metricCode = metricCode;
  }

  public StudentAlertUpdate contentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Assessment item concerned.
   * @return contentItemId
   */
  @Valid 
  @JsonProperty("contentItemId")
  public @Nullable UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public StudentAlertUpdate sentAt(@Nullable OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * Sent.
   * @return sentAt
   */
  @Valid 
  @JsonProperty("sentAt")
  public @Nullable OffsetDateTime getSentAt() {
    return sentAt;
  }

  @JsonProperty("sentAt")
  public void setSentAt(@Nullable OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public StudentAlertUpdate subject(@Nullable String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Subject.
   * @return subject
   */
  @Size(max = 255) 
  @JsonProperty("subject")
  public @Nullable String getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(@Nullable String subject) {
    this.subject = subject;
  }

  public StudentAlertUpdate message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * Message.
   * @return message
   */
  
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  public StudentAlertUpdate details(@Nullable String details) {
    this.details = details;
    return this;
  }

  /**
   * Benchmark details.
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable String getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable String details) {
    this.details = details;
  }

  public StudentAlertUpdate readAt(@Nullable OffsetDateTime readAt) {
    this.readAt = readAt;
    return this;
  }

  /**
   * Read by the recipient.
   * @return readAt
   */
  @Valid 
  @JsonProperty("readAt")
  public @Nullable OffsetDateTime getReadAt() {
    return readAt;
  }

  @JsonProperty("readAt")
  public void setReadAt(@Nullable OffsetDateTime readAt) {
    this.readAt = readAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentAlertUpdate studentAlertUpdate = (StudentAlertUpdate) o;
    return Objects.equals(this.userId, studentAlertUpdate.userId) &&
        Objects.equals(this.cohortId, studentAlertUpdate.cohortId) &&
        Objects.equals(this.courseId, studentAlertUpdate.courseId) &&
        Objects.equals(this.metricCode, studentAlertUpdate.metricCode) &&
        Objects.equals(this.contentItemId, studentAlertUpdate.contentItemId) &&
        Objects.equals(this.sentAt, studentAlertUpdate.sentAt) &&
        Objects.equals(this.subject, studentAlertUpdate.subject) &&
        Objects.equals(this.message, studentAlertUpdate.message) &&
        Objects.equals(this.details, studentAlertUpdate.details) &&
        Objects.equals(this.readAt, studentAlertUpdate.readAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, cohortId, courseId, metricCode, contentItemId, sentAt, subject, message, details, readAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentAlertUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    metricCode: ").append(toIndentedString(metricCode)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
    sb.append("    readAt: ").append(toIndentedString(readAt)).append("\n");
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

