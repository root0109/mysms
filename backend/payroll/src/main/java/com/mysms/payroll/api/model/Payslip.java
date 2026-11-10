// Owned API contract of payroll (JAVA-CODING-RULES §7); first materialized from api/openapi/payroll.yaml on 2026-11-10.
package com.mysms.payroll.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Payroll run result for one staff member and pay period.
 */

public class Payslip {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String grossAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String deductionTotal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String netAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID documentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String payslipNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID staffSalaryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate periodStart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate periodEnd;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String payPercentage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String leaveDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer lateCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String leaveDeduction;

  /**
   * Payment state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    UNPAID("unpaid"),
    
    PAID("paid"),
    
    CANCELLED("cancelled");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Payslip() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Payslip(UUID institutionId, String payslipNumber, UUID staffId, UUID staffSalaryId, LocalDate periodStart, LocalDate periodEnd) {
    this.institutionId = institutionId;
    this.payslipNumber = payslipNumber;
    this.staffId = staffId;
    this.staffSalaryId = staffSalaryId;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
  }

  public Payslip grossAmount(@Nullable String grossAmount) {
    this.grossAmount = grossAmount;
    return this;
  }

  /**
   * Gross for the period.
   * @return grossAmount
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("grossAmount")
  public @Nullable String getGrossAmount() {
    return grossAmount;
  }

  @JsonProperty("grossAmount")
  public void setGrossAmount(@Nullable String grossAmount) {
    this.grossAmount = grossAmount;
  }

  public Payslip deductionTotal(@Nullable String deductionTotal) {
    this.deductionTotal = deductionTotal;
    return this;
  }

  /**
   * Total deductions.
   * @return deductionTotal
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("deductionTotal")
  public @Nullable String getDeductionTotal() {
    return deductionTotal;
  }

  @JsonProperty("deductionTotal")
  public void setDeductionTotal(@Nullable String deductionTotal) {
    this.deductionTotal = deductionTotal;
  }

  public Payslip netAmount(@Nullable String netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  /**
   * Net payable.
   * @return netAmount
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("netAmount")
  public @Nullable String getNetAmount() {
    return netAmount;
  }

  @JsonProperty("netAmount")
  public void setNetAmount(@Nullable String netAmount) {
    this.netAmount = netAmount;
  }

  public Payslip documentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
    return this;
  }

  /**
   * Rendered PDF of the payslip.
   * @return documentFileId
   */
  @Valid 
  @JsonProperty("documentFileId")
  public @Nullable UUID getDocumentFileId() {
    return documentFileId;
  }

