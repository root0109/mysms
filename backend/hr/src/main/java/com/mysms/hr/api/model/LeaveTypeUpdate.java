// Owned API contract of hr.
package com.mysms.hr.api.model;

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
 * Fields of LeaveType that can be updated (partial update; see updateMask).
 */

public class LeaveTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxDaysPerYear;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public LeaveTypeUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Stable key used by imports, integrations and seed scripts.
   * @return code
   */
  @Size(max = 40) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public LeaveTypeUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public LeaveTypeUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public LeaveTypeUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year the entitlement applies to (NULL = every year).
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public LeaveTypeUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Leave type.
   * @return name
   */
  @Size(max = 100) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public LeaveTypeUpdate maxDaysPerYear(@Nullable String maxDaysPerYear) {
    this.maxDaysPerYear = maxDaysPerYear;
    return this;
  }

  /**
   * Entitlement in days.
   * @return maxDaysPerYear
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("maxDaysPerYear")
  public @Nullable String getMaxDaysPerYear() {
    return maxDaysPerYear;
  }

  @JsonProperty("maxDaysPerYear")
  public void setMaxDaysPerYear(@Nullable String maxDaysPerYear) {
    this.maxDaysPerYear = maxDaysPerYear;
  }

  public LeaveTypeUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeaveTypeUpdate leaveTypeUpdate = (LeaveTypeUpdate) o;
    return Objects.equals(this.code, leaveTypeUpdate.code) &&
        Objects.equals(this.sortOrder, leaveTypeUpdate.sortOrder) &&
        Objects.equals(this.institutionId, leaveTypeUpdate.institutionId) &&
        Objects.equals(this.academicYearId, leaveTypeUpdate.academicYearId) &&
        Objects.equals(this.name, leaveTypeUpdate.name) &&
        Objects.equals(this.maxDaysPerYear, leaveTypeUpdate.maxDaysPerYear) &&
        Objects.equals(this.isActive, leaveTypeUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, sortOrder, institutionId, academicYearId, name, maxDaysPerYear, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LeaveTypeUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    maxDaysPerYear: ").append(toIndentedString(maxDaysPerYear)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

