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
 * Fields of ForumPostVote that can be updated (partial update; see updateMask).
 */

public class ForumPostVoteUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID forumPostId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isLike;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime votedAt;

  public ForumPostVoteUpdate forumPostId(@Nullable UUID forumPostId) {
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

  public ForumPostVoteUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Voter.
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

  public ForumPostVoteUpdate isLike(@Nullable Boolean isLike) {
    this.isLike = isLike;
    return this;
  }

  /**
   * Like (true) or unlike (false).
   * @return isLike
   */
  
  @JsonProperty("isLike")
  public @Nullable Boolean getIsLike() {
    return isLike;
  }

  @JsonProperty("isLike")
  public void setIsLike(@Nullable Boolean isLike) {
    this.isLike = isLike;
  }

  public ForumPostVoteUpdate votedAt(@Nullable OffsetDateTime votedAt) {
    this.votedAt = votedAt;
    return this;
  }

  /**
   * When.
   * @return votedAt
   */
  @Valid 
  @JsonProperty("votedAt")
  public @Nullable OffsetDateTime getVotedAt() {
    return votedAt;
  }

  @JsonProperty("votedAt")
  public void setVotedAt(@Nullable OffsetDateTime votedAt) {
    this.votedAt = votedAt;
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
        Objects.equals(this.userId, forumPostVoteUpdate.userId) &&
        Objects.equals(this.isLike, forumPostVoteUpdate.isLike) &&
        Objects.equals(this.votedAt, forumPostVoteUpdate.votedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumPostId, userId, isLike, votedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumPostVoteUpdate {\n");
    sb.append("    forumPostId: ").append(toIndentedString(forumPostId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    isLike: ").append(toIndentedString(isLike)).append("\n");
    sb.append("    votedAt: ").append(toIndentedString(votedAt)).append("\n");
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

