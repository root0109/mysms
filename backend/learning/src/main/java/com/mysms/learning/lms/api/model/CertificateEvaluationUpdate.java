// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of CertificateEvaluation that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CertificateEvaluationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> certificateRequestId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> evaluatorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> evaluationDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> rating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> summary;

  /**
   * Outcome.
   */
  public enum StatusEnum {
    PENDING("pending"),
    
    IN_PROGRESS("in_progress"),
    
    PASS("pass"),
    
    FAIL("fail");

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
  private @Nullable Optional<@NotNull StatusEnum> status;

  public CertificateEvaluationUpdate courseId(@Nullable Optional<UUID> courseId) {
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

  public CertificateEvaluationUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public CertificateEvaluationUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
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

  public CertificateEvaluationUpdate certificateRequestId(@Nullable Optional<UUID> certificateRequestId) {
    this.certificateRequestId = certificateRequestId;
    return this;
  }

  /**
   * Booking evaluated.
   * @return certificateRequestId
   */
  
  @JsonProperty("certificateRequestId")
  public @Nullable Optional<UUID> getCertificateRequestId() {
    return certificateRequestId;
  }

  @JsonProperty("certificateRequestId")
  public void setCertificateRequestId(@Nullable Optional<UUID> certificateRequestId) {
    this.certificateRequestId = certificateRequestId;
  }

  public CertificateEvaluationUpdate evaluatorUserId(@Nullable Optional<UUID> evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
    return this;
  }

  /**
   * Evaluator.
   * @return evaluatorUserId
   */
  
  @JsonProperty("evaluatorUserId")
  public @Nullable Optional<UUID> getEvaluatorUserId() {
    return evaluatorUserId;
  }

  @JsonProperty("evaluatorUserId")
  public void setEvaluatorUserId(@Nullable Optional<UUID> evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
  }

  public CertificateEvaluationUpdate evaluationDate(@Nullable Optional<LocalDate> evaluationDate) {
    this.evaluationDate = evaluationDate;
    return this;
  }

  /**
   * Date.
   * @return evaluationDate
   */
  
  @JsonProperty("evaluationDate")
  public @Nullable Optional<LocalDate> getEvaluationDate() {
    return evaluationDate;
  }

  @JsonProperty("evaluationDate")
  public void setEvaluationDate(@Nullable Optional<LocalDate> evaluationDate) {
    this.evaluationDate = evaluationDate;
  }

  public CertificateEvaluationUpdate startTime(@Nullable Optional<String> startTime) {
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

  public CertificateEvaluationUpdate endTime(@Nullable Optional<String> endTime) {
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

  public CertificateEvaluationUpdate rating(@Nullable Optional<String> rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Rating (0-5).
   * @return rating
   */
  
  @JsonProperty("rating")
  public @Nullable Optional<String> getRating() {
    return rating;
  }

  @JsonProperty("rating")
  public void setRating(@Nullable Optional<String> rating) {
    this.rating = rating;
  }

  public CertificateEvaluationUpdate summary(@Nullable Optional<String> summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Evaluator summary.
   * @return summary
   */
  
  @JsonProperty("summary")
  public @Nullable Optional<String> getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  public void setSummary(@Nullable Optional<String> summary) {
    this.summary = summary;
  }

  public CertificateEvaluationUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * Outcome.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CertificateEvaluationUpdate certificateEvaluationUpdate = (CertificateEvaluationUpdate) o;
    return Objects.equals(this.courseId, certificateEvaluationUpdate.courseId) &&
        Objects.equals(this.userId, certificateEvaluationUpdate.userId) &&
        Objects.equals(this.cohortId, certificateEvaluationUpdate.cohortId) &&
        Objects.equals(this.certificateRequestId, certificateEvaluationUpdate.certificateRequestId) &&
        Objects.equals(this.evaluatorUserId, certificateEvaluationUpdate.evaluatorUserId) &&
        Objects.equals(this.evaluationDate, certificateEvaluationUpdate.evaluationDate) &&
        Objects.equals(this.startTime, certificateEvaluationUpdate.startTime) &&
        Objects.equals(this.endTime, certificateEvaluationUpdate.endTime) &&
        Objects.equals(this.rating, certificateEvaluationUpdate.rating) &&
        Objects.equals(this.summary, certificateEvaluationUpdate.summary) &&
        Objects.equals(this.status, certificateEvaluationUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, userId, cohortId, certificateRequestId, evaluatorUserId, evaluationDate, startTime, endTime, rating, summary, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CertificateEvaluationUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    certificateRequestId: ").append(toIndentedString(certificateRequestId)).append("\n");
    sb.append("    evaluatorUserId: ").append(toIndentedString(evaluatorUserId)).append("\n");
    sb.append("    evaluationDate: ").append(toIndentedString(evaluationDate)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

