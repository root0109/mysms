// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

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
 * MarkFeedbackMessageRequest
 */

public class MarkFeedbackMessageRequest {

  /**
   * Status.
   */
  public enum ReviewStatusEnum {
    SEEN("seen"),
    
    ACTION_TAKEN("action_taken");

    private final String value;

    ReviewStatusEnum(String value) {
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
    public static ReviewStatusEnum fromValue(String value) {
      for (ReviewStatusEnum b : ReviewStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ReviewStatusEnum reviewStatus;

  public MarkFeedbackMessageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarkFeedbackMessageRequest(ReviewStatusEnum reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public MarkFeedbackMessageRequest reviewStatus(ReviewStatusEnum reviewStatus) {
    this.reviewStatus = reviewStatus;
    return this;
  }

  /**
   * Status.
   * @return reviewStatus
   */
  @NotNull 
  @JsonProperty("reviewStatus")
  public ReviewStatusEnum getReviewStatus() {
    return reviewStatus;
  }

  @JsonProperty("reviewStatus")
  public void setReviewStatus(ReviewStatusEnum reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarkFeedbackMessageRequest markFeedbackMessageRequest = (MarkFeedbackMessageRequest) o;
    return Objects.equals(this.reviewStatus, markFeedbackMessageRequest.reviewStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reviewStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarkFeedbackMessageRequest {\n");
    sb.append("    reviewStatus: ").append(toIndentedString(reviewStatus)).append("\n");
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

