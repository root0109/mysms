// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of PostComment that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PostCommentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> parentCommentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public PostCommentUpdate parentCommentId(@Nullable Optional<UUID> parentCommentId) {
    this.parentCommentId = parentCommentId;
    return this;
  }

  /**
   * Comment replied to.
   * @return parentCommentId
   */
  
  @JsonProperty("parentCommentId")
  public @Nullable Optional<UUID> getParentCommentId() {
    return parentCommentId;
  }

  @JsonProperty("parentCommentId")
  public void setParentCommentId(@Nullable Optional<UUID> parentCommentId) {
    this.parentCommentId = parentCommentId;
  }

  public PostCommentUpdate body(@Nullable Optional<String> body) {
    this.body = body;
    return this;
  }

  /**
   * Comment text.
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

  public PostCommentUpdate attachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  
  @JsonProperty("attachmentFileId")
  public @Nullable Optional<UUID> getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public PostCommentUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Visible.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostCommentUpdate postCommentUpdate = (PostCommentUpdate) o;
    return Objects.equals(this.parentCommentId, postCommentUpdate.parentCommentId) &&
        Objects.equals(this.body, postCommentUpdate.body) &&
        Objects.equals(this.attachmentFileId, postCommentUpdate.attachmentFileId) &&
        Objects.equals(this.isActive, postCommentUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentCommentId, body, attachmentFileId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostCommentUpdate {\n");
    sb.append("    parentCommentId: ").append(toIndentedString(parentCommentId)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

