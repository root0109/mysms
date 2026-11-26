// Owned API contract of hr.
package com.mysms.hrms.hr.api.model;

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
 * Fields of LeaveType that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LeaveTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> maxDaysPerYear;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public LeaveTypeUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Stable key used by imports, integrations and seed scripts.
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

  public LeaveTypeUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

  public LeaveTypeUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public LeaveTypeUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year the entitlement applies to (NULL = every year).
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public LeaveTypeUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Leave type.
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

  public LeaveTypeUpdate maxDaysPerYear(@Nullable Optional<String> maxDaysPerYear) {
    this.maxDaysPerYear = maxDaysPerYear;
    return this;
  }

  /**
   * Entitlement in days.
   * @return maxDaysPerYear
   */
  
  @JsonProperty("maxDaysPerYear")
  public @Nullable Optional<String> getMaxDaysPerYear() {
    return maxDaysPerYear;
  }

  @JsonProperty("maxDaysPerYear")
  public void setMaxDaysPerYear(@Nullable Optional<String> maxDaysPerYear) {
    this.maxDaysPerYear = maxDaysPerYear;
  }

  public LeaveTypeUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
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

