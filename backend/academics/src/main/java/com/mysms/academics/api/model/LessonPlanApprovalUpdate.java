// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
package com.mysms.academics.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of LessonPlanApproval that can be updated (partial update; see updateMask).
 */

public class LessonPlanApprovalUpdate {

  /**
   * Decision.
   */
  public enum DecisionEnum {
    PENDING("pending"),
    
    APPROVED("approved"),
    
    REJECTED("rejected");

    private final String value;

    DecisionEnum(String value) {
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
    public static DecisionEnum fromValue(String value) {
      for (DecisionEnum b : DecisionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DecisionEnum decision;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String comments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID decidedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime decidedAt;

  public LessonPlanApprovalUpdate decision(@Nullable DecisionEnum decision) {
    this.decision = decision;
    return this;
  }

  /**
   * Decision.
   * @return decision
   */
  
  @JsonProperty("decision")
  public @Nullable DecisionEnum getDecision() {
    return decision;
  }

  @JsonProperty("decision")
  public void setDecision(@Nullable DecisionEnum decision) {
    this.decision = decision;
  }

  public LessonPlanApprovalUpdate comments(@Nullable String comments) {
    this.comments = comments;
    return this;
  }

  /**
   * Reviewer comments.
   * @return comments
   */
  
  @JsonProperty("comments")
  public @Nullable String getComments() {
    return comments;
  }

  @JsonProperty("comments")
  public void setComments(@Nullable String comments) {
    this.comments = comments;
  }

  public LessonPlanApprovalUpdate decidedByUserId(@Nullable UUID decidedByUserId) {
    this.decidedByUserId = decidedByUserId;
    return this;
  }

  /**
   * Reviewer.
   * @return decidedByUserId
   */
  @Valid 
  @JsonProperty("decidedByUserId")
  public @Nullable UUID getDecidedByUserId() {
    return decidedByUserId;
  }

  @JsonProperty("decidedByUserId")
  public void setDecidedByUserId(@Nullable UUID decidedByUserId) {
    this.decidedByUserId = decidedByUserId;
  }

  public LessonPlanApprovalUpdate decidedAt(@Nullable OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
    return this;
  }

  /**
   * Decision time.
   * @return decidedAt
   */
  @Valid 
  @JsonProperty("decidedAt")
  public @Nullable OffsetDateTime getDecidedAt() {
    return decidedAt;
  }

  @JsonProperty("decidedAt")
  public void setDecidedAt(@Nullable OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonPlanApprovalUpdate lessonPlanApprovalUpdate = (LessonPlanApprovalUpdate) o;
    return Objects.equals(this.decision, lessonPlanApprovalUpdate.decision) &&
        Objects.equals(this.comments, lessonPlanApprovalUpdate.comments) &&
        Objects.equals(this.decidedByUserId, lessonPlanApprovalUpdate.decidedByUserId) &&
        Objects.equals(this.decidedAt, lessonPlanApprovalUpdate.decidedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(decision, comments, decidedByUserId, decidedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonPlanApprovalUpdate {\n");
    sb.append("    decision: ").append(toIndentedString(decision)).append("\n");
    sb.append("    comments: ").append(toIndentedString(comments)).append("\n");
    sb.append("    decidedByUserId: ").append(toIndentedString(decidedByUserId)).append("\n");
    sb.append("    decidedAt: ").append(toIndentedString(decidedAt)).append("\n");
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

