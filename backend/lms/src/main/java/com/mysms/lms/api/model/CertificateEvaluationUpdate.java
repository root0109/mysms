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
 * Fields of CertificateEvaluation that can be updated (partial update; see updateMask).
 */

public class CertificateEvaluationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID certificateRequestId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID evaluatorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate evaluationDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String rating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String summary;

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
  private @Nullable StatusEnum status;

  public CertificateEvaluationUpdate courseId(@Nullable UUID courseId) {
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

  public CertificateEvaluationUpdate userId(@Nullable UUID userId) {
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

  public CertificateEvaluationUpdate cohortId(@Nullable UUID cohortId) {
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

  public CertificateEvaluationUpdate certificateRequestId(@Nullable UUID certificateRequestId) {
    this.certificateRequestId = certificateRequestId;
    return this;
  }

  /**
   * Booking evaluated.
   * @return certificateRequestId
   */
  @Valid 
  @JsonProperty("certificateRequestId")
  public @Nullable UUID getCertificateRequestId() {
    return certificateRequestId;
  }

  @JsonProperty("certificateRequestId")
  public void setCertificateRequestId(@Nullable UUID certificateRequestId) {
    this.certificateRequestId = certificateRequestId;
  }

  public CertificateEvaluationUpdate evaluatorUserId(@Nullable UUID evaluatorUserId) {
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

  public CertificateEvaluationUpdate evaluationDate(@Nullable LocalDate evaluationDate) {
    this.evaluationDate = evaluationDate;
    return this;
  }

  /**
   * Date.
   * @return evaluationDate
   */
  @Valid 
  @JsonProperty("evaluationDate")
  public @Nullable LocalDate getEvaluationDate() {
    return evaluationDate;
  }

  @JsonProperty("evaluationDate")
  public void setEvaluationDate(@Nullable LocalDate evaluationDate) {
    this.evaluationDate = evaluationDate;
  }

  public CertificateEvaluationUpdate startTime(@Nullable String startTime) {
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

  public CertificateEvaluationUpdate endTime(@Nullable String endTime) {
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

  public CertificateEvaluationUpdate rating(@Nullable String rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Rating (0-5).
   * @return rating
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("rating")
  public @Nullable String getRating() {
    return rating;
  }

  @JsonProperty("rating")
  public void setRating(@Nullable String rating) {
    this.rating = rating;
  }

  public CertificateEvaluationUpdate summary(@Nullable String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Evaluator summary.
   * @return summary
   */
  
  @JsonProperty("summary")
  public @Nullable String getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  public void setSummary(@Nullable String summary) {
    this.summary = summary;
  }

  public CertificateEvaluationUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Outcome.
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

