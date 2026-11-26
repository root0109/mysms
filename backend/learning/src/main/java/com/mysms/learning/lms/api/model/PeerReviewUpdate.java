// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PeerReview that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PeerReviewUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> assignmentGroupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> reviewerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> revieweeUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> points;

  public PeerReviewUpdate assignmentGroupId(@Nullable Optional<UUID> assignmentGroupId) {
    this.assignmentGroupId = assignmentGroupId;
    return this;
  }

  /**
   * Group.
   * @return assignmentGroupId
   */
  
  @JsonProperty("assignmentGroupId")
  public @Nullable Optional<UUID> getAssignmentGroupId() {
    return assignmentGroupId;
  }

  @JsonProperty("assignmentGroupId")
  public void setAssignmentGroupId(@Nullable Optional<UUID> assignmentGroupId) {
    this.assignmentGroupId = assignmentGroupId;
  }

  public PeerReviewUpdate reviewerUserId(@Nullable Optional<UUID> reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
    return this;
  }

  /**
   * Reviewer.
   * @return reviewerUserId
   */
  
  @JsonProperty("reviewerUserId")
  public @Nullable Optional<UUID> getReviewerUserId() {
    return reviewerUserId;
  }

  @JsonProperty("reviewerUserId")
  public void setReviewerUserId(@Nullable Optional<UUID> reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
  }

  public PeerReviewUpdate revieweeUserId(@Nullable Optional<UUID> revieweeUserId) {
    this.revieweeUserId = revieweeUserId;
    return this;
  }

  /**
   * Member reviewed.
   * @return revieweeUserId
   */
  
  @JsonProperty("revieweeUserId")
  public @Nullable Optional<UUID> getRevieweeUserId() {
    return revieweeUserId;
  }

  @JsonProperty("revieweeUserId")
  public void setRevieweeUserId(@Nullable Optional<UUID> revieweeUserId) {
    this.revieweeUserId = revieweeUserId;
  }

  public PeerReviewUpdate points(@Nullable Optional<String> points) {
    this.points = points;
    return this;
  }

  /**
   * Points given.
   * @return points
   */
  
  @JsonProperty("points")
  public @Nullable Optional<String> getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(@Nullable Optional<String> points) {
    this.points = points;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PeerReviewUpdate peerReviewUpdate = (PeerReviewUpdate) o;
    return Objects.equals(this.assignmentGroupId, peerReviewUpdate.assignmentGroupId) &&
        Objects.equals(this.reviewerUserId, peerReviewUpdate.reviewerUserId) &&
        Objects.equals(this.revieweeUserId, peerReviewUpdate.revieweeUserId) &&
        Objects.equals(this.points, peerReviewUpdate.points);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignmentGroupId, reviewerUserId, revieweeUserId, points);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PeerReviewUpdate {\n");
    sb.append("    assignmentGroupId: ").append(toIndentedString(assignmentGroupId)).append("\n");
    sb.append("    reviewerUserId: ").append(toIndentedString(reviewerUserId)).append("\n");
    sb.append("    revieweeUserId: ").append(toIndentedString(revieweeUserId)).append("\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
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

