// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RefundPaymentRequest
 */

public class RefundPaymentRequest {

  /**
   * How the money goes back (defaults to the payment's own method).
   */
  public enum RefundMethodEnum {
    GATEWAY("gateway"),
    
    CASH("cash"),
    
    CHEQUE("cheque"),
    
    BANK_TRANSFER("bank_transfer"),
    
    ADJUSTMENT("adjustment");

    private final String value;

    RefundMethodEnum(String value) {
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
    public static RefundMethodEnum fromValue(String value) {
      for (RefundMethodEnum b : RefundMethodEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RefundMethodEnum refundMethod;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String amount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reason;

  public RefundPaymentRequest refundMethod(@Nullable RefundMethodEnum refundMethod) {
    this.refundMethod = refundMethod;
    return this;
  }

  /**
   * How the money goes back (defaults to the payment's own method).
   * @return refundMethod
   */
  
  @JsonProperty("refundMethod")
  public @Nullable RefundMethodEnum getRefundMethod() {
    return refundMethod;
  }

  @JsonProperty("refundMethod")
  public void setRefundMethod(@Nullable RefundMethodEnum refundMethod) {
    this.refundMethod = refundMethod;
  }

  public RefundPaymentRequest etag(@Nullable String etag) {
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

  public RefundPaymentRequest amount(@Nullable String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Partial amount.
   * @return amount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("amount")
  public @Nullable String getAmount() {
    return amount;
  }

  @JsonProperty("amount")
  public void setAmount(@Nullable String amount) {
    this.amount = amount;
  }

  public RefundPaymentRequest reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Reason / comment recorded with the transition.
   * @return reason
   */
  
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RefundPaymentRequest refundPaymentRequest = (RefundPaymentRequest) o;
    return Objects.equals(this.refundMethod, refundPaymentRequest.refundMethod) &&
        Objects.equals(this.etag, refundPaymentRequest.etag) &&
        Objects.equals(this.amount, refundPaymentRequest.amount) &&
        Objects.equals(this.reason, refundPaymentRequest.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refundMethod, etag, amount, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RefundPaymentRequest {\n");
    sb.append("    refundMethod: ").append(toIndentedString(refundMethod)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

