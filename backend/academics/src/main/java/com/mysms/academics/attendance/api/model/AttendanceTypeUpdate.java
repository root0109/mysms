// Owned API contract of attendance.
package com.mysms.academics.attendance.api.model;

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
 * Fields of AttendanceType that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AttendanceTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> countsPresent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> countsAbsent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> countsExcused;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> countsLate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public AttendanceTypeUpdate code(@Nullable Optional<String> code) {
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

  public AttendanceTypeUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

  public AttendanceTypeUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public AttendanceTypeUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Mark name.
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

  public AttendanceTypeUpdate countsPresent(@Nullable Optional<Boolean> countsPresent) {
    this.countsPresent = countsPresent;
    return this;
  }

  /**
   * Counts as present.
   * @return countsPresent
   */
  
  @JsonProperty("countsPresent")
  public @Nullable Optional<Boolean> getCountsPresent() {
    return countsPresent;
  }

  @JsonProperty("countsPresent")
  public void setCountsPresent(@Nullable Optional<Boolean> countsPresent) {
    this.countsPresent = countsPresent;
  }

  public AttendanceTypeUpdate countsAbsent(@Nullable Optional<Boolean> countsAbsent) {
    this.countsAbsent = countsAbsent;
    return this;
  }

  /**
   * Counts as absent.
   * @return countsAbsent
   */
  
  @JsonProperty("countsAbsent")
  public @Nullable Optional<Boolean> getCountsAbsent() {
    return countsAbsent;
  }

  @JsonProperty("countsAbsent")
  public void setCountsAbsent(@Nullable Optional<Boolean> countsAbsent) {
    this.countsAbsent = countsAbsent;
  }

  public AttendanceTypeUpdate countsExcused(@Nullable Optional<Boolean> countsExcused) {
    this.countsExcused = countsExcused;
    return this;
  }

  /**
   * Counts as excused.
   * @return countsExcused
   */
  
  @JsonProperty("countsExcused")
  public @Nullable Optional<Boolean> getCountsExcused() {
    return countsExcused;
  }

  @JsonProperty("countsExcused")
  public void setCountsExcused(@Nullable Optional<Boolean> countsExcused) {
    this.countsExcused = countsExcused;
  }

  public AttendanceTypeUpdate countsLate(@Nullable Optional<Boolean> countsLate) {
    this.countsLate = countsLate;
    return this;
  }

  /**
   * Counts as late.
   * @return countsLate
   */
  
  @JsonProperty("countsLate")
  public @Nullable Optional<Boolean> getCountsLate() {
    return countsLate;
  }

  @JsonProperty("countsLate")
  public void setCountsLate(@Nullable Optional<Boolean> countsLate) {
    this.countsLate = countsLate;
  }

  public AttendanceTypeUpdate isActive(@Nullable Optional<Boolean> isActive) {
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

