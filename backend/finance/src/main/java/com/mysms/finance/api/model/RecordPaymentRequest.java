// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mysms.finance.api.model.PaymentAllocationInput;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * An offline/counter payment and how it settles invoices and dues.
 */

public class RecordPaymentRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID payerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID payerPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String payerName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String billingName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String amount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID currencyId;

  /**
   * How the money arrived.
   */
  public enum PaymentMethodEnum {
    CASH("cash"),
    
    CHEQUE("cheque"),
    
    DD("dd"),
    
    BANK_TRANSFER("bank_transfer"),
    
    CARD_POS("card_pos"),
    
    ADJUSTMENT("adjustment");

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
  private @Nullable String instrumentReference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate instrumentDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bankName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime receivedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid PaymentAllocationInput> allocations = new ArrayList<>();

  public RecordPaymentRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RecordPaymentRequest(String billingName, String amount, UUID currencyId, PaymentMethodEnum paymentMethod) {
    this.billingName = billingName;
    this.amount = amount;
    this.currencyId = currencyId;
    this.paymentMethod = paymentMethod;
  }

  public RecordPaymentRequest payerUserId(@Nullable UUID payerUserId) {
    this.payerUserId = payerUserId;
    return this;
  }

  /**
   * Paying user, when they have an account.
   * @return payerUserId
   */
  @Valid 
  @JsonProperty("payerUserId")
  public @Nullable UUID getPayerUserId() {
    return payerUserId;
  }

  @JsonProperty("payerUserId")
  public void setPayerUserId(@Nullable UUID payerUserId) {
    this.payerUserId = payerUserId;
  }

  public RecordPaymentRequest payerPersonId(@Nullable UUID payerPersonId) {
    this.payerPersonId = payerPersonId;
    return this;
  }

  /**
   * Paying person without an account (at least one payer field required).
   * @return payerPersonId
   */
  @Valid 
  @JsonProperty("payerPersonId")
  public @Nullable UUID getPayerPersonId() {
    return payerPersonId;
  }

  @JsonProperty("payerPersonId")
  public void setPayerPersonId(@Nullable UUID payerPersonId) {
    this.payerPersonId = payerPersonId;
  }

  public RecordPaymentRequest payerName(@Nullable String payerName) {
    this.payerName = payerName;
    return this;
  }

  /**
   * Free-text payer name when no record exists.
   * @return payerName
   */
  @Size(max = 200) 
  @JsonProperty("payerName")
  public @Nullable String getPayerName() {
    return payerName;
  }

  @JsonProperty("payerName")
  public void setPayerName(@Nullable String payerName) {
    this.payerName = payerName;
  }

  public RecordPaymentRequest billingName(String billingName) {
    this.billingName = billingName;
    return this;
  }

  /**
   * Name on the receipt.
   * @return billingName
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("billingName")
  public String getBillingName() {
    return billingName;
  }

  @JsonProperty("billingName")
  public void setBillingName(String billingName) {
    this.billingName = billingName;
  }

  public RecordPaymentRequest amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Amount received.
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

  public RecordPaymentRequest currencyId(UUID currencyId) {
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

  public RecordPaymentRequest paymentMethod(PaymentMethodEnum paymentMethod) {
    this.paymentMethod = paymentMethod;
    return this;
  }

  /**
   * How the money arrived.
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

  public RecordPaymentRequest instrumentReference(@Nullable String instrumentReference) {
    this.instrumentReference = instrumentReference;
    return this;
  }

  /**
   * Cheque/DD/transfer reference.
   * @return instrumentReference
   */
  @Size(max = 80) 
  @JsonProperty("instrumentReference")
  public @Nullable String getInstrumentReference() {
    return instrumentReference;
  }

  @JsonProperty("instrumentReference")
  public void setInstrumentReference(@Nullable String instrumentReference) {
    this.instrumentReference = instrumentReference;
  }

  public RecordPaymentRequest instrumentDate(@Nullable LocalDate instrumentDate) {
    this.instrumentDate = instrumentDate;
    return this;
  }

  /**
   * Date on the instrument.
   * @return instrumentDate
   */
  @Valid 
  @JsonProperty("instrumentDate")
  public @Nullable LocalDate getInstrumentDate() {
    return instrumentDate;
  }

  @JsonProperty("instrumentDate")
  public void setInstrumentDate(@Nullable LocalDate instrumentDate) {
    this.instrumentDate = instrumentDate;
  }

  public RecordPaymentRequest bankName(@Nullable String bankName) {
    this.bankName = bankName;
    return this;
  }

  /**
   * Drawee bank.
   * @return bankName
   */
  @Size(max = 120) 
  @JsonProperty("bankName")
  public @Nullable String getBankName() {
    return bankName;
  }

  @JsonProperty("bankName")
  public void setBankName(@Nullable String bankName) {
    this.bankName = bankName;
  }

  public RecordPaymentRequest receivedAt(@Nullable OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
    return this;
  }

  /**
   * When the money was received (defaults to now).
   * @return receivedAt
   */
  @Valid 
  @JsonProperty("receivedAt")
  public @Nullable OffsetDateTime getReceivedAt() {
    return receivedAt;
  }

  @JsonProperty("receivedAt")
  public void setReceivedAt(@Nullable OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  public RecordPaymentRequest allocations(List<@Valid PaymentAllocationInput> allocations) {
    this.allocations = allocations;
    return this;
  }

  public RecordPaymentRequest addAllocationsItem(PaymentAllocationInput allocationsItem) {
    if (this.allocations == null) {
      this.allocations = new ArrayList<>();
    }
    this.allocations.add(allocationsItem);
    return this;
  }

  /**
   * How the amount settles invoices and dues (sum must not exceed amount).
   * @return allocations
   */
  
  @JsonProperty("allocations")
  public List<@Valid PaymentAllocationInput> getAllocations() {
    return allocations;
  }

  @JsonProperty("allocations")
  public void setAllocations(List<@Valid PaymentAllocationInput> allocations) {
    this.allocations = allocations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordPaymentRequest recordPaymentRequest = (RecordPaymentRequest) o;
    return Objects.equals(this.payerUserId, recordPaymentRequest.payerUserId) &&
        Objects.equals(this.payerPersonId, recordPaymentRequest.payerPersonId) &&
        Objects.equals(this.payerName, recordPaymentRequest.payerName) &&
        Objects.equals(this.billingName, recordPaymentRequest.billingName) &&
        Objects.equals(this.amount, recordPaymentRequest.amount) &&
        Objects.equals(this.currencyId, recordPaymentRequest.currencyId) &&
        Objects.equals(this.paymentMethod, recordPaymentRequest.paymentMethod) &&
        Objects.equals(this.instrumentReference, recordPaymentRequest.instrumentReference) &&
        Objects.equals(this.instrumentDate, recordPaymentRequest.instrumentDate) &&
        Objects.equals(this.bankName, recordPaymentRequest.bankName) &&
        Objects.equals(this.receivedAt, recordPaymentRequest.receivedAt) &&
        Objects.equals(this.allocations, recordPaymentRequest.allocations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payerUserId, payerPersonId, payerName, billingName, amount, currencyId, paymentMethod, instrumentReference, instrumentDate, bankName, receivedAt, allocations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordPaymentRequest {\n");
    sb.append("    payerUserId: ").append(toIndentedString(payerUserId)).append("\n");
    sb.append("    payerPersonId: ").append(toIndentedString(payerPersonId)).append("\n");
    sb.append("    payerName: ").append(toIndentedString(payerName)).append("\n");
    sb.append("    billingName: ").append(toIndentedString(billingName)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
    sb.append("    instrumentReference: ").append(toIndentedString(instrumentReference)).append("\n");
    sb.append("    instrumentDate: ").append(toIndentedString(instrumentDate)).append("\n");
    sb.append("    bankName: ").append(toIndentedString(bankName)).append("\n");
    sb.append("    receivedAt: ").append(toIndentedString(receivedAt)).append("\n");
    sb.append("    allocations: ").append(toIndentedString(allocations)).append("\n");
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

