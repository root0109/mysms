// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RecordItemViewRequest
 */

public class RecordItemViewRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean liked;

  public RecordItemViewRequest liked(@Nullable Boolean liked) {
    this.liked = liked;
    return this;
  }

  /**
   * Like / unlike at the same time.
   * @return liked
   */
  
  @JsonProperty("liked")
  public @Nullable Boolean getLiked() {
    return liked;
  }

  @JsonProperty("liked")
  public void setLiked(@Nullable Boolean liked) {
    this.liked = liked;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordItemViewRequest recordItemViewRequest = (RecordItemViewRequest) o;
    return Objects.equals(this.liked, recordItemViewRequest.liked);
  }

  @Override
  public int hashCode() {
    return Objects.hash(liked);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordItemViewRequest {\n");
    sb.append("    liked: ").append(toIndentedString(liked)).append("\n");
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

