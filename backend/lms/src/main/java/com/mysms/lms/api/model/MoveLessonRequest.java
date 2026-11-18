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
 * MoveLessonRequest
 */

public class MoveLessonRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID targetChapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer position;

  public MoveLessonRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MoveLessonRequest(UUID targetChapterId, Integer position) {
    this.targetChapterId = targetChapterId;
    this.position = position;
  }

  public MoveLessonRequest targetChapterId(UUID targetChapterId) {
    this.targetChapterId = targetChapterId;
    return this;
  }

  /**
   * Chapter.
   * @return targetChapterId
   */
  @NotNull @Valid 
  @JsonProperty("targetChapterId")
  public UUID getTargetChapterId() {
    return targetChapterId;
  }

  @JsonProperty("targetChapterId")
  public void setTargetChapterId(UUID targetChapterId) {
    this.targetChapterId = targetChapterId;
  }

  public MoveLessonRequest position(Integer position) {
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
    MoveLessonRequest moveLessonRequest = (MoveLessonRequest) o;
    return Objects.equals(this.targetChapterId, moveLessonRequest.targetChapterId) &&
        Objects.equals(this.position, moveLessonRequest.position);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetChapterId, position);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MoveLessonRequest {\n");
    sb.append("    targetChapterId: ").append(toIndentedString(targetChapterId)).append("\n");
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

