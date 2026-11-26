// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GradeLearningSubmissionRequest
 */

public class GradeLearningSubmissionRequest {

  /**
   * Result.
   */
  public enum StatusEnum {
    PASS("pass"),
    
    FAIL("fail"),
    
    NOT_APPLICABLE("not_applicable");

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
  private StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String evaluatorComments;

  public GradeLearningSubmissionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradeLearningSubmissionRequest(StatusEnum status) {
    this.status = status;
  }

  public GradeLearningSubmissionRequest status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Result.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public GradeLearningSubmissionRequest evaluatorComments(@Nullable String evaluatorComments) {
    this.evaluatorComments = evaluatorComments;
    return this;
  }

  /**
   * Feedback.
   * @return evaluatorComments
   */
  
  @JsonProperty("evaluatorComments")
  public @Nullable String getEvaluatorComments() {
    return evaluatorComments;
  }

  @JsonProperty("evaluatorComments")
  public void setEvaluatorComments(@Nullable String evaluatorComments) {
    this.evaluatorComments = evaluatorComments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradeLearningSubmissionRequest gradeLearningSubmissionRequest = (GradeLearningSubmissionRequest) o;
    return Objects.equals(this.status, gradeLearningSubmissionRequest.status) &&
        Objects.equals(this.evaluatorComments, gradeLearningSubmissionRequest.evaluatorComments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, evaluatorComments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradeLearningSubmissionRequest {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    evaluatorComments: ").append(toIndentedString(evaluatorComments)).append("\n");
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

