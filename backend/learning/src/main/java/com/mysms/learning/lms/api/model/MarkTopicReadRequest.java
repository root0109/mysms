// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * MarkTopicReadRequest
 */

public class MarkTopicReadRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> forumPostIds = new ArrayList<>();

  public MarkTopicReadRequest forumPostIds(List<UUID> forumPostIds) {
    this.forumPostIds = forumPostIds;
    return this;
  }

  public MarkTopicReadRequest addForumPostIdsItem(UUID forumPostIdsItem) {
    if (this.forumPostIds == null) {
      this.forumPostIds = new ArrayList<>();
    }
    this.forumPostIds.add(forumPostIdsItem);
    return this;
  }

  /**
   * Posts read (default whole topic).
   * @return forumPostIds
   */
  
  @JsonProperty("forumPostIds")
  public List<UUID> getForumPostIds() {
    return forumPostIds;
  }

  @JsonProperty("forumPostIds")
  public void setForumPostIds(List<UUID> forumPostIds) {
    this.forumPostIds = forumPostIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarkTopicReadRequest markTopicReadRequest = (MarkTopicReadRequest) o;
    return Objects.equals(this.forumPostIds, markTopicReadRequest.forumPostIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumPostIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarkTopicReadRequest {\n");
    sb.append("    forumPostIds: ").append(toIndentedString(forumPostIds)).append("\n");
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

