// Owned API contract of hr.
package com.mysms.hrms.hr.api.model;

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
 * Fields of Designation that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class DesignationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  /**
   * Teaching or non-teaching.
   */
  public enum StaffCategoryEnum {
    TEACHING("teaching"),
    
    NON_TEACHING("non_teaching");

    private final String value;

    StaffCategoryEnum(String value) {
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
    public static StaffCategoryEnum fromValue(String value) {
      for (StaffCategoryEnum b : StaffCategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull StaffCategoryEnum> staffCategory;

  public DesignationUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

  public DesignationUpdate isActive(@Nullable Optional<Boolean> isActive) {
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

  public DesignationUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Designation code.
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

  public DesignationUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Designation name.
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

  public DesignationUpdate staffCategory(@Nullable Optional<StaffCategoryEnum> staffCategory) {
    this.staffCategory = staffCategory;
    return this;
  }

  /**
   * Teaching or non-teaching.
   * @return staffCategory
   */
  
  @JsonProperty("staffCategory")
  public @Nullable Optional<StaffCategoryEnum> getStaffCategory() {
    return staffCategory;
  }

  @JsonProperty("staffCategory")
  public void setStaffCategory(@Nullable Optional<StaffCategoryEnum> staffCategory) {
    this.staffCategory = staffCategory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DesignationUpdate designationUpdate = (DesignationUpdate) o;
    return Objects.equals(this.sortOrder, designationUpdate.sortOrder) &&
        Objects.equals(this.isActive, designationUpdate.isActive) &&
        Objects.equals(this.code, designationUpdate.code) &&
        Objects.equals(this.name, designationUpdate.name) &&
        Objects.equals(this.staffCategory, designationUpdate.staffCategory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortOrder, isActive, code, name, staffCategory);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DesignationUpdate {\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    staffCategory: ").append(toIndentedString(staffCategory)).append("\n");
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

