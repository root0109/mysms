// Owned API contract of finance.
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
 * One slice of a payment settling an invoice or a fee due (exactly one target).
 */

public class PaymentAllocationInput {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID invoiceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentFeeDueId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String amount;

  public PaymentAllocationInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PaymentAllocationInput(String amount) {
    this.amount = amount;
  }

  public PaymentAllocationInput invoiceId(@Nullable UUID invoiceId) {
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

  public PaymentAllocationInput studentFeeDueId(@Nullable UUID studentFeeDueId) {
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

  public PaymentAllocationInput amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Allocated amount.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaymentAllocationInput paymentAllocationInput = (PaymentAllocationInput) o;
    return Objects.equals(this.invoiceId, paymentAllocationInput.invoiceId) &&
        Objects.equals(this.studentFeeDueId, paymentAllocationInput.studentFeeDueId) &&
        Objects.equals(this.amount, paymentAllocationInput.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invoiceId, studentFeeDueId, amount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaymentAllocationInput {\n");
    sb.append("    invoiceId: ").append(toIndentedString(invoiceId)).append("\n");
    sb.append("    studentFeeDueId: ").append(toIndentedString(studentFeeDueId)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
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

