// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RaiseChargeInvoiceRequest
 */

public class RaiseChargeInvoiceRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID billToUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String billToName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String amount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String sourceType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID sourceId;

  public RaiseChargeInvoiceRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RaiseChargeInvoiceRequest(UUID institutionId, String billToName, String description, String amount, String sourceType, UUID sourceId) {
    this.institutionId = institutionId;
    this.billToName = billToName;
    this.description = description;
    this.amount = amount;
    this.sourceType = sourceType;
    this.sourceId = sourceId;
  }

  public RaiseChargeInvoiceRequest institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution the charge belongs to.
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

  public RaiseChargeInvoiceRequest studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student billed (at least one of studentId and billToUserId).
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public RaiseChargeInvoiceRequest billToUserId(@Nullable UUID billToUserId) {
    this.billToUserId = billToUserId;
    return this;
  }

  /**
   * User account billed (at least one of studentId and billToUserId).
   * @return billToUserId
   */
  @Valid 
  @JsonProperty("billToUserId")
  public @Nullable UUID getBillToUserId() {
    return billToUserId;
  }

  @JsonProperty("billToUserId")
  public void setBillToUserId(@Nullable UUID billToUserId) {
    this.billToUserId = billToUserId;
  }

  public RaiseChargeInvoiceRequest billToName(String billToName) {
    this.billToName = billToName;
    return this;
  }

  /**
   * Name printed on the invoice.
   * @return billToName
   */
  @NotNull @Size(min = 1, max = 200) 
  @JsonProperty("billToName")
  public String getBillToName() {
    return billToName;
  }

  @JsonProperty("billToName")
  public void setBillToName(String billToName) {
    this.billToName = billToName;
  }

  public RaiseChargeInvoiceRequest description(String description) {
    this.description = description;
    return this;
  }

  /**
   * What the charge is for, printed as the invoice's one line.
   * @return description
   */
  @NotNull @Size(min = 1, max = 255) 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  public RaiseChargeInvoiceRequest amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Amount charged (decimal string, more than zero).
   * @return amount
   */
  @NotNull @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("amount")
  public String getAmount() {
    return amount;
  }

  @JsonProperty("amount")
  public void setAmount(String amount) {
    this.amount = amount;
  }

  public RaiseChargeInvoiceRequest currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency; when omitted, the institution's currency.
   * @return currencyId
   */
  @Valid 
  @JsonProperty("currencyId")
  public @Nullable UUID getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
  }

  public RaiseChargeInvoiceRequest dueDate(@Nullable LocalDate dueDate) {
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

  public RaiseChargeInvoiceRequest sourceType(String sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  /**
   * What raised the charge (<module>.<table>, e.g. library.loan).
   * @return sourceType
   */
  @NotNull @Pattern(regexp = "^[a-z_]+\\.[a-z_]+$") @Size(max = 80) 
  @JsonProperty("sourceType")
  public String getSourceType() {
    return sourceType;
  }

  @JsonProperty("sourceType")
  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public RaiseChargeInvoiceRequest sourceId(UUID sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  /**
   * Id of that record; one charge invoice per source.
   * @return sourceId
   */
  @NotNull @Valid 
  @JsonProperty("sourceId")
  public UUID getSourceId() {
    return sourceId;
  }

  @JsonProperty("sourceId")
  public void setSourceId(UUID sourceId) {
    this.sourceId = sourceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RaiseChargeInvoiceRequest raiseChargeInvoiceRequest = (RaiseChargeInvoiceRequest) o;
    return Objects.equals(this.institutionId, raiseChargeInvoiceRequest.institutionId) &&
        Objects.equals(this.studentId, raiseChargeInvoiceRequest.studentId) &&
        Objects.equals(this.billToUserId, raiseChargeInvoiceRequest.billToUserId) &&
        Objects.equals(this.billToName, raiseChargeInvoiceRequest.billToName) &&
        Objects.equals(this.description, raiseChargeInvoiceRequest.description) &&
        Objects.equals(this.amount, raiseChargeInvoiceRequest.amount) &&
        Objects.equals(this.currencyId, raiseChargeInvoiceRequest.currencyId) &&
        Objects.equals(this.dueDate, raiseChargeInvoiceRequest.dueDate) &&
        Objects.equals(this.sourceType, raiseChargeInvoiceRequest.sourceType) &&
        Objects.equals(this.sourceId, raiseChargeInvoiceRequest.sourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, studentId, billToUserId, billToName, description, amount, currencyId, dueDate, sourceType, sourceId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RaiseChargeInvoiceRequest {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    billToUserId: ").append(toIndentedString(billToUserId)).append("\n");
    sb.append("    billToName: ").append(toIndentedString(billToName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
    sb.append("    sourceType: ").append(toIndentedString(sourceType)).append("\n");
    sb.append("    sourceId: ").append(toIndentedString(sourceId)).append("\n");
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
