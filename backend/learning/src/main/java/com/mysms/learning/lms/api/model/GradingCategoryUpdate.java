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
 * Fields of GradingCategory that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class GradingCategoryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> weightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> equalItemWeighting;

  public GradingCategoryUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course template (NULL with cohort NULL = tenant-wide category name).
   * @return courseId
   */
  
  @JsonProperty("courseId")
  public @Nullable Optional<UUID> getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
  }

  public GradingCategoryUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort copy.
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public GradingCategoryUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Category name.
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

  public GradingCategoryUpdate weightPercent(@Nullable Optional<String> weightPercent) {
    this.weightPercent = weightPercent;
    return this;
  }

  /**
   * Weight of the category in the final grade.
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

  public GradingCategoryUpdate equalItemWeighting(@Nullable Optional<Boolean> equalItemWeighting) {
    this.equalItemWeighting = equalItemWeighting;
    return this;
  }

  /**
   * Items inside the category count equally regardless of points.
   * @return equalItemWeighting
   */
  
  @JsonProperty("equalItemWeighting")
  public @Nullable Optional<Boolean> getEqualItemWeighting() {
    return equalItemWeighting;
  }

  @JsonProperty("equalItemWeighting")
  public void setEqualItemWeighting(@Nullable Optional<Boolean> equalItemWeighting) {
    this.equalItemWeighting = equalItemWeighting;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradingCategoryUpdate gradingCategoryUpdate = (GradingCategoryUpdate) o;
    return Objects.equals(this.courseId, gradingCategoryUpdate.courseId) &&
        Objects.equals(this.cohortId, gradingCategoryUpdate.cohortId) &&
        Objects.equals(this.name, gradingCategoryUpdate.name) &&
        Objects.equals(this.weightPercent, gradingCategoryUpdate.weightPercent) &&
        Objects.equals(this.equalItemWeighting, gradingCategoryUpdate.equalItemWeighting);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, cohortId, name, weightPercent, equalItemWeighting);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradingCategoryUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    weightPercent: ").append(toIndentedString(weightPercent)).append("\n");
    sb.append("    equalItemWeighting: ").append(toIndentedString(equalItemWeighting)).append("\n");
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

