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
 * Fields of RubricLevel that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RubricLevelUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> scorePercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public RubricLevelUpdate label(@Nullable Optional<String> label) {
    this.label = label;
    return this;
  }

  /**
   * Level label (Excellent, Good...).
   * @return label
   */
  
  @JsonProperty("label")
  public @Nullable Optional<String> getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable Optional<String> label) {
    this.label = label;
  }

  public RubricLevelUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Level description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public RubricLevelUpdate scorePercent(@Nullable Optional<String> scorePercent) {
    this.scorePercent = scorePercent;
    return this;
  }

  /**
   * Share of the criterion weight earned at this level.
   * @return scorePercent
   */
  
  @JsonProperty("scorePercent")
  public @Nullable Optional<String> getScorePercent() {
    return scorePercent;
  }

  @JsonProperty("scorePercent")
  public void setScorePercent(@Nullable Optional<String> scorePercent) {
    this.scorePercent = scorePercent;
  }

  public RubricLevelUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
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
    RubricLevelUpdate rubricLevelUpdate = (RubricLevelUpdate) o;
    return Objects.equals(this.label, rubricLevelUpdate.label) &&
        Objects.equals(this.description, rubricLevelUpdate.description) &&
        Objects.equals(this.scorePercent, rubricLevelUpdate.scorePercent) &&
        Objects.equals(this.sortOrder, rubricLevelUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, description, scorePercent, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RubricLevelUpdate {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    scorePercent: ").append(toIndentedString(scorePercent)).append("\n");
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

