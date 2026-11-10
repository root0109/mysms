// Owned API contract of hr (JAVA-CODING-RULES §7); first materialized from api/openapi/hr.yaml on 2026-11-10.
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
 * Computed leave position per leave type (entitled, taken, pending, available); not the stored hr LeaveBalance resource.
 */

public class LeaveBalanceSummary {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID leaveTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String entitledDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String takenDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String pendingDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String availableDays;

  public LeaveBalanceSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LeaveBalanceSummary(UUID leaveTypeId, String entitledDays, String takenDays, String pendingDays, String availableDays) {
    this.leaveTypeId = leaveTypeId;
    this.entitledDays = entitledDays;
    this.takenDays = takenDays;
    this.pendingDays = pendingDays;
    this.availableDays = availableDays;
  }

  public LeaveBalanceSummary leaveTypeId(UUID leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
    return this;
  }

  /**
   * Leave type.
   * @return leaveTypeId
   */
  @NotNull @Valid 
  @JsonProperty("leaveTypeId")
  public UUID getLeaveTypeId() {
    return leaveTypeId;
  }

  @JsonProperty("leaveTypeId")
  public void setLeaveTypeId(UUID leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
  }

  public LeaveBalanceSummary entitledDays(String entitledDays) {
    this.entitledDays = entitledDays;
    return this;
  }

  /**
   * Entitlement.
   * @return entitledDays
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("entitledDays")
  public String getEntitledDays() {
    return entitledDays;
  }

  @JsonProperty("entitledDays")
  public void setEntitledDays(String entitledDays) {
    this.entitledDays = entitledDays;
  }

  public LeaveBalanceSummary takenDays(String takenDays) {
    this.takenDays = takenDays;
    return this;
  }

  /**
   * Approved days taken.
   * @return takenDays
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("takenDays")
  public String getTakenDays() {
    return takenDays;
  }

  @JsonProperty("takenDays")
  public void setTakenDays(String takenDays) {
    this.takenDays = takenDays;
  }

  public LeaveBalanceSummary pendingDays(String pendingDays) {
    this.pendingDays = pendingDays;
    return this;
  }

  /**
   * Days pending approval.
   * @return pendingDays
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("pendingDays")
  public String getPendingDays() {
    return pendingDays;
  }

  @JsonProperty("pendingDays")
  public void setPendingDays(String pendingDays) {
    this.pendingDays = pendingDays;
  }

  public LeaveBalanceSummary availableDays(String availableDays) {
    this.availableDays = availableDays;
    return this;
  }

  /**
   * Remaining.
   * @return availableDays
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("availableDays")
  public String getAvailableDays() {
    return availableDays;
  }

  @JsonProperty("availableDays")
  public void setAvailableDays(String availableDays) {
    this.availableDays = availableDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeaveBalanceSummary leaveBalanceSummary = (LeaveBalanceSummary) o;
    return Objects.equals(this.leaveTypeId, leaveBalanceSummary.leaveTypeId) &&
        Objects.equals(this.entitledDays, leaveBalanceSummary.entitledDays) &&
        Objects.equals(this.takenDays, leaveBalanceSummary.takenDays) &&
        Objects.equals(this.pendingDays, leaveBalanceSummary.pendingDays) &&
        Objects.equals(this.availableDays, leaveBalanceSummary.availableDays);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leaveTypeId, entitledDays, takenDays, pendingDays, availableDays);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LeaveBalanceSummary {\n");
    sb.append("    leaveTypeId: ").append(toIndentedString(leaveTypeId)).append("\n");
    sb.append("    entitledDays: ").append(toIndentedString(entitledDays)).append("\n");
    sb.append("    takenDays: ").append(toIndentedString(takenDays)).append("\n");
    sb.append("    pendingDays: ").append(toIndentedString(pendingDays)).append("\n");
    sb.append("    availableDays: ").append(toIndentedString(availableDays)).append("\n");
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

