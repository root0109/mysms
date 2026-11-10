// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

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
 * One slice of a payment settling an invoice or a fee due. Ledger row — never updated; reversed by a refund.
 */

public class PaymentAllocation {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID invoiceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentFeeDueId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String amount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  public PaymentAllocation id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
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

  public PaymentAllocation invoiceId(@Nullable UUID invoiceId) {
    this.invoiceId = invoiceId;
    return this;
  }

  /**
   * Invoice this slice settles.
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

  public PaymentAllocation studentFeeDueId(@Nullable UUID studentFeeDueId) {
    this.studentFeeDueId = studentFeeDueId;
    return this;
  }

  /**
   * Fee due this slice settles.
   * @return studentFeeDueId
   */
  @Valid 
  @JsonProperty("studentFeeDueId")
  public @Nullable UUID getStudentFeeDueId() {
    return studentFeeDueId;
  }

  @JsonProperty("studentFeeDueId")
  public void setStudentFeeDueId(@Nullable UUID studentFeeDueId) {
    this.studentFeeDueId = studentFeeDueId;
  }

  public PaymentAllocation amount(@Nullable String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Allocated amount.
   * @return amount
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("amount")
  public @Nullable String getAmount() {
    return amount;
  }

  @JsonProperty("amount")
  public void setAmount(@Nullable String amount) {
    this.amount = amount;
  }

  public PaymentAllocation currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency of the slice (always the payment currency).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaymentAllocation paymentAllocation = (PaymentAllocation) o;
    return Objects.equals(this.id, paymentAllocation.id) &&
        Objects.equals(this.invoiceId, paymentAllocation.invoiceId) &&
        Objects.equals(this.studentFeeDueId, paymentAllocation.studentFeeDueId) &&
        Objects.equals(this.amount, paymentAllocation.amount) &&
        Objects.equals(this.currencyId, paymentAllocation.currencyId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, invoiceId, studentFeeDueId, amount, currencyId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaymentAllocation {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    invoiceId: ").append(toIndentedString(invoiceId)).append("\n");
    sb.append("    studentFeeDueId: ").append(toIndentedString(studentFeeDueId)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
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

