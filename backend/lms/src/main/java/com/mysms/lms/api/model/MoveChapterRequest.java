// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * MoveChapterRequest
 */

public class MoveChapterRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentChapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer position;

  public MoveChapterRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MoveChapterRequest(Integer position) {
    this.position = position;
  }

  public MoveChapterRequest parentChapterId(@Nullable UUID parentChapterId) {
    this.parentChapterId = parentChapterId;
    return this;
  }

  /**
   * New parent (omit for top level).
   * @return parentChapterId
   */
  @Valid 
  @JsonProperty("parentChapterId")
  public @Nullable UUID getParentChapterId() {
    return parentChapterId;
  }

  @JsonProperty("parentChapterId")
  public void setParentChapterId(@Nullable UUID parentChapterId) {
    this.parentChapterId = parentChapterId;
  }

  public MoveChapterRequest position(Integer position) {
    this.position = position;
    return this;
  }

  /**
   * 1-based position.
   * @return position
   */
  @NotNull 
  @JsonProperty("position")
  public Integer getPosition() {
    return position;
  }

  @JsonProperty("position")
  public void setPosition(Integer position) {
    this.position = position;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MoveChapterRequest moveChapterRequest = (MoveChapterRequest) o;
    return Objects.equals(this.parentChapterId, moveChapterRequest.parentChapterId) &&
        Objects.equals(this.position, moveChapterRequest.position);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentChapterId, position);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MoveChapterRequest {\n");
    sb.append("    parentChapterId: ").append(toIndentedString(parentChapterId)).append("\n");
    sb.append("    position: ").append(toIndentedString(position)).append("\n");
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

