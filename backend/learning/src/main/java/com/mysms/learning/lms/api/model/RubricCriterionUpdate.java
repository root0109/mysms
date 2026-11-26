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
 * Fields of RubricCriterion that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RubricCriterionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> groupName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> weightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> groupWeightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public RubricCriterionUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Criterion.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public RubricCriterionUpdate groupName(@Nullable Optional<String> groupName) {
    this.groupName = groupName;
    return this;
  }

  /**
   * Criteria group.
   * @return groupName
   */
  
  @JsonProperty("groupName")
  public @Nullable Optional<String> getGroupName() {
    return groupName;
  }

  @JsonProperty("groupName")
  public void setGroupName(@Nullable Optional<String> groupName) {
    this.groupName = groupName;
  }

  public RubricCriterionUpdate weightPercent(@Nullable Optional<String> weightPercent) {
    this.weightPercent = weightPercent;
    return this;
  }

  /**
   * Weight of the criterion.
   * @return weightPercent
   */
  
  @JsonProperty("weightPercent")
  public @Nullable Optional<String> getWeightPercent() {
    return weightPercent;
  }

  @JsonProperty("weightPercent")
  public void setWeightPercent(@Nullable Optional<String> weightPercent) {
    this.weightPercent = weightPercent;
  }

  public RubricCriterionUpdate groupWeightPercent(@Nullable Optional<String> groupWeightPercent) {
    this.groupWeightPercent = groupWeightPercent;
    return this;
  }

  /**
   * Total weight of the criterion's group.
   * @return groupWeightPercent
   */
  
  @JsonProperty("groupWeightPercent")
  public @Nullable Optional<String> getGroupWeightPercent() {
    return groupWeightPercent;
  }

  @JsonProperty("groupWeightPercent")
  public void setGroupWeightPercent(@Nullable Optional<String> groupWeightPercent) {
    this.groupWeightPercent = groupWeightPercent;
  }

  public RubricCriterionUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

