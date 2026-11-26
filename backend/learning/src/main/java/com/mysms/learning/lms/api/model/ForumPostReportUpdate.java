// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ForumPostReport that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ForumPostReportUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> forumPostId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> reportCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  public ForumPostReportUpdate forumPostId(@Nullable Optional<UUID> forumPostId) {
    this.forumPostId = forumPostId;
    return this;
  }

  /**
   * Post.
   * @return forumPostId
   */
  
  @JsonProperty("forumPostId")
  public @Nullable Optional<UUID> getForumPostId() {
    return forumPostId;
  }

  @JsonProperty("forumPostId")
  public void setForumPostId(@Nullable Optional<UUID> forumPostId) {
    this.forumPostId = forumPostId;
  }

  public ForumPostReportUpdate reportCategoryId(@Nullable Optional<UUID> reportCategoryId) {
    this.reportCategoryId = reportCategoryId;
    return this;
  }

  /**
   * Reason category.
   * @return reportCategoryId
   */
  
  @JsonProperty("reportCategoryId")
  public @Nullable Optional<UUID> getReportCategoryId() {
    return reportCategoryId;
  }

  @JsonProperty("reportCategoryId")
  public void setReportCategoryId(@Nullable Optional<UUID> reportCategoryId) {
    this.reportCategoryId = reportCategoryId;
  }

  public ForumPostReportUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Details.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
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
        Objects.equals(this.description, forumPostReportUpdate.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumPostId, reportCategoryId, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumPostReportUpdate {\n");
    sb.append("    forumPostId: ").append(toIndentedString(forumPostId)).append("\n");
    sb.append("    reportCategoryId: ").append(toIndentedString(reportCategoryId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

