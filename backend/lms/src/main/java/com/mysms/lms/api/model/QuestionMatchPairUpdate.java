// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of QuestionMatchPair that can be updated (partial update; see updateMask).
 */

public class QuestionMatchPairUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String leftText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String rightText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public QuestionMatchPairUpdate leftText(@Nullable String leftText) {
    this.leftText = leftText;
    return this;
  }

  /**
   * Left column item.
   * @return leftText
   */
  @Size(max = 500) 
  @JsonProperty("leftText")
  public @Nullable String getLeftText() {
    return leftText;
  }

  @JsonProperty("leftText")
  public void setLeftText(@Nullable String leftText) {
    this.leftText = leftText;
  }

  public QuestionMatchPairUpdate rightText(@Nullable String rightText) {
    this.rightText = rightText;
    return this;
  }

  /**
   * Matching right column item.
   * @return rightText
   */
  @Size(max = 500) 
  @JsonProperty("rightText")
  public @Nullable String getRightText() {
    return rightText;
  }

  @JsonProperty("rightText")
  public void setRightText(@Nullable String rightText) {
    this.rightText = rightText;
  }

  public QuestionMatchPairUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
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
    QuestionMatchPairUpdate questionMatchPairUpdate = (QuestionMatchPairUpdate) o;
    return Objects.equals(this.leftText, questionMatchPairUpdate.leftText) &&
        Objects.equals(this.rightText, questionMatchPairUpdate.rightText) &&
        Objects.equals(this.sortOrder, questionMatchPairUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leftText, rightText, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionMatchPairUpdate {\n");
    sb.append("    leftText: ").append(toIndentedString(leftText)).append("\n");
    sb.append("    rightText: ").append(toIndentedString(rightText)).append("\n");
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

