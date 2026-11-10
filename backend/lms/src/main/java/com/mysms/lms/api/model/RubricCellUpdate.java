// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * Fields of RubricCell that can be updated (partial update; see updateMask).
 */

public class RubricCellUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID rubricCriterionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID rubricLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public RubricCellUpdate rubricCriterionId(@Nullable UUID rubricCriterionId) {
    this.rubricCriterionId = rubricCriterionId;
    return this;
  }

  /**
   * Criterion.
   * @return rubricCriterionId
   */
  @Valid 
  @JsonProperty("rubricCriterionId")
  public @Nullable UUID getRubricCriterionId() {
    return rubricCriterionId;
  }

  @JsonProperty("rubricCriterionId")
  public void setRubricCriterionId(@Nullable UUID rubricCriterionId) {
    this.rubricCriterionId = rubricCriterionId;
  }

  public RubricCellUpdate rubricLevelId(@Nullable UUID rubricLevelId) {
    this.rubricLevelId = rubricLevelId;
    return this;
  }

  /**
   * Level.
   * @return rubricLevelId
   */
  @Valid 
  @JsonProperty("rubricLevelId")
  public @Nullable UUID getRubricLevelId() {
    return rubricLevelId;
  }

  @JsonProperty("rubricLevelId")
  public void setRubricLevelId(@Nullable UUID rubricLevelId) {
    this.rubricLevelId = rubricLevelId;
  }

  public RubricCellUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Descriptor.
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

  public RubricCellUpdate sortOrder(@Nullable Integer sortOrder) {
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
    RubricCellUpdate rubricCellUpdate = (RubricCellUpdate) o;
    return Objects.equals(this.rubricCriterionId, rubricCellUpdate.rubricCriterionId) &&
        Objects.equals(this.rubricLevelId, rubricCellUpdate.rubricLevelId) &&
        Objects.equals(this.description, rubricCellUpdate.description) &&
        Objects.equals(this.sortOrder, rubricCellUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rubricCriterionId, rubricLevelId, description, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RubricCellUpdate {\n");
    sb.append("    rubricCriterionId: ").append(toIndentedString(rubricCriterionId)).append("\n");
    sb.append("    rubricLevelId: ").append(toIndentedString(rubricLevelId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

