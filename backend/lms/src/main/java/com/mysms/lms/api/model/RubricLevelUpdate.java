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
 * Fields of RubricLevel that can be updated (partial update; see updateMask).
 */

public class RubricLevelUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scorePercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public RubricLevelUpdate label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Level label (Excellent, Good...).
   * @return label
   */
  @Size(max = 100) 
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public RubricLevelUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Level description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public RubricLevelUpdate scorePercent(@Nullable String scorePercent) {
    this.scorePercent = scorePercent;
    return this;
  }

  /**
   * Share of the criterion weight earned at this level.
   * @return scorePercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("scorePercent")
  public @Nullable String getScorePercent() {
    return scorePercent;
  }

  @JsonProperty("scorePercent")
  public void setScorePercent(@Nullable String scorePercent) {
    this.scorePercent = scorePercent;
  }

  public RubricLevelUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
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

