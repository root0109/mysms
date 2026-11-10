// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

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
 * Fee amount due from a student for an installment / fee head.
 */

public class StudentFeeDue {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID feeStructureId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID concessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String discountAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String netAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programEnrollmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentEnrollmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID installmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID feeHeadId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String amount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String discountPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feeFactor;

  /**
   * State.
   */
  public enum StatusEnum {
    DRAFT("draft"),
    
    INVOICED("invoiced"),
    
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
  private @Nullable UUID invoiceId;

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

  public StudentFeeDue() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StudentFeeDue(UUID institutionId, UUID studentId, String amount, UUID currencyId) {
    this.institutionId = institutionId;
    this.studentId = studentId;
    this.amount = amount;
    this.currencyId = currencyId;
  }

  public StudentFeeDue academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year the due belongs to.
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

  public StudentFeeDue feeStructureId(@Nullable UUID feeStructureId) {
    this.feeStructureId = feeStructureId;
    return this;
  }

  /**
   * Fee structure that generated this due (absent for manual dues).
   * @return feeStructureId
   */
  @Valid 
  @JsonProperty("feeStructureId")
  public @Nullable UUID getFeeStructureId() {
    return feeStructureId;
  }

  @JsonProperty("feeStructureId")
  public void setFeeStructureId(@Nullable UUID feeStructureId) {
    this.feeStructureId = feeStructureId;
  }

  public StudentFeeDue concessionId(@Nullable UUID concessionId) {
    this.concessionId = concessionId;
    return this;
  }

  /**
   * Concession applied to this due.
   * @return concessionId
   */
  @Valid 
  @JsonProperty("concessionId")
  public @Nullable UUID getConcessionId() {
    return concessionId;
  }

  @JsonProperty("concessionId")
  public void setConcessionId(@Nullable UUID concessionId) {
    this.concessionId = concessionId;
  }

  public StudentFeeDue discountAmount(@Nullable String discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  /**
   * Concession/discount applied to this due.
   * @return discountAmount
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("discountAmount")
  public @Nullable String getDiscountAmount() {
    return discountAmount;
  }

  @JsonProperty("discountAmount")
  public void setDiscountAmount(@Nullable String discountAmount) {
    this.discountAmount = discountAmount;
  }

  public StudentFeeDue netAmount(@Nullable String netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  /**
   * Payable after discount (amount - discountAmount).
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

  public StudentFeeDue id(@Nullable UUID id) {
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

  public StudentFeeDue institutionId(UUID institutionId) {
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

  public StudentFeeDue studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public StudentFeeDue programEnrollmentId(@Nullable UUID programEnrollmentId) {
    this.programEnrollmentId = programEnrollmentId;
    return this;
  }

  /**
   * Course registration the fee belongs to.
   * @return programEnrollmentId
   */
  @Valid 
  @JsonProperty("programEnrollmentId")
  public @Nullable UUID getProgramEnrollmentId() {
    return programEnrollmentId;
  }

  @JsonProperty("programEnrollmentId")
  public void setProgramEnrollmentId(@Nullable UUID programEnrollmentId) {
    this.programEnrollmentId = programEnrollmentId;
  }

  public StudentFeeDue studentEnrollmentId(@Nullable UUID studentEnrollmentId) {
    this.studentEnrollmentId = studentEnrollmentId;
    return this;
  }

  /**
   * School-year placement the fee belongs to.
   * @return studentEnrollmentId
   */
  @Valid 
  @JsonProperty("studentEnrollmentId")
  public @Nullable UUID getStudentEnrollmentId() {
    return studentEnrollmentId;
  }

  @JsonProperty("studentEnrollmentId")
  public void setStudentEnrollmentId(@Nullable UUID studentEnrollmentId) {
    this.studentEnrollmentId = studentEnrollmentId;
  }

  public StudentFeeDue installmentId(@Nullable UUID installmentId) {
    this.installmentId = installmentId;
    return this;
  }

  /**
   * Installment.
   * @return installmentId
   */
  @Valid 
  @JsonProperty("installmentId")
  public @Nullable UUID getInstallmentId() {
    return installmentId;
  }

  @JsonProperty("installmentId")
  public void setInstallmentId(@Nullable UUID installmentId) {
    this.installmentId = installmentId;
  }

  public StudentFeeDue feeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
    return this;
  }

  /**
   * Fee head.
   * @return feeHeadId
   */
  @Valid 
  @JsonProperty("feeHeadId")
  public @Nullable UUID getFeeHeadId() {
    return feeHeadId;
  }

  @JsonProperty("feeHeadId")
  public void setFeeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
  }

  public StudentFeeDue dueDate(@Nullable LocalDate dueDate) {
    this.dueDate = dueDate;
    return this;
  }

  /**
   * Due date.
   * @return dueDate
   */
  @Valid 
  @JsonProperty("dueDate")
  public @Nullable LocalDate getDueDate() {
    return dueDate;
  }

