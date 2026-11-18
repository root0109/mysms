// Owned API contract of attendance.
package com.mysms.attendance.api.model;

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
 * Fields of AttendanceType that can be updated (partial update; see updateMask).
 */

public class AttendanceTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean countsPresent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean countsAbsent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean countsExcused;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean countsLate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public AttendanceTypeUpdate code(@Nullable String code) {
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

  public AttendanceTypeUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public AttendanceTypeUpdate institutionId(@Nullable UUID institutionId) {
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

  public AttendanceTypeUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Mark name.
   * @return name
   */
  @Size(max = 60) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AttendanceTypeUpdate countsPresent(@Nullable Boolean countsPresent) {
    this.countsPresent = countsPresent;
    return this;
  }

  /**
   * Counts as present.
   * @return countsPresent
   */
  
  @JsonProperty("countsPresent")
  public @Nullable Boolean getCountsPresent() {
    return countsPresent;
  }

  @JsonProperty("countsPresent")
  public void setCountsPresent(@Nullable Boolean countsPresent) {
    this.countsPresent = countsPresent;
  }

  public AttendanceTypeUpdate countsAbsent(@Nullable Boolean countsAbsent) {
    this.countsAbsent = countsAbsent;
    return this;
  }

  /**
   * Counts as absent.
   * @return countsAbsent
   */
  
  @JsonProperty("countsAbsent")
  public @Nullable Boolean getCountsAbsent() {
    return countsAbsent;
  }

  @JsonProperty("countsAbsent")
  public void setCountsAbsent(@Nullable Boolean countsAbsent) {
    this.countsAbsent = countsAbsent;
  }

  public AttendanceTypeUpdate countsExcused(@Nullable Boolean countsExcused) {
    this.countsExcused = countsExcused;
    return this;
  }

  /**
   * Counts as excused.
   * @return countsExcused
   */
  
  @JsonProperty("countsExcused")
  public @Nullable Boolean getCountsExcused() {
    return countsExcused;
  }

  @JsonProperty("countsExcused")
  public void setCountsExcused(@Nullable Boolean countsExcused) {
    this.countsExcused = countsExcused;
  }

  public AttendanceTypeUpdate countsLate(@Nullable Boolean countsLate) {
    this.countsLate = countsLate;
    return this;
  }

  /**
   * Counts as late.
   * @return countsLate
   */
  
  @JsonProperty("countsLate")
  public @Nullable Boolean getCountsLate() {
    return countsLate;
  }

  @JsonProperty("countsLate")
  public void setCountsLate(@Nullable Boolean countsLate) {
    this.countsLate = countsLate;
  }

  public AttendanceTypeUpdate isActive(@Nullable Boolean isActive) {
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
    AttendanceTypeUpdate attendanceTypeUpdate = (AttendanceTypeUpdate) o;
    return Objects.equals(this.code, attendanceTypeUpdate.code) &&
        Objects.equals(this.sortOrder, attendanceTypeUpdate.sortOrder) &&
        Objects.equals(this.institutionId, attendanceTypeUpdate.institutionId) &&
        Objects.equals(this.name, attendanceTypeUpdate.name) &&
        Objects.equals(this.countsPresent, attendanceTypeUpdate.countsPresent) &&
        Objects.equals(this.countsAbsent, attendanceTypeUpdate.countsAbsent) &&
        Objects.equals(this.countsExcused, attendanceTypeUpdate.countsExcused) &&
        Objects.equals(this.countsLate, attendanceTypeUpdate.countsLate) &&
        Objects.equals(this.isActive, attendanceTypeUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, sortOrder, institutionId, name, countsPresent, countsAbsent, countsExcused, countsLate, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceTypeUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    countsPresent: ").append(toIndentedString(countsPresent)).append("\n");
    sb.append("    countsAbsent: ").append(toIndentedString(countsAbsent)).append("\n");
    sb.append("    countsExcused: ").append(toIndentedString(countsExcused)).append("\n");
    sb.append("    countsLate: ").append(toIndentedString(countsLate)).append("\n");
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

