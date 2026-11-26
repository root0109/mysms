// Owned API contract of finance.
package com.mysms.finance.api.model;

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
 * Fields of Invoice that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class InvoiceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> billToName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> invoiceDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> dueDate;

  public InvoiceUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public InvoiceUpdate billToName(@Nullable Optional<String> billToName) {
    this.billToName = billToName;
    return this;
  }

  /**
   * Name on the invoice.
   * @return billToName
   */
  
  @JsonProperty("billToName")
  public @Nullable Optional<String> getBillToName() {
    return billToName;
  }

  @JsonProperty("billToName")
  public void setBillToName(@Nullable Optional<String> billToName) {
    this.billToName = billToName;
  }

  public InvoiceUpdate invoiceDate(@Nullable Optional<LocalDate> invoiceDate) {
    this.invoiceDate = invoiceDate;
    return this;
  }

  /**
   * Invoice date.
   * @return invoiceDate
   */
  
  @JsonProperty("invoiceDate")
  public @Nullable Optional<LocalDate> getInvoiceDate() {
    return invoiceDate;
  }

  @JsonProperty("invoiceDate")
  public void setInvoiceDate(@Nullable Optional<LocalDate> invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public InvoiceUpdate dueDate(@Nullable Optional<LocalDate> dueDate) {
    this.dueDate = dueDate;
    return this;
  }

  /**
   * Due date.
   * @return dueDate
   */
  
  @JsonProperty("dueDate")
  public @Nullable Optional<LocalDate> getDueDate() {
    return dueDate;
  }

  @JsonProperty("dueDate")
  public void setDueDate(@Nullable Optional<LocalDate> dueDate) {
    this.dueDate = dueDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvoiceUpdate invoiceUpdate = (InvoiceUpdate) o;
    return Objects.equals(this.institutionId, invoiceUpdate.institutionId) &&
        Objects.equals(this.billToName, invoiceUpdate.billToName) &&
        Objects.equals(this.invoiceDate, invoiceUpdate.invoiceDate) &&
        Objects.equals(this.dueDate, invoiceUpdate.dueDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, billToName, invoiceDate, dueDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvoiceUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    billToName: ").append(toIndentedString(billToName)).append("\n");
    sb.append("    invoiceDate: ").append(toIndentedString(invoiceDate)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
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

