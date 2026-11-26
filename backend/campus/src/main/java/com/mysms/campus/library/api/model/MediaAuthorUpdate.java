// Owned API contract of library.
package com.mysms.campus.library.api.model;

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
 * Fields of MediaAuthor that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class MediaAuthorUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> authorId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public MediaAuthorUpdate authorId(@Nullable Optional<UUID> authorId) {
    this.authorId = authorId;
    return this;
  }

  /**
   * Author.
   * @return authorId
   */
  
  @JsonProperty("authorId")
  public @Nullable Optional<UUID> getAuthorId() {
    return authorId;
  }

  @JsonProperty("authorId")
  public void setAuthorId(@Nullable Optional<UUID> authorId) {
    this.authorId = authorId;
  }

  public MediaAuthorUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Author order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MediaAuthorUpdate mediaAuthorUpdate = (MediaAuthorUpdate) o;
    return Objects.equals(this.authorId, mediaAuthorUpdate.authorId) &&
        Objects.equals(this.sortOrder, mediaAuthorUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorId, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MediaAuthorUpdate {\n");
    sb.append("    authorId: ").append(toIndentedString(authorId)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

