// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.GradebookEntry;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GradeEntryResult
 */

public class GradeEntryResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GradebookEntry entry;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String error;

  public GradeEntryResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradeEntryResult(UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public GradeEntryResult contentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item.
   * @return contentItemId
   */
  @NotNull @Valid 
  @JsonProperty("contentItemId")
  public UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public GradeEntryResult userId(@Nullable UUID userId) {
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

  public GradeEntryResult groupId(@Nullable UUID groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group.
   * @return groupId
   */
  @Valid 
  @JsonProperty("groupId")
  public @Nullable UUID getGroupId() {
    return groupId;
  }

  @JsonProperty("groupId")
  public void setGroupId(@Nullable UUID groupId) {
    this.groupId = groupId;
  }

  public GradeEntryResult entry(@Nullable GradebookEntry entry) {
    this.entry = entry;
    return this;
  }

  /**
   * Get entry
   * @return entry
   */
  @Valid 
  @JsonProperty("entry")
  public @Nullable GradebookEntry getEntry() {
    return entry;
  }

  @JsonProperty("entry")
  public void setEntry(@Nullable GradebookEntry entry) {
    this.entry = entry;
  }

  public GradeEntryResult error(@Nullable String error) {
    this.error = error;
    return this;
  }

  /**
   * Why this row was rejected (e.g. stale etag).
   * @return error
   */
  
  @JsonProperty("error")
  public @Nullable String getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable String error) {
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradeEntryResult gradeEntryResult = (GradeEntryResult) o;
    return Objects.equals(this.contentItemId, gradeEntryResult.contentItemId) &&
        Objects.equals(this.userId, gradeEntryResult.userId) &&
        Objects.equals(this.groupId, gradeEntryResult.groupId) &&
        Objects.equals(this.entry, gradeEntryResult.entry) &&
        Objects.equals(this.error, gradeEntryResult.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, userId, groupId, entry, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradeEntryResult {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    entry: ").append(toIndentedString(entry)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
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

