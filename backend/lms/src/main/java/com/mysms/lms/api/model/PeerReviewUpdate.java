// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of PeerReview that can be updated (partial update; see updateMask).
 */

public class PeerReviewUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignmentGroupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reviewerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID revieweeUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String points;

  public PeerReviewUpdate assignmentGroupId(@Nullable UUID assignmentGroupId) {
    this.assignmentGroupId = assignmentGroupId;
    return this;
  }

  /**
   * Group.
   * @return assignmentGroupId
   */
  @Valid 
  @JsonProperty("assignmentGroupId")
  public @Nullable UUID getAssignmentGroupId() {
    return assignmentGroupId;
  }

  @JsonProperty("assignmentGroupId")
  public void setAssignmentGroupId(@Nullable UUID assignmentGroupId) {
    this.assignmentGroupId = assignmentGroupId;
  }

  public PeerReviewUpdate reviewerUserId(@Nullable UUID reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
    return this;
  }

  /**
   * Reviewer.
   * @return reviewerUserId
   */
  @Valid 
  @JsonProperty("reviewerUserId")
  public @Nullable UUID getReviewerUserId() {
    return reviewerUserId;
  }

  @JsonProperty("reviewerUserId")
  public void setReviewerUserId(@Nullable UUID reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
  }

  public PeerReviewUpdate revieweeUserId(@Nullable UUID revieweeUserId) {
    this.revieweeUserId = revieweeUserId;
    return this;
  }

  /**
   * Member reviewed.
   * @return revieweeUserId
   */
  @Valid 
  @JsonProperty("revieweeUserId")
  public @Nullable UUID getRevieweeUserId() {
    return revieweeUserId;
  }

  @JsonProperty("revieweeUserId")
  public void setRevieweeUserId(@Nullable UUID revieweeUserId) {
    this.revieweeUserId = revieweeUserId;
  }

  public PeerReviewUpdate points(@Nullable String points) {
    this.points = points;
    return this;
  }

  /**
   * Points given.
   * @return points
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("points")
  public @Nullable String getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(@Nullable String points) {
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

