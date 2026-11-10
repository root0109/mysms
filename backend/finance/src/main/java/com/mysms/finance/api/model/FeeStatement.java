// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.StudentFeeDue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * FeeStatement
 */

public class FeeStatement {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid StudentFeeDue> dues = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Invoice> invoices = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Payment> payments = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String totalDue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String totalPaid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String balance;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String currencyCode;

  public FeeStatement() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FeeStatement(String totalDue, String totalPaid, String balance, String currencyCode) {
    this.totalDue = totalDue;
    this.totalPaid = totalPaid;
    this.balance = balance;
    this.currencyCode = currencyCode;
  }

  public FeeStatement dues(List<@Valid StudentFeeDue> dues) {
    this.dues = dues;
    return this;
  }

  public FeeStatement addDuesItem(StudentFeeDue duesItem) {
    if (this.dues == null) {
      this.dues = new ArrayList<>();
    }
    this.dues.add(duesItem);
    return this;
  }

  /**
   * Dues.
   * @return dues
   */
  
  @JsonProperty("dues")
  public List<@Valid StudentFeeDue> getDues() {
    return dues;
  }

  @JsonProperty("dues")
  public void setDues(List<@Valid StudentFeeDue> dues) {
    this.dues = dues;
  }

  public FeeStatement invoices(List<@Valid Invoice> invoices) {
    this.invoices = invoices;
    return this;
  }

  public FeeStatement addInvoicesItem(Invoice invoicesItem) {
    if (this.invoices == null) {
      this.invoices = new ArrayList<>();
    }
    this.invoices.add(invoicesItem);
    return this;
  }

  /**
   * Invoices.
   * @return invoices
   */
  
  @JsonProperty("invoices")
  public List<@Valid Invoice> getInvoices() {
    return invoices;
  }

  @JsonProperty("invoices")
  public void setInvoices(List<@Valid Invoice> invoices) {
    this.invoices = invoices;
  }

  public FeeStatement payments(List<@Valid Payment> payments) {
    this.payments = payments;
    return this;
  }

  public FeeStatement addPaymentsItem(Payment paymentsItem) {
    if (this.payments == null) {
      this.payments = new ArrayList<>();
    }
    this.payments.add(paymentsItem);
    return this;
  }

  /**
   * Payments.
   * @return payments
   */
  
  @JsonProperty("payments")
  public List<@Valid Payment> getPayments() {
    return payments;
  }

  @JsonProperty("payments")
  public void setPayments(List<@Valid Payment> payments) {
    this.payments = payments;
  }

  public FeeStatement totalDue(String totalDue) {
    this.totalDue = totalDue;
    return this;
  }

  /**
   * Total billed.
   * @return totalDue
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalDue")
  public String getTotalDue() {
    return totalDue;
  }

  @JsonProperty("totalDue")
  public void setTotalDue(String totalDue) {
    this.totalDue = totalDue;
  }

  public FeeStatement totalPaid(String totalPaid) {
    this.totalPaid = totalPaid;
    return this;
  }

  /**
   * Total paid.
   * @return totalPaid
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalPaid")
  public String getTotalPaid() {
    return totalPaid;
  }

  @JsonProperty("totalPaid")
  public void setTotalPaid(String totalPaid) {
    this.totalPaid = totalPaid;
  }

  public FeeStatement balance(String balance) {
    this.balance = balance;
    return this;
  }

  /**
   * Outstanding.
   * @return balance
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("balance")
  public String getBalance() {
    return balance;
  }

  @JsonProperty("balance")
  public void setBalance(String balance) {
    this.balance = balance;
  }

  public FeeStatement currencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
    return this;
  }

  /**
   * Currency.
   * @return currencyCode
   */
  @NotNull 
  @JsonProperty("currencyCode")
  public String getCurrencyCode() {
    return currencyCode;
  }

  @JsonProperty("currencyCode")
  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeeStatement feeStatement = (FeeStatement) o;
    return Objects.equals(this.dues, feeStatement.dues) &&
        Objects.equals(this.invoices, feeStatement.invoices) &&
        Objects.equals(this.payments, feeStatement.payments) &&
        Objects.equals(this.totalDue, feeStatement.totalDue) &&
        Objects.equals(this.totalPaid, feeStatement.totalPaid) &&
        Objects.equals(this.balance, feeStatement.balance) &&
        Objects.equals(this.currencyCode, feeStatement.currencyCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dues, invoices, payments, totalDue, totalPaid, balance, currencyCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeeStatement {\n");
    sb.append("    dues: ").append(toIndentedString(dues)).append("\n");
    sb.append("    invoices: ").append(toIndentedString(invoices)).append("\n");
    sb.append("    payments: ").append(toIndentedString(payments)).append("\n");
    sb.append("    totalDue: ").append(toIndentedString(totalDue)).append("\n");
    sb.append("    totalPaid: ").append(toIndentedString(totalPaid)).append("\n");
    sb.append("    balance: ").append(toIndentedString(balance)).append("\n");
    sb.append("    currencyCode: ").append(toIndentedString(currencyCode)).append("\n");
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

