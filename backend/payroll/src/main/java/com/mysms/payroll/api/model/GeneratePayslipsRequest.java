// Owned API contract of payroll (JAVA-CODING-RULES §7); first materialized from api/openapi/payroll.yaml on 2026-11-10.
package com.mysms.payroll.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GeneratePayslipsRequest
 */

public class GeneratePayslipsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate periodStart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate periodEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> staffIds = new ArrayList<>();

  public GeneratePayslipsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GeneratePayslipsRequest(UUID institutionId, LocalDate periodStart, LocalDate periodEnd) {
    this.institutionId = institutionId;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }

  public GeneratePayslipsRequest institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public GeneratePayslipsRequest periodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
    return this;
  }

  /**
   * Start.
   * @return periodStart
   */
  @NotNull @Valid 
  @JsonProperty("periodStart")
  public LocalDate getPeriodStart() {
    return periodStart;
  }

  @JsonProperty("periodStart")
  public void setPeriodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
  }

  public GeneratePayslipsRequest periodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
    return this;
  }

  /**
   * End.
   * @return periodEnd
   */
  @NotNull @Valid 
  @JsonProperty("periodEnd")
  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  @JsonProperty("periodEnd")
  public void setPeriodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
  }

  public GeneratePayslipsRequest staffIds(List<UUID> staffIds) {
    this.staffIds = staffIds;
    return this;
  }

  public GeneratePayslipsRequest addStaffIdsItem(UUID staffIdsItem) {
    if (this.staffIds == null) {
      this.staffIds = new ArrayList<>();
    }
    this.staffIds.add(staffIdsItem);
    return this;
  }

  /**
   * Subset (default all with a salary).
   * @return staffIds
   */
  
  @JsonProperty("staffIds")
  public List<UUID> getStaffIds() {
    return staffIds;
  }

  @JsonProperty("staffIds")
  public void setStaffIds(List<UUID> staffIds) {
    this.staffIds = staffIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GeneratePayslipsRequest generatePayslipsRequest = (GeneratePayslipsRequest) o;
    return Objects.equals(this.institutionId, generatePayslipsRequest.institutionId) &&
        Objects.equals(this.periodStart, generatePayslipsRequest.periodStart) &&
        Objects.equals(this.periodEnd, generatePayslipsRequest.periodEnd) &&
        Objects.equals(this.staffIds, generatePayslipsRequest.staffIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, periodStart, periodEnd, staffIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GeneratePayslipsRequest {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    periodStart: ").append(toIndentedString(periodStart)).append("\n");
    sb.append("    periodEnd: ").append(toIndentedString(periodEnd)).append("\n");
    sb.append("    staffIds: ").append(toIndentedString(staffIds)).append("\n");
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

