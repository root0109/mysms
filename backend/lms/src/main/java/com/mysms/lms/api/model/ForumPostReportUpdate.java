// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ForumPostReport that can be updated (partial update; see updateMask).
 */

public class ForumPostReportUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID forumPostId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reportCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reportedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime reportedAt;

  public ForumPostReportUpdate forumPostId(@Nullable UUID forumPostId) {
    this.forumPostId = forumPostId;
    return this;
  }

  /**
   * Post.
   * @return forumPostId
   */
  @Valid 
  @JsonProperty("forumPostId")
  public @Nullable UUID getForumPostId() {
    return forumPostId;
  }

  @JsonProperty("forumPostId")
  public void setForumPostId(@Nullable UUID forumPostId) {
    this.forumPostId = forumPostId;
  }

  public ForumPostReportUpdate reportCategoryId(@Nullable UUID reportCategoryId) {
    this.reportCategoryId = reportCategoryId;
    return this;
  }

  /**
   * Reason category.
   * @return reportCategoryId
   */
  @Valid 
  @JsonProperty("reportCategoryId")
  public @Nullable UUID getReportCategoryId() {
    return reportCategoryId;
  }

  @JsonProperty("reportCategoryId")
  public void setReportCategoryId(@Nullable UUID reportCategoryId) {
    this.reportCategoryId = reportCategoryId;
  }

  public ForumPostReportUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Details.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ForumPostReportUpdate reportedBy(@Nullable UUID reportedBy) {
    this.reportedBy = reportedBy;
    return this;
  }

  /**
   * Reporter.
   * @return reportedBy
   */
  @Valid 
  @JsonProperty("reportedBy")
  public @Nullable UUID getReportedBy() {
    return reportedBy;
  }

  @JsonProperty("reportedBy")
  public void setReportedBy(@Nullable UUID reportedBy) {
    this.reportedBy = reportedBy;
  }

  public ForumPostReportUpdate reportedAt(@Nullable OffsetDateTime reportedAt) {
    this.reportedAt = reportedAt;
    return this;
  }

  /**
   * When.
   * @return reportedAt
   */
  @Valid 
  @JsonProperty("reportedAt")
  public @Nullable OffsetDateTime getReportedAt() {
    return reportedAt;
  }

  @JsonProperty("reportedAt")
  public void setReportedAt(@Nullable OffsetDateTime reportedAt) {
    this.reportedAt = reportedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForumPostReportUpdate forumPostReportUpdate = (ForumPostReportUpdate) o;
    return Objects.equals(this.forumPostId, forumPostReportUpdate.forumPostId) &&
        Objects.equals(this.reportCategoryId, forumPostReportUpdate.reportCategoryId) &&
        Objects.equals(this.description, forumPostReportUpdate.description) &&
        Objects.equals(this.reportedBy, forumPostReportUpdate.reportedBy) &&
        Objects.equals(this.reportedAt, forumPostReportUpdate.reportedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumPostId, reportCategoryId, description, reportedBy, reportedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumPostReportUpdate {\n");
    sb.append("    forumPostId: ").append(toIndentedString(forumPostId)).append("\n");
    sb.append("    reportCategoryId: ").append(toIndentedString(reportCategoryId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    reportedBy: ").append(toIndentedString(reportedBy)).append("\n");
    sb.append("    reportedAt: ").append(toIndentedString(reportedAt)).append("\n");
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

