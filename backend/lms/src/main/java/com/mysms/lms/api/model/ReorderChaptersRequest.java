// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * ReorderChaptersRequest
 */

public class ReorderChaptersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> chapterIds = new ArrayList<>();

  public ReorderChaptersRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReorderChaptersRequest(List<UUID> chapterIds) {
    this.chapterIds = chapterIds;
  }

  public ReorderChaptersRequest chapterIds(List<UUID> chapterIds) {
    this.chapterIds = chapterIds;
    return this;
  }

  public ReorderChaptersRequest addChapterIdsItem(UUID chapterIdsItem) {
    if (this.chapterIds == null) {
      this.chapterIds = new ArrayList<>();
    }
    this.chapterIds.add(chapterIdsItem);
    return this;
  }

  /**
   * Chapters in the new order.
   * @return chapterIds
   */
  @NotNull 
  @JsonProperty("chapterIds")
  public List<UUID> getChapterIds() {
    return chapterIds;
  }

  @JsonProperty("chapterIds")
  public void setChapterIds(List<UUID> chapterIds) {
    this.chapterIds = chapterIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReorderChaptersRequest reorderChaptersRequest = (ReorderChaptersRequest) o;
    return Objects.equals(this.chapterIds, reorderChaptersRequest.chapterIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chapterIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReorderChaptersRequest {\n");
    sb.append("    chapterIds: ").append(toIndentedString(chapterIds)).append("\n");
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

