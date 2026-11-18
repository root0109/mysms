// Owned API contract of finance.
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
 * Invoice raised to a student / guardian / user (fees, library fines, purchases).
 */

public class Invoice {

  /**
   * invoice or credit_note. A posted invoice is never edited; corrections are credit notes.
   */
  public enum InvoiceTypeEnum {
    INVOICE("invoice"),
    
    CREDIT_NOTE("credit_note");

    private final String value;

    InvoiceTypeEnum(String value) {
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
    public static InvoiceTypeEnum fromValue(String value) {
      for (InvoiceTypeEnum b : InvoiceTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable InvoiceTypeEnum invoiceType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID creditForInvoiceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime postedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime cancelledAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID documentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String invoiceNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID billToUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String billToName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate invoiceDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subtotal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String discountTotal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String taxTotal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String total;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String amountPaid;

  /**
   * State. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    DRAFT("draft"),
    
    POSTED("posted"),
    
    PARTIALLY_PAID("partially_paid"),
    
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

  public Invoice() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Invoice(String invoiceNumber, String billToName, LocalDate invoiceDate, UUID currencyId) {
    this.invoiceNumber = invoiceNumber;
    this.billToName = billToName;
    this.invoiceDate = invoiceDate;
    this.currencyId = currencyId;
  }

  public Invoice invoiceType(@Nullable InvoiceTypeEnum invoiceType) {
    this.invoiceType = invoiceType;
    return this;
  }

  /**
   * invoice or credit_note. A posted invoice is never edited; corrections are credit notes.
   * @return invoiceType
   */
  
  @JsonProperty("invoiceType")
  public @Nullable InvoiceTypeEnum getInvoiceType() {
    return invoiceType;
  }

  @JsonProperty("invoiceType")
  public void setInvoiceType(@Nullable InvoiceTypeEnum invoiceType) {
    this.invoiceType = invoiceType;
  }

  public Invoice creditForInvoiceId(@Nullable UUID creditForInvoiceId) {
    this.creditForInvoiceId = creditForInvoiceId;
    return this;
  }

  /**
   * Invoice a credit note corrects.
   * @return creditForInvoiceId
   */
  @Valid 
  @JsonProperty("creditForInvoiceId")
  public @Nullable UUID getCreditForInvoiceId() {
    return creditForInvoiceId;
  }

  @JsonProperty("creditForInvoiceId")
  public void setCreditForInvoiceId(@Nullable UUID creditForInvoiceId) {
    this.creditForInvoiceId = creditForInvoiceId;
  }

  public Invoice postedAt(@Nullable OffsetDateTime postedAt) {
    this.postedAt = postedAt;
    return this;
  }

  /**
   * When the invoice became a posted (immutable) document.
   * @return postedAt
   */
  @Valid 
  @JsonProperty("postedAt")
  public @Nullable OffsetDateTime getPostedAt() {
    return postedAt;
  }

  @JsonProperty("postedAt")
  public void setPostedAt(@Nullable OffsetDateTime postedAt) {
    this.postedAt = postedAt;
  }

  public Invoice cancelledAt(@Nullable OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
    return this;
  }

  /**
   * When it was cancelled (allowed only while draft).
   * @return cancelledAt
   */
  @Valid 
  @JsonProperty("cancelledAt")
  public @Nullable OffsetDateTime getCancelledAt() {
    return cancelledAt;
  }

  @JsonProperty("cancelledAt")
  public void setCancelledAt(@Nullable OffsetDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public Invoice documentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
    return this;
  }

  /**
   * Rendered PDF of the issued document.
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

  public Invoice id(@Nullable UUID id) {
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

  public Invoice institutionId(@Nullable UUID institutionId) {
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

  public Invoice invoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
    return this;
  }

  /**
   * Invoice number.
   * @return invoiceNumber
   */
  @Size(max = 40) 
  @JsonProperty("invoiceNumber")
  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  @JsonProperty("invoiceNumber")
  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public Invoice studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Billed student.
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

  public Invoice billToUserId(@Nullable UUID billToUserId) {
    this.billToUserId = billToUserId;
    return this;
  }

  /**
   * Billed user (guardian / student).
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

  public Invoice billToName(String billToName) {
    this.billToName = billToName;
    return this;
  }

  /**
   * Name on the invoice.
   * @return billToName
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("billToName")
  public String getBillToName() {
    return billToName;
  }

  @JsonProperty("billToName")
  public void setBillToName(String billToName) {
    this.billToName = billToName;
  }

  public Invoice invoiceDate(LocalDate invoiceDate) {
    this.invoiceDate = invoiceDate;
    return this;
  }

  /**
   * Invoice date.
   * @return invoiceDate
   */
  @NotNull @Valid 
  @JsonProperty("invoiceDate")
  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  @JsonProperty("invoiceDate")
  public void setInvoiceDate(LocalDate invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public Invoice dueDate(@Nullable LocalDate dueDate) {
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

  public Invoice currencyId(UUID currencyId) {
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

  public Invoice subtotal(@Nullable String subtotal) {
    this.subtotal = subtotal;
    return this;
  }

  /**
   * Sum of line amounts.
   * @return subtotal
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("subtotal")
  public @Nullable String getSubtotal() {
    return subtotal;
  }

  @JsonProperty("subtotal")
  public void setSubtotal(@Nullable String subtotal) {
    this.subtotal = subtotal;
  }

  public Invoice discountTotal(@Nullable String discountTotal) {
    this.discountTotal = discountTotal;
    return this;
  }

  /**
   * Discounts.
   * @return discountTotal
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("discountTotal")
  public @Nullable String getDiscountTotal() {
    return discountTotal;
  }

  @JsonProperty("discountTotal")
  public void setDiscountTotal(@Nullable String discountTotal) {
    this.discountTotal = discountTotal;
  }

  public Invoice taxTotal(@Nullable String taxTotal) {
    this.taxTotal = taxTotal;
    return this;
  }

  /**
   * Taxes.
   * @return taxTotal
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("taxTotal")
  public @Nullable String getTaxTotal() {
    return taxTotal;
  }

  @JsonProperty("taxTotal")
  public void setTaxTotal(@Nullable String taxTotal) {
    this.taxTotal = taxTotal;
  }

  public Invoice total(@Nullable String total) {
    this.total = total;
    return this;
  }

  /**
   * Grand total.
   * @return total
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("total")
  public @Nullable String getTotal() {
    return total;
  }

  @JsonProperty("total")
  public void setTotal(@Nullable String total) {
    this.total = total;
  }

  public Invoice amountPaid(@Nullable String amountPaid) {
    this.amountPaid = amountPaid;
    return this;
  }

  /**
   * Paid so far.
   * @return amountPaid
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("amountPaid")
  public @Nullable String getAmountPaid() {
    return amountPaid;
  }

  @JsonProperty("amountPaid")
  public void setAmountPaid(@Nullable String amountPaid) {
    this.amountPaid = amountPaid;
  }

  public Invoice status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State. Changed only through the workflow operations.
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

  public Invoice createTime(@Nullable OffsetDateTime createTime) {
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

  public Invoice createdBy(@Nullable UUID createdBy) {
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

  public Invoice updateTime(@Nullable OffsetDateTime updateTime) {
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

  public Invoice updatedBy(@Nullable UUID updatedBy) {
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

  public Invoice etag(@Nullable String etag) {
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
    Invoice invoice = (Invoice) o;
    return Objects.equals(this.invoiceType, invoice.invoiceType) &&
        Objects.equals(this.creditForInvoiceId, invoice.creditForInvoiceId) &&
        Objects.equals(this.postedAt, invoice.postedAt) &&
        Objects.equals(this.cancelledAt, invoice.cancelledAt) &&
        Objects.equals(this.documentFileId, invoice.documentFileId) &&
        Objects.equals(this.id, invoice.id) &&
        Objects.equals(this.institutionId, invoice.institutionId) &&
        Objects.equals(this.invoiceNumber, invoice.invoiceNumber) &&
        Objects.equals(this.studentId, invoice.studentId) &&
        Objects.equals(this.billToUserId, invoice.billToUserId) &&
        Objects.equals(this.billToName, invoice.billToName) &&
        Objects.equals(this.invoiceDate, invoice.invoiceDate) &&
        Objects.equals(this.dueDate, invoice.dueDate) &&
        Objects.equals(this.currencyId, invoice.currencyId) &&
        Objects.equals(this.subtotal, invoice.subtotal) &&
        Objects.equals(this.discountTotal, invoice.discountTotal) &&
        Objects.equals(this.taxTotal, invoice.taxTotal) &&
        Objects.equals(this.total, invoice.total) &&
        Objects.equals(this.amountPaid, invoice.amountPaid) &&
        Objects.equals(this.status, invoice.status) &&
        Objects.equals(this.createTime, invoice.createTime) &&
        Objects.equals(this.createdBy, invoice.createdBy) &&
        Objects.equals(this.updateTime, invoice.updateTime) &&
        Objects.equals(this.updatedBy, invoice.updatedBy) &&
        Objects.equals(this.etag, invoice.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invoiceType, creditForInvoiceId, postedAt, cancelledAt, documentFileId, id, institutionId, invoiceNumber, studentId, billToUserId, billToName, invoiceDate, dueDate, currencyId, subtotal, discountTotal, taxTotal, total, amountPaid, status, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Invoice {\n");
    sb.append("    invoiceType: ").append(toIndentedString(invoiceType)).append("\n");
    sb.append("    creditForInvoiceId: ").append(toIndentedString(creditForInvoiceId)).append("\n");
    sb.append("    postedAt: ").append(toIndentedString(postedAt)).append("\n");
    sb.append("    cancelledAt: ").append(toIndentedString(cancelledAt)).append("\n");
    sb.append("    documentFileId: ").append(toIndentedString(documentFileId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    invoiceNumber: ").append(toIndentedString(invoiceNumber)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    billToUserId: ").append(toIndentedString(billToUserId)).append("\n");
    sb.append("    billToName: ").append(toIndentedString(billToName)).append("\n");
    sb.append("    invoiceDate: ").append(toIndentedString(invoiceDate)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    subtotal: ").append(toIndentedString(subtotal)).append("\n");
    sb.append("    discountTotal: ").append(toIndentedString(discountTotal)).append("\n");
    sb.append("    taxTotal: ").append(toIndentedString(taxTotal)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    amountPaid: ").append(toIndentedString(amountPaid)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

