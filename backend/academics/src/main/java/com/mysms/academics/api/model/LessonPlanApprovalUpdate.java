// Owned API contract of academics.
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
 * Fields of LessonPlanApproval that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
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
  private @Nullable Optional<@NotNull DecisionEnum> decision;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> comments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> decidedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> decidedAt;

  public LessonPlanApprovalUpdate decision(@Nullable Optional<DecisionEnum> decision) {
    this.decision = decision;
    return this;
  }

  /**
   * Decision.
   * @return decision
   */
  
  @JsonProperty("decision")
  public @Nullable Optional<DecisionEnum> getDecision() {
    return decision;
  }

  @JsonProperty("decision")
  public void setDecision(@Nullable Optional<DecisionEnum> decision) {
    this.decision = decision;
  }

  public LessonPlanApprovalUpdate comments(@Nullable Optional<String> comments) {
    this.comments = comments;
    return this;
  }

  /**
   * Reviewer comments.
   * @return comments
   */
  
  @JsonProperty("comments")
  public @Nullable Optional<String> getComments() {
    return comments;
  }

  @JsonProperty("comments")
  public void setComments(@Nullable Optional<String> comments) {
    this.comments = comments;
  }

  public LessonPlanApprovalUpdate decidedByUserId(@Nullable Optional<UUID> decidedByUserId) {
    this.decidedByUserId = decidedByUserId;
    return this;
  }

  /**
   * Reviewer.
   * @return decidedByUserId
   */
  
  @JsonProperty("decidedByUserId")
  public @Nullable Optional<UUID> getDecidedByUserId() {
    return decidedByUserId;
  }

  @JsonProperty("decidedByUserId")
  public void setDecidedByUserId(@Nullable Optional<UUID> decidedByUserId) {
    this.decidedByUserId = decidedByUserId;
  }

  public LessonPlanApprovalUpdate decidedAt(@Nullable Optional<OffsetDateTime> decidedAt) {
    this.decidedAt = decidedAt;
    return this;
  }

  /**
   * Decision time.
   * @return decidedAt
   */
  
  @JsonProperty("decidedAt")
  public @Nullable Optional<OffsetDateTime> getDecidedAt() {
    return decidedAt;
  }

  @JsonProperty("decidedAt")
  public void setDecidedAt(@Nullable Optional<OffsetDateTime> decidedAt) {
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

