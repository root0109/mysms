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
 * Fields of RubricAssessment that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RubricAssessmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> rubricCellId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> comment;

  public RubricAssessmentUpdate contentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Graded item.
   * @return contentItemId
   */
  
  @JsonProperty("contentItemId")
  public @Nullable Optional<UUID> getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
  }

  public RubricAssessmentUpdate userId(@Nullable Optional<UUID> userId) {
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

  public RubricAssessmentUpdate groupId(@Nullable Optional<UUID> groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group.
   * @return groupId
   */
  
  @JsonProperty("groupId")
  public @Nullable Optional<UUID> getGroupId() {
    return groupId;
  }

  @JsonProperty("groupId")
  public void setGroupId(@Nullable Optional<UUID> groupId) {
    this.groupId = groupId;
  }

  public RubricAssessmentUpdate rubricCellId(@Nullable Optional<UUID> rubricCellId) {
    this.rubricCellId = rubricCellId;
    return this;
  }

  /**
   * Chosen cell (criterion × level).
   * @return rubricCellId
   */
  
  @JsonProperty("rubricCellId")
  public @Nullable Optional<UUID> getRubricCellId() {
    return rubricCellId;
  }

  @JsonProperty("rubricCellId")
  public void setRubricCellId(@Nullable Optional<UUID> rubricCellId) {
    this.rubricCellId = rubricCellId;
  }

  public RubricAssessmentUpdate comment(@Nullable Optional<String> comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Comment on the criterion.
   * @return comment
   */
  
  @JsonProperty("comment")
  public @Nullable Optional<String> getComment() {
    return comment;
  }

  @JsonProperty("comment")
  public void setComment(@Nullable Optional<String> comment) {
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RubricAssessmentUpdate rubricAssessmentUpdate = (RubricAssessmentUpdate) o;
    return Objects.equals(this.contentItemId, rubricAssessmentUpdate.contentItemId) &&
        Objects.equals(this.userId, rubricAssessmentUpdate.userId) &&
        Objects.equals(this.groupId, rubricAssessmentUpdate.groupId) &&
        Objects.equals(this.rubricCellId, rubricAssessmentUpdate.rubricCellId) &&
        Objects.equals(this.comment, rubricAssessmentUpdate.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, userId, groupId, rubricCellId, comment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RubricAssessmentUpdate {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    rubricCellId: ").append(toIndentedString(rubricCellId)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
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

