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
 * MatchAnswer
 */

public class MatchAnswer {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID leftId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID rightId;

  public MatchAnswer() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MatchAnswer(UUID leftId, UUID rightId) {
    this.leftId = leftId;
    this.rightId = rightId;
  }

  public MatchAnswer leftId(UUID leftId) {
    this.leftId = leftId;
    return this;
  }

  /**
   * Left item.
   * @return leftId
   */
  @NotNull @Valid 
  @JsonProperty("leftId")
  public UUID getLeftId() {
    return leftId;
  }

  @JsonProperty("leftId")
  public void setLeftId(UUID leftId) {
    this.leftId = leftId;
  }

  public MatchAnswer rightId(UUID rightId) {
    this.rightId = rightId;
    return this;
  }

  /**
   * Matched right item.
   * @return rightId
   */
  @NotNull @Valid 
  @JsonProperty("rightId")
  public UUID getRightId() {
    return rightId;
  }

  @JsonProperty("rightId")
  public void setRightId(UUID rightId) {
    this.rightId = rightId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MatchAnswer matchAnswer = (MatchAnswer) o;
    return Objects.equals(this.leftId, matchAnswer.leftId) &&
        Objects.equals(this.rightId, matchAnswer.rightId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leftId, rightId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MatchAnswer {\n");
    sb.append("    leftId: ").append(toIndentedString(leftId)).append("\n");
    sb.append("    rightId: ").append(toIndentedString(rightId)).append("\n");
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

