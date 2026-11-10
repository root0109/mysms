// Owned API contract of hr (JAVA-CODING-RULES §7); first materialized from api/openapi/hr.yaml on 2026-11-10.
package com.mysms.hr.api.model;

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
 * Fields of Designation that can be updated (partial update; see updateMask).
 */

public class DesignationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

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
  private @Nullable StaffCategoryEnum staffCategory;

  public DesignationUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public DesignationUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether offered for new picks.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public DesignationUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Designation code.
   * @return code
   */
  @Size(max = 60) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public DesignationUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Designation name.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public DesignationUpdate staffCategory(@Nullable StaffCategoryEnum staffCategory) {
    this.staffCategory = staffCategory;
    return this;
  }

  /**
   * Teaching or non-teaching.
   * @return staffCategory
   */
  
  @JsonProperty("staffCategory")
  public @Nullable StaffCategoryEnum getStaffCategory() {
    return staffCategory;
  }

  @JsonProperty("staffCategory")
  public void setStaffCategory(@Nullable StaffCategoryEnum staffCategory) {
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

