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
 * Fields of ForumPost that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ForumPostUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> parentPostId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isDraft;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showSignature;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> usesInlineAuthoring;

  public ForumPostUpdate parentPostId(@Nullable Optional<UUID> parentPostId) {
    this.parentPostId = parentPostId;
    return this;
  }

  /**
   * Post replied to.
   * @return parentPostId
   */
  
  @JsonProperty("parentPostId")
  public @Nullable Optional<UUID> getParentPostId() {
    return parentPostId;
  }

  @JsonProperty("parentPostId")
  public void setParentPostId(@Nullable Optional<UUID> parentPostId) {
    this.parentPostId = parentPostId;
  }

  public ForumPostUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public ForumPostUpdate body(@Nullable Optional<String> body) {
    this.body = body;
    return this;
  }

  /**
   * Message.
   * @return body
   */
  
  @JsonProperty("body")
  public @Nullable Optional<String> getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(@Nullable Optional<String> body) {
    this.body = body;
  }

  public ForumPostUpdate isDraft(@Nullable Optional<Boolean> isDraft) {
    this.isDraft = isDraft;
    return this;
  }

  /**
   * Draft (not yet posted).
   * @return isDraft
   */
  
  @JsonProperty("isDraft")
  public @Nullable Optional<Boolean> getIsDraft() {
    return isDraft;
  }

  @JsonProperty("isDraft")
  public void setIsDraft(@Nullable Optional<Boolean> isDraft) {
    this.isDraft = isDraft;
  }

  public ForumPostUpdate showSignature(@Nullable Optional<Boolean> showSignature) {
    this.showSignature = showSignature;
    return this;
  }

  /**
   * Append the author's signature.
   * @return showSignature
   */
  
  @JsonProperty("showSignature")
  public @Nullable Optional<Boolean> getShowSignature() {
    return showSignature;
  }

  @JsonProperty("showSignature")
  public void setShowSignature(@Nullable Optional<Boolean> showSignature) {
    this.showSignature = showSignature;
  }

  public ForumPostUpdate usesInlineAuthoring(@Nullable Optional<Boolean> usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
    return this;
  }

  /**
   * Authored with the inline editor.
   * @return usesInlineAuthoring
   */
  
  @JsonProperty("usesInlineAuthoring")
  public @Nullable Optional<Boolean> getUsesInlineAuthoring() {
    return usesInlineAuthoring;
  }

  @JsonProperty("usesInlineAuthoring")
  public void setUsesInlineAuthoring(@Nullable Optional<Boolean> usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForumPostUpdate forumPostUpdate = (ForumPostUpdate) o;
    return Objects.equals(this.parentPostId, forumPostUpdate.parentPostId) &&
        Objects.equals(this.title, forumPostUpdate.title) &&
        Objects.equals(this.body, forumPostUpdate.body) &&
        Objects.equals(this.isDraft, forumPostUpdate.isDraft) &&
        Objects.equals(this.showSignature, forumPostUpdate.showSignature) &&
        Objects.equals(this.usesInlineAuthoring, forumPostUpdate.usesInlineAuthoring);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentPostId, title, body, isDraft, showSignature, usesInlineAuthoring);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumPostUpdate {\n");
    sb.append("    parentPostId: ").append(toIndentedString(parentPostId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    isDraft: ").append(toIndentedString(isDraft)).append("\n");
    sb.append("    showSignature: ").append(toIndentedString(showSignature)).append("\n");
    sb.append("    usesInlineAuthoring: ").append(toIndentedString(usesInlineAuthoring)).append("\n");
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

