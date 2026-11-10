// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.GradeInput;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GradeEntryRequest
 */

public class GradeEntryRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID groupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private GradeInput grade;

  public GradeEntryRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradeEntryRequest(UUID contentItemId, GradeInput grade) {
    this.contentItemId = contentItemId;
    this.grade = grade;
  }

  public GradeEntryRequest contentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Graded item.
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

  public GradeEntryRequest userId(@Nullable UUID userId) {
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

  public GradeEntryRequest groupId(@Nullable UUID groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Group (group assignments).
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

  public GradeEntryRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token of the existing entry; a stale token returns ABORTED / 409.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  public GradeEntryRequest grade(GradeInput grade) {
    this.grade = grade;
    return this;
  }

  /**
   * Get grade
   * @return grade
   */
  @NotNull @Valid 
  @JsonProperty("grade")
  public GradeInput getGrade() {
    return grade;
  }

  @JsonProperty("grade")
  public void setGrade(GradeInput grade) {
    this.grade = grade;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradeEntryRequest gradeEntryRequest = (GradeEntryRequest) o;
    return Objects.equals(this.contentItemId, gradeEntryRequest.contentItemId) &&
        Objects.equals(this.userId, gradeEntryRequest.userId) &&
        Objects.equals(this.groupId, gradeEntryRequest.groupId) &&
        Objects.equals(this.etag, gradeEntryRequest.etag) &&
        Objects.equals(this.grade, gradeEntryRequest.grade);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, userId, groupId, etag, grade);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradeEntryRequest {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    grade: ").append(toIndentedString(grade)).append("\n");
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

