// Owned API contract of payroll.
package com.mysms.payroll.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * MarkPayslipPaidRequest
 */

public class MarkPayslipPaidRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID disbursementAccountId;

  /**
   * Method.
   */
  public enum PaymentMethodEnum {
    CASH("cash"),
    
    CHEQUE("cheque"),
    
    BANK_TRANSFER("bank_transfer"),
    
    OTHER("other");

    private final String value;

    PaymentMethodEnum(String value) {
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
    public static PaymentMethodEnum fromValue(String value) {
      for (PaymentMethodEnum b : PaymentMethodEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private PaymentMethodEnum paymentMethod;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String referenceNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate transactionDate;

  public MarkPayslipPaidRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarkPayslipPaidRequest(UUID disbursementAccountId, PaymentMethodEnum paymentMethod, LocalDate transactionDate) {
    this.disbursementAccountId = disbursementAccountId;
    this.paymentMethod = paymentMethod;
    this.transactionDate = transactionDate;
  }

  public MarkPayslipPaidRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
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

  public MarkPayslipPaidRequest disbursementAccountId(UUID disbursementAccountId) {
    this.disbursementAccountId = disbursementAccountId;
    return this;
  }

  /**
   * Account.
   * @return disbursementAccountId
   */
  @NotNull @Valid 
  @JsonProperty("disbursementAccountId")
  public UUID getDisbursementAccountId() {
    return disbursementAccountId;
  }

  @JsonProperty("disbursementAccountId")
  public void setDisbursementAccountId(UUID disbursementAccountId) {
    this.disbursementAccountId = disbursementAccountId;
  }

  public MarkPayslipPaidRequest paymentMethod(PaymentMethodEnum paymentMethod) {
    this.paymentMethod = paymentMethod;
    return this;
  }

  /**
   * Method.
   * @return paymentMethod
   */
  @NotNull 
  @JsonProperty("paymentMethod")
  public PaymentMethodEnum getPaymentMethod() {
    return paymentMethod;
  }

  @JsonProperty("paymentMethod")
  public void setPaymentMethod(PaymentMethodEnum paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public MarkPayslipPaidRequest referenceNumber(@Nullable String referenceNumber) {
    this.referenceNumber = referenceNumber;
    return this;
  }

  /**
   * Reference.
   * @return referenceNumber
   */
  
  @JsonProperty("referenceNumber")
  public @Nullable String getReferenceNumber() {
    return referenceNumber;
  }

  @JsonProperty("referenceNumber")
  public void setReferenceNumber(@Nullable String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public MarkPayslipPaidRequest transactionDate(LocalDate transactionDate) {
    this.transactionDate = transactionDate;
    return this;
  }

  /**
   * Date.
   * @return transactionDate
   */
  @NotNull @Valid 
  @JsonProperty("transactionDate")
  public LocalDate getTransactionDate() {
    return transactionDate;
  }

  @JsonProperty("transactionDate")
  public void setTransactionDate(LocalDate transactionDate) {
    this.transactionDate = transactionDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarkPayslipPaidRequest markPayslipPaidRequest = (MarkPayslipPaidRequest) o;
    return Objects.equals(this.etag, markPayslipPaidRequest.etag) &&
        Objects.equals(this.disbursementAccountId, markPayslipPaidRequest.disbursementAccountId) &&
        Objects.equals(this.paymentMethod, markPayslipPaidRequest.paymentMethod) &&
        Objects.equals(this.referenceNumber, markPayslipPaidRequest.referenceNumber) &&
        Objects.equals(this.transactionDate, markPayslipPaidRequest.transactionDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, disbursementAccountId, paymentMethod, referenceNumber, transactionDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarkPayslipPaidRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    disbursementAccountId: ").append(toIndentedString(disbursementAccountId)).append("\n");
    sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
    sb.append("    referenceNumber: ").append(toIndentedString(referenceNumber)).append("\n");
    sb.append("    transactionDate: ").append(toIndentedString(transactionDate)).append("\n");
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

