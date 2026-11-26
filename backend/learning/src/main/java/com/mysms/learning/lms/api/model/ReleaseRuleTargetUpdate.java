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
 * Fields of ReleaseRuleTarget that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ReleaseRuleTargetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> chapterId;

  public ReleaseRuleTargetUpdate contentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item released.
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

  public ReleaseRuleTargetUpdate chapterId(@Nullable Optional<UUID> chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter released.
   * @return chapterId
   */
  
  @JsonProperty("chapterId")
  public @Nullable Optional<UUID> getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(@Nullable Optional<UUID> chapterId) {
    this.chapterId = chapterId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReleaseRuleTargetUpdate releaseRuleTargetUpdate = (ReleaseRuleTargetUpdate) o;
    return Objects.equals(this.contentItemId, releaseRuleTargetUpdate.contentItemId) &&
        Objects.equals(this.chapterId, releaseRuleTargetUpdate.chapterId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, chapterId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReleaseRuleTargetUpdate {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
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

