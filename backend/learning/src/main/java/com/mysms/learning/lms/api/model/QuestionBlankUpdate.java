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
 * Fields of QuestionBlank that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionBlankUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 500) String> blankText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public QuestionBlankUpdate blankText(@Nullable Optional<String> blankText) {
    this.blankText = blankText;
    return this;
  }

  /**
   * Blank placeholder / label.
   * @return blankText
   */
  
  @JsonProperty("blankText")
  public @Nullable Optional<String> getBlankText() {
    return blankText;
  }

  @JsonProperty("blankText")
  public void setBlankText(@Nullable Optional<String> blankText) {
    this.blankText = blankText;
  }

  public QuestionBlankUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
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
    QuestionBlankUpdate questionBlankUpdate = (QuestionBlankUpdate) o;
    return Objects.equals(this.blankText, questionBlankUpdate.blankText) &&
        Objects.equals(this.sortOrder, questionBlankUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blankText, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionBlankUpdate {\n");
    sb.append("    blankText: ").append(toIndentedString(blankText)).append("\n");
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

