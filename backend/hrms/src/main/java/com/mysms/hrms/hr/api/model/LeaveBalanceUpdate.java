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
 * Fields of LeaveBalance that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LeaveBalanceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> leaveTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> entitledDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> carriedForwardDays;

  public LeaveBalanceUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public LeaveBalanceUpdate leaveTypeId(@Nullable Optional<UUID> leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
    return this;
  }

  /**
   * Leave type.
   * @return leaveTypeId
   */
  
  @JsonProperty("leaveTypeId")
  public @Nullable Optional<UUID> getLeaveTypeId() {
    return leaveTypeId;
  }

  @JsonProperty("leaveTypeId")
  public void setLeaveTypeId(@Nullable Optional<UUID> leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
  }

  public LeaveBalanceUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year the balance is tracked for.
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

  public LeaveBalanceUpdate entitledDays(@Nullable Optional<String> entitledDays) {
    this.entitledDays = entitledDays;
    return this;
  }

  /**
   * Days granted for the year.
   * @return entitledDays
   */
  
  @JsonProperty("entitledDays")
  public @Nullable Optional<String> getEntitledDays() {
    return entitledDays;
  }

  @JsonProperty("entitledDays")
  public void setEntitledDays(@Nullable Optional<String> entitledDays) {
    this.entitledDays = entitledDays;
  }

  public LeaveBalanceUpdate carriedForwardDays(@Nullable Optional<String> carriedForwardDays) {
    this.carriedForwardDays = carriedForwardDays;
    return this;
  }

  /**
   * Days carried in from the previous year.
   * @return carriedForwardDays
   */
  
  @JsonProperty("carriedForwardDays")
  public @Nullable Optional<String> getCarriedForwardDays() {
    return carriedForwardDays;
  }

  @JsonProperty("carriedForwardDays")
  public void setCarriedForwardDays(@Nullable Optional<String> carriedForwardDays) {
    this.carriedForwardDays = carriedForwardDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeaveBalanceUpdate leaveBalanceUpdate = (LeaveBalanceUpdate) o;
    return Objects.equals(this.staffId, leaveBalanceUpdate.staffId) &&
        Objects.equals(this.leaveTypeId, leaveBalanceUpdate.leaveTypeId) &&
        Objects.equals(this.academicYearId, leaveBalanceUpdate.academicYearId) &&
        Objects.equals(this.entitledDays, leaveBalanceUpdate.entitledDays) &&
        Objects.equals(this.carriedForwardDays, leaveBalanceUpdate.carriedForwardDays);
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffId, leaveTypeId, academicYearId, entitledDays, carriedForwardDays);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LeaveBalanceUpdate {\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    leaveTypeId: ").append(toIndentedString(leaveTypeId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    entitledDays: ").append(toIndentedString(entitledDays)).append("\n");
    sb.append("    carriedForwardDays: ").append(toIndentedString(carriedForwardDays)).append("\n");
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

