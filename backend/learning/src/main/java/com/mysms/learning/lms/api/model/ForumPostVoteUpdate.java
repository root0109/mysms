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
 * Fields of ForumPostVote that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ForumPostVoteUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> forumPostId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isLike;

  public ForumPostVoteUpdate forumPostId(@Nullable Optional<UUID> forumPostId) {
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

  public ForumPostVoteUpdate isLike(@Nullable Optional<Boolean> isLike) {
    this.isLike = isLike;
    return this;
  }

  /**
   * Like (true) or unlike (false).
   * @return isLike
   */
  
  @JsonProperty("isLike")
  public @Nullable Optional<Boolean> getIsLike() {
    return isLike;
  }

  @JsonProperty("isLike")
  public void setIsLike(@Nullable Optional<Boolean> isLike) {
    this.isLike = isLike;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForumPostVoteUpdate forumPostVoteUpdate = (ForumPostVoteUpdate) o;
    return Objects.equals(this.forumPostId, forumPostVoteUpdate.forumPostId) &&
        Objects.equals(this.isLike, forumPostVoteUpdate.isLike);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumPostId, isLike);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumPostVoteUpdate {\n");
    sb.append("    forumPostId: ").append(toIndentedString(forumPostId)).append("\n");
    sb.append("    isLike: ").append(toIndentedString(isLike)).append("\n");
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