  @JsonProperty("documentFileId")
  public void setDocumentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
  }

  public Payslip id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public Payslip institutionId(UUID institutionId) {
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

  public Payslip payslipNumber(String payslipNumber) {
    this.payslipNumber = payslipNumber;
    return this;
  }

  /**
   * Payslip number.
   * @return payslipNumber
   */
  @NotNull @Size(max = 40) 
  @JsonProperty("payslipNumber")
  public String getPayslipNumber() {
    return payslipNumber;
  }

  @JsonProperty("payslipNumber")
  public void setPayslipNumber(String payslipNumber) {
    this.payslipNumber = payslipNumber;
  }

  public Payslip staffId(UUID staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member.
   * @return staffId
   */
  @NotNull @Valid 
  @JsonProperty("staffId")
  public UUID getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(UUID staffId) {
    this.staffId = staffId;
  }

  public Payslip staffSalaryId(UUID staffSalaryId) {
    this.staffSalaryId = staffSalaryId;
    return this;
  }

  /**
   * Salary structure applied.
   * @return staffSalaryId
   */
  @NotNull @Valid 
  @JsonProperty("staffSalaryId")
  public UUID getStaffSalaryId() {
    return staffSalaryId;
  }

  @JsonProperty("staffSalaryId")
  public void setStaffSalaryId(UUID staffSalaryId) {
    this.staffSalaryId = staffSalaryId;
  }

  public Payslip periodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
    return this;
  }

  /**
   * Pay period start.
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

  public Payslip periodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
    return this;
  }

  /**
   * Pay period end.
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

  public Payslip payPercentage(@Nullable String payPercentage) {
    this.payPercentage = payPercentage;
    return this;
  }

  /**
   * Percentage of salary payable.
   * @return payPercentage
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("payPercentage")
  public @Nullable String getPayPercentage() {
    return payPercentage;
  }

  @JsonProperty("payPercentage")
  public void setPayPercentage(@Nullable String payPercentage) {
    this.payPercentage = payPercentage;
  }

  public Payslip leaveDays(@Nullable String leaveDays) {
    this.leaveDays = leaveDays;
    return this;
  }

  /**
   * Leave days in the period.
   * @return leaveDays
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("leaveDays")
  public @Nullable String getLeaveDays() {
    return leaveDays;
  }

  @JsonProperty("leaveDays")
  public void setLeaveDays(@Nullable String leaveDays) {
    this.leaveDays = leaveDays;
  }

  public Payslip lateCount(@Nullable Integer lateCount) {
    this.lateCount = lateCount;
    return this;
  }

  /**
   * Late arrivals in the period.
   * @return lateCount
   */
  
  @JsonProperty("lateCount")
  public @Nullable Integer getLateCount() {
    return lateCount;
  }

  @JsonProperty("lateCount")
  public void setLateCount(@Nullable Integer lateCount) {
    this.lateCount = lateCount;
  }

  public Payslip leaveDeduction(@Nullable String leaveDeduction) {
    this.leaveDeduction = leaveDeduction;
    return this;
  }

  /**
   * Deduction for unpaid leave.
   * @return leaveDeduction
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("leaveDeduction")
  public @Nullable String getLeaveDeduction() {
    return leaveDeduction;
  }

  @JsonProperty("leaveDeduction")
  public void setLeaveDeduction(@Nullable String leaveDeduction) {
    this.leaveDeduction = leaveDeduction;
  }

  public Payslip status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Payment state. Changed only through the workflow operations.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public Payslip remarks(@Nullable String remarks) {
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

  public Payslip createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Payslip createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Payslip updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Payslip updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Payslip etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Payslip payslip = (Payslip) o;
    return Objects.equals(this.grossAmount, payslip.grossAmount) &&
        Objects.equals(this.deductionTotal, payslip.deductionTotal) &&
        Objects.equals(this.netAmount, payslip.netAmount) &&
        Objects.equals(this.documentFileId, payslip.documentFileId) &&
        Objects.equals(this.id, payslip.id) &&
        Objects.equals(this.institutionId, payslip.institutionId) &&
        Objects.equals(this.payslipNumber, payslip.payslipNumber) &&
        Objects.equals(this.staffId, payslip.staffId) &&
        Objects.equals(this.staffSalaryId, payslip.staffSalaryId) &&
        Objects.equals(this.periodStart, payslip.periodStart) &&
        Objects.equals(this.periodEnd, payslip.periodEnd) &&
        Objects.equals(this.payPercentage, payslip.payPercentage) &&
        Objects.equals(this.leaveDays, payslip.leaveDays) &&
        Objects.equals(this.lateCount, payslip.lateCount) &&
        Objects.equals(this.leaveDeduction, payslip.leaveDeduction) &&
        Objects.equals(this.status, payslip.status) &&
        Objects.equals(this.remarks, payslip.remarks) &&
        Objects.equals(this.createTime, payslip.createTime) &&
        Objects.equals(this.createdBy, payslip.createdBy) &&
        Objects.equals(this.updateTime, payslip.updateTime) &&
        Objects.equals(this.updatedBy, payslip.updatedBy) &&
        Objects.equals(this.etag, payslip.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grossAmount, deductionTotal, netAmount, documentFileId, id, institutionId, payslipNumber, staffId, staffSalaryId, periodStart, periodEnd, payPercentage, leaveDays, lateCount, leaveDeduction, status, remarks, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Payslip {\n");
    sb.append("    grossAmount: ").append(toIndentedString(grossAmount)).append("\n");
    sb.append("    deductionTotal: ").append(toIndentedString(deductionTotal)).append("\n");
    sb.append("    netAmount: ").append(toIndentedString(netAmount)).append("\n");
    sb.append("    documentFileId: ").append(toIndentedString(documentFileId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    payslipNumber: ").append(toIndentedString(payslipNumber)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    staffSalaryId: ").append(toIndentedString(staffSalaryId)).append("\n");
    sb.append("    periodStart: ").append(toIndentedString(periodStart)).append("\n");
    sb.append("    periodEnd: ").append(toIndentedString(periodEnd)).append("\n");
    sb.append("    payPercentage: ").append(toIndentedString(payPercentage)).append("\n");
    sb.append("    leaveDays: ").append(toIndentedString(leaveDays)).append("\n");
    sb.append("    lateCount: ").append(toIndentedString(lateCount)).append("\n");
    sb.append("    leaveDeduction: ").append(toIndentedString(leaveDeduction)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

