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
 * Fields of RubricCriterion that can be updated (partial update; see updateMask).
 */

public class RubricCriterionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String groupName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String weightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String groupWeightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public RubricCriterionUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Criterion.
   * @return name
   */
  @Size(max = 200) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public RubricCriterionUpdate groupName(@Nullable String groupName) {
    this.groupName = groupName;
    return this;
  }

  /**
   * Criteria group.
   * @return groupName
   */
  @Size(max = 200) 
  @JsonProperty("groupName")
  public @Nullable String getGroupName() {
    return groupName;
  }

  @JsonProperty("groupName")
  public void setGroupName(@Nullable String groupName) {
    this.groupName = groupName;
  }

  public RubricCriterionUpdate weightPercent(@Nullable String weightPercent) {
    this.weightPercent = weightPercent;
    return this;
  }

  /**
   * Weight of the criterion.
   * @return weightPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("weightPercent")
  public @Nullable String getWeightPercent() {
    return weightPercent;
  }

  @JsonProperty("weightPercent")
  public void setWeightPercent(@Nullable String weightPercent) {
    this.weightPercent = weightPercent;
  }

  public RubricCriterionUpdate groupWeightPercent(@Nullable String groupWeightPercent) {
    this.groupWeightPercent = groupWeightPercent;
    return this;
  }

  /**
   * Total weight of the criterion's group.
   * @return groupWeightPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("groupWeightPercent")
  public @Nullable String getGroupWeightPercent() {
    return groupWeightPercent;
  }

  @JsonProperty("groupWeightPercent")
  public void setGroupWeightPercent(@Nullable String groupWeightPercent) {
    this.groupWeightPercent = groupWeightPercent;
  }

  public RubricCriterionUpdate sortOrder(@Nullable Integer sortOrder) {
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
    RubricCriterionUpdate rubricCriterionUpdate = (RubricCriterionUpdate) o;
    return Objects.equals(this.name, rubricCriterionUpdate.name) &&
        Objects.equals(this.groupName, rubricCriterionUpdate.groupName) &&
        Objects.equals(this.weightPercent, rubricCriterionUpdate.weightPercent) &&
        Objects.equals(this.groupWeightPercent, rubricCriterionUpdate.groupWeightPercent) &&
        Objects.equals(this.sortOrder, rubricCriterionUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, groupName, weightPercent, groupWeightPercent, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RubricCriterionUpdate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
    sb.append("    weightPercent: ").append(toIndentedString(weightPercent)).append("\n");
    sb.append("    groupWeightPercent: ").append(toIndentedString(groupWeightPercent)).append("\n");
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

