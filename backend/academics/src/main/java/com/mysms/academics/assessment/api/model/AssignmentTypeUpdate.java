// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AssignmentType that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AssignmentTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  /**
   * How it is graded.
   */
  public enum GradingBasisEnum {
    SUBMISSION("submission"),
    
    ATTENDANCE("attendance");

    private final String value;

    GradingBasisEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static GradingBasisEnum fromValue(String value) {
      for (GradingBasisEnum b : GradingBasisEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull GradingBasisEnum> gradingBasis;

  public AssignmentTypeUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

  public AssignmentTypeUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether offered for new picks.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public AssignmentTypeUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Type code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public AssignmentTypeUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Type name.
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

  public AssignmentTypeUpdate gradingBasis(@Nullable Optional<GradingBasisEnum> gradingBasis) {
    this.gradingBasis = gradingBasis;
    return this;
  }

  /**
   * How it is graded.
   * @return gradingBasis
   */
  
  @JsonProperty("gradingBasis")
  public @Nullable Optional<GradingBasisEnum> getGradingBasis() {
    return gradingBasis;
  }

  @JsonProperty("gradingBasis")
  public void setGradingBasis(@Nullable Optional<GradingBasisEnum> gradingBasis) {
    this.gradingBasis = gradingBasis;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignmentTypeUpdate assignmentTypeUpdate = (AssignmentTypeUpdate) o;
    return Objects.equals(this.sortOrder, assignmentTypeUpdate.sortOrder) &&
        Objects.equals(this.isActive, assignmentTypeUpdate.isActive) &&
        Objects.equals(this.code, assignmentTypeUpdate.code) &&
        Objects.equals(this.name, assignmentTypeUpdate.name) &&
        Objects.equals(this.gradingBasis, assignmentTypeUpdate.gradingBasis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortOrder, isActive, code, name, gradingBasis);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignmentTypeUpdate {\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    gradingBasis: ").append(toIndentedString(gradingBasis)).append("\n");
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

