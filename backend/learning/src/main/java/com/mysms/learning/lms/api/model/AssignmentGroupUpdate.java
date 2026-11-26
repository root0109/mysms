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
 * Fields of AssignmentGroup that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AssignmentGroupUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> points;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> discussionTopicId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isReassigned;

  public AssignmentGroupUpdate cohortId(@Nullable Optional<UUID> cohortId) {
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

  public AssignmentGroupUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Group name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public AssignmentGroupUpdate points(@Nullable Optional<String> points) {
    this.points = points;
    return this;
  }

  /**
   * Points awarded to the group.
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

  public AssignmentGroupUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Group active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public AssignmentGroupUpdate discussionTopicId(@Nullable Optional<UUID> discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
    return this;
  }

  /**
   * Private discussion topic of the group.
   * @return discussionTopicId
   */
  
  @JsonProperty("discussionTopicId")
  public @Nullable Optional<UUID> getDiscussionTopicId() {
    return discussionTopicId;
  }

  @JsonProperty("discussionTopicId")
  public void setDiscussionTopicId(@Nullable Optional<UUID> discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
  }

  public AssignmentGroupUpdate isReassigned(@Nullable Optional<Boolean> isReassigned) {
    this.isReassigned = isReassigned;
    return this;
  }

  /**
   * Assignment was reassigned to the group.
   * @return isReassigned
   */
  
  @JsonProperty("isReassigned")
  public @Nullable Optional<Boolean> getIsReassigned() {
    return isReassigned;
  }

  @JsonProperty("isReassigned")
  public void setIsReassigned(@Nullable Optional<Boolean> isReassigned) {
    this.isReassigned = isReassigned;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignmentGroupUpdate assignmentGroupUpdate = (AssignmentGroupUpdate) o;
    return Objects.equals(this.cohortId, assignmentGroupUpdate.cohortId) &&
        Objects.equals(this.name, assignmentGroupUpdate.name) &&
        Objects.equals(this.points, assignmentGroupUpdate.points) &&
        Objects.equals(this.isActive, assignmentGroupUpdate.isActive) &&
        Objects.equals(this.discussionTopicId, assignmentGroupUpdate.discussionTopicId) &&
        Objects.equals(this.isReassigned, assignmentGroupUpdate.isReassigned);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, name, points, isActive, discussionTopicId, isReassigned);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignmentGroupUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    discussionTopicId: ").append(toIndentedString(discussionTopicId)).append("\n");
    sb.append("    isReassigned: ").append(toIndentedString(isReassigned)).append("\n");
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