  @JsonProperty("dueDate")
  public void setDueDate(@Nullable LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public StudentFeeDue amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Amount before discount.
   * @return amount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("amount")
  public String getAmount() {
    return amount;
  }

  @JsonProperty("amount")
  public void setAmount(String amount) {
    this.amount = amount;
  }

  public StudentFeeDue currencyId(UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency.
   * @return currencyId
   */
  @NotNull @Valid 
  @JsonProperty("currencyId")
  public UUID getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(UUID currencyId) {
    this.currencyId = currencyId;
  }

  public StudentFeeDue discountPercent(@Nullable String discountPercent) {
    this.discountPercent = discountPercent;
    return this;
  }

  /**
   * Discount.
   * @return discountPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("discountPercent")
  public @Nullable String getDiscountPercent() {
    return discountPercent;
  }

  @JsonProperty("discountPercent")
  public void setDiscountPercent(@Nullable String discountPercent) {
    this.discountPercent = discountPercent;
  }

  public StudentFeeDue feeFactor(@Nullable String feeFactor) {
    this.feeFactor = feeFactor;
    return this;
  }

  /**
   * Multiplier applied (e.g. pro-rata).
   * @return feeFactor
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("feeFactor")
  public @Nullable String getFeeFactor() {
    return feeFactor;
  }

  @JsonProperty("feeFactor")
  public void setFeeFactor(@Nullable String feeFactor) {
    this.feeFactor = feeFactor;
  }

  public StudentFeeDue status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State.
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

  public StudentFeeDue invoiceId(@Nullable UUID invoiceId) {
    this.invoiceId = invoiceId;
    return this;
  }

  /**
   * Invoice raised.
   * @return invoiceId
   */
  @Valid 
  @JsonProperty("invoiceId")
  public @Nullable UUID getInvoiceId() {
    return invoiceId;
  }

  @JsonProperty("invoiceId")
  public void setInvoiceId(@Nullable UUID invoiceId) {
    this.invoiceId = invoiceId;
  }

  public StudentFeeDue createTime(@Nullable OffsetDateTime createTime) {
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

  public StudentFeeDue createdBy(@Nullable UUID createdBy) {
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

  public StudentFeeDue updateTime(@Nullable OffsetDateTime updateTime) {
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

  public StudentFeeDue updatedBy(@Nullable UUID updatedBy) {
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

  public StudentFeeDue etag(@Nullable String etag) {
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
    StudentFeeDue studentFeeDue = (StudentFeeDue) o;
    return Objects.equals(this.academicYearId, studentFeeDue.academicYearId) &&
        Objects.equals(this.feeStructureId, studentFeeDue.feeStructureId) &&
        Objects.equals(this.concessionId, studentFeeDue.concessionId) &&
        Objects.equals(this.discountAmount, studentFeeDue.discountAmount) &&
        Objects.equals(this.netAmount, studentFeeDue.netAmount) &&
        Objects.equals(this.id, studentFeeDue.id) &&
        Objects.equals(this.institutionId, studentFeeDue.institutionId) &&
        Objects.equals(this.studentId, studentFeeDue.studentId) &&
        Objects.equals(this.programEnrollmentId, studentFeeDue.programEnrollmentId) &&
        Objects.equals(this.studentEnrollmentId, studentFeeDue.studentEnrollmentId) &&
        Objects.equals(this.installmentId, studentFeeDue.installmentId) &&
        Objects.equals(this.feeHeadId, studentFeeDue.feeHeadId) &&
        Objects.equals(this.dueDate, studentFeeDue.dueDate) &&
        Objects.equals(this.amount, studentFeeDue.amount) &&
        Objects.equals(this.currencyId, studentFeeDue.currencyId) &&
        Objects.equals(this.discountPercent, studentFeeDue.discountPercent) &&
        Objects.equals(this.feeFactor, studentFeeDue.feeFactor) &&
        Objects.equals(this.status, studentFeeDue.status) &&
        Objects.equals(this.invoiceId, studentFeeDue.invoiceId) &&
        Objects.equals(this.createTime, studentFeeDue.createTime) &&
        Objects.equals(this.createdBy, studentFeeDue.createdBy) &&
        Objects.equals(this.updateTime, studentFeeDue.updateTime) &&
        Objects.equals(this.updatedBy, studentFeeDue.updatedBy) &&
        Objects.equals(this.etag, studentFeeDue.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(academicYearId, feeStructureId, concessionId, discountAmount, netAmount, id, institutionId, studentId, programEnrollmentId, studentEnrollmentId, installmentId, feeHeadId, dueDate, amount, currencyId, discountPercent, feeFactor, status, invoiceId, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentFeeDue {\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    feeStructureId: ").append(toIndentedString(feeStructureId)).append("\n");
    sb.append("    concessionId: ").append(toIndentedString(concessionId)).append("\n");
    sb.append("    discountAmount: ").append(toIndentedString(discountAmount)).append("\n");
    sb.append("    netAmount: ").append(toIndentedString(netAmount)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    programEnrollmentId: ").append(toIndentedString(programEnrollmentId)).append("\n");
    sb.append("    studentEnrollmentId: ").append(toIndentedString(studentEnrollmentId)).append("\n");
    sb.append("    installmentId: ").append(toIndentedString(installmentId)).append("\n");
    sb.append("    feeHeadId: ").append(toIndentedString(feeHeadId)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    discountPercent: ").append(toIndentedString(discountPercent)).append("\n");
    sb.append("    feeFactor: ").append(toIndentedString(feeFactor)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    invoiceId: ").append(toIndentedString(invoiceId)).append("\n");
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

