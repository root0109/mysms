// Owned API contract of payroll.
package com.mysms.payroll.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of StaffSalary that can be updated (partial update; see updateMask).
 */

public class StaffSalaryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID salaryTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String grossSalary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate effectiveDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remarks;

  public StaffSalaryUpdate staffId(@Nullable UUID staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member.
   * @return staffId
   */
  @Valid 
  @JsonProperty("staffId")
  public @Nullable UUID getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable UUID staffId) {
    this.staffId = staffId;
  }

  public StaffSalaryUpdate salaryTemplateId(@Nullable UUID salaryTemplateId) {
    this.salaryTemplateId = salaryTemplateId;
    return this;
  }

  /**
   * Template used.
   * @return salaryTemplateId
   */
  @Valid 
  @JsonProperty("salaryTemplateId")
  public @Nullable UUID getSalaryTemplateId() {
    return salaryTemplateId;
  }

  @JsonProperty("salaryTemplateId")
  public void setSalaryTemplateId(@Nullable UUID salaryTemplateId) {
    this.salaryTemplateId = salaryTemplateId;
  }

  public StaffSalaryUpdate grossSalary(@Nullable String grossSalary) {
    this.grossSalary = grossSalary;
    return this;
  }

  /**
   * Gross monthly salary.
   * @return grossSalary
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("grossSalary")
  public @Nullable String getGrossSalary() {
    return grossSalary;
  }

  @JsonProperty("grossSalary")
  public void setGrossSalary(@Nullable String grossSalary) {
    this.grossSalary = grossSalary;
  }

  public StaffSalaryUpdate effectiveDate(@Nullable LocalDate effectiveDate) {
    this.effectiveDate = effectiveDate;
    return this;
  }

  /**
   * Effective from.
   * @return effectiveDate
   */
  @Valid 
  @JsonProperty("effectiveDate")
  public @Nullable LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  @JsonProperty("effectiveDate")
  public void setEffectiveDate(@Nullable LocalDate effectiveDate) {
    this.effectiveDate = effectiveDate;
  }

  public StaffSalaryUpdate remarks(@Nullable String remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Comments.
   * @return remarks
   */
  
  @JsonProperty("remarks")
  public @Nullable String getRemarks() {
    return remarks;
  }

  @JsonProperty("remarks")
  public void setRemarks(@Nullable String remarks) {
    this.remarks = remarks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffSalaryUpdate staffSalaryUpdate = (StaffSalaryUpdate) o;
    return Objects.equals(this.staffId, staffSalaryUpdate.staffId) &&
        Objects.equals(this.salaryTemplateId, staffSalaryUpdate.salaryTemplateId) &&
        Objects.equals(this.grossSalary, staffSalaryUpdate.grossSalary) &&
        Objects.equals(this.effectiveDate, staffSalaryUpdate.effectiveDate) &&
        Objects.equals(this.remarks, staffSalaryUpdate.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffId, salaryTemplateId, grossSalary, effectiveDate, remarks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffSalaryUpdate {\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    salaryTemplateId: ").append(toIndentedString(salaryTemplateId)).append("\n");
    sb.append("    grossSalary: ").append(toIndentedString(grossSalary)).append("\n");
    sb.append("    effectiveDate: ").append(toIndentedString(effectiveDate)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
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

