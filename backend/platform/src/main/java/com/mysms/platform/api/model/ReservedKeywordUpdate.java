// Owned API contract of platform.
package com.mysms.platform.api.model;

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
 * Fields of ReservedKeyword that can be updated (partial update; see updateMask).
 */

public class ReservedKeywordUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String keyword;

  public ReservedKeywordUpdate keyword(@Nullable String keyword) {
    this.keyword = keyword;
    return this;
  }

  /**
   * Reserved word.
   * @return keyword
   */
  @Size(max = 120) 
  @JsonProperty("keyword")
  public @Nullable String getKeyword() {
    return keyword;
  }

  @JsonProperty("keyword")
  public void setKeyword(@Nullable String keyword) {
    this.keyword = keyword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReservedKeywordUpdate reservedKeywordUpdate = (ReservedKeywordUpdate) o;
    return Objects.equals(this.keyword, reservedKeywordUpdate.keyword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReservedKeywordUpdate {\n");
    sb.append("    keyword: ").append(toIndentedString(keyword)).append("\n");
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

