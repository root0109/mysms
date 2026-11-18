// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mysms.finance.api.model.PaymentAllocation;
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
 * Money received: fees, LMS course / cohort / certificate purchases, admission fees.
 */

public class Payment {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid PaymentAllocation> allocations = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate instrumentDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bankName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gatewayAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID receivedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String instrumentReference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String payerName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID payerPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String receiptNumber;

  /**
   * How the money arrived.
   */
  public enum PaymentMethodEnum {
    GATEWAY("gateway"),
    
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
  private @Nullable PaymentMethodEnum paymentMethod;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID payerUserId;

  /**
   * What the payment is for.
   */
  public enum PurposeEnum {
    INVOICE("invoice"),
    
    FEES("fees"),
    
    LMS_COURSE("lms_course"),
    
    LMS_COHORT("lms_cohort"),
    
    LMS_CERTIFICATE("lms_certificate"),
    
    ADMISSION("admission");

    private final String value;

    PurposeEnum(String value) {
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
    public static PurposeEnum fromValue(String value) {
      for (PurposeEnum b : PurposeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private PurposeEnum purpose;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID invoiceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lmsCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lmsCohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID admissionApplicationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayOrderId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayPaymentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String originalAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String discountAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String amount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String amountWithTax;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID couponId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String couponCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String billingName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID billingAddressId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String taxRegistrationNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String taxIdentity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String taxIdentityMasked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID acquisitionSourceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean payerConsent;

  /**
   * State. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    INITIATED("initiated"),
    
    RECEIVED("received"),
    
    FAILED("failed"),
    
    REFUNDED("refunded");

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
  private @Nullable OffsetDateTime receivedAt;

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

  public Payment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Payment(PurposeEnum purpose, String amount, UUID currencyId) {
    this.purpose = purpose;
    this.amount = amount;
    this.currencyId = currencyId;
  }

  public Payment allocations(List<@Valid PaymentAllocation> allocations) {
    this.allocations = allocations;
    return this;
  }

  public Payment addAllocationsItem(PaymentAllocation allocationsItem) {
    if (this.allocations == null) {
      this.allocations = new ArrayList<>();
    }
    this.allocations.add(allocationsItem);
    return this;
  }

  /**
   * How this payment settles invoices and dues.
   * @return allocations
   */
  
  @JsonProperty("allocations")
  public List<@Valid PaymentAllocation> getAllocations() {
    return allocations;
  }

  @JsonProperty("allocations")
  public void setAllocations(List<@Valid PaymentAllocation> allocations) {
    this.allocations = allocations;
  }

  public Payment instrumentDate(@Nullable LocalDate instrumentDate) {
    this.instrumentDate = instrumentDate;
    return this;
  }

  /**
   * Date on the cheque/DD/transfer instrument.
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

  public Payment bankName(@Nullable String bankName) {
    this.bankName = bankName;
    return this;
  }

  /**
   * Drawee bank for cheque/DD payments.
   * @return bankName
   */
  
  @JsonProperty("bankName")
  public @Nullable String getBankName() {
    return bankName;
  }

  @JsonProperty("bankName")
  public void setBankName(@Nullable String bankName) {
    this.bankName = bankName;
  }

  public Payment gatewayAccountId(@Nullable UUID gatewayAccountId) {
    this.gatewayAccountId = gatewayAccountId;
    return this;
  }

  /**
   * Gateway account (merchant credentials) the payment was taken on.
   * @return gatewayAccountId
   */
  @Valid 
  @JsonProperty("gatewayAccountId")
  public @Nullable UUID getGatewayAccountId() {
    return gatewayAccountId;
  }

  @JsonProperty("gatewayAccountId")
  public void setGatewayAccountId(@Nullable UUID gatewayAccountId) {
    this.gatewayAccountId = gatewayAccountId;
  }

  public Payment receivedByUserId(@Nullable UUID receivedByUserId) {
    this.receivedByUserId = receivedByUserId;
    return this;
  }

  /**
   * Staff user who recorded a counter payment.
   * @return receivedByUserId
   */
  @Valid 
  @JsonProperty("receivedByUserId")
  public @Nullable UUID getReceivedByUserId() {
    return receivedByUserId;
  }

  @JsonProperty("receivedByUserId")
  public void setReceivedByUserId(@Nullable UUID receivedByUserId) {
    this.receivedByUserId = receivedByUserId;
  }

  public Payment instrumentReference(@Nullable String instrumentReference) {
    this.instrumentReference = instrumentReference;
    return this;
  }

  /**
   * Cheque/DD/transfer reference number for offline payments.
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

  public Payment payerName(@Nullable String payerName) {
    this.payerName = payerName;
    return this;
  }

  /**
   * Free-text payer name for counter payments with no person record.
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

  public Payment payerPersonId(@Nullable UUID payerPersonId) {
    this.payerPersonId = payerPersonId;
    return this;
  }

  /**
   * Paying person when they have no user account (counter payments).
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

  public Payment receiptNumber(@Nullable String receiptNumber) {
    this.receiptNumber = receiptNumber;
    return this;
  }

  /**
   * Tenant receipt number issued for offline/counter payments; unique per tenant when set.
   * @return receiptNumber
   */
  @Size(max = 40) 
  @JsonProperty("receiptNumber")
  public @Nullable String getReceiptNumber() {
    return receiptNumber;
  }

  @JsonProperty("receiptNumber")
  public void setReceiptNumber(@Nullable String receiptNumber) {
    this.receiptNumber = receiptNumber;
  }

  public Payment paymentMethod(@Nullable PaymentMethodEnum paymentMethod) {
    this.paymentMethod = paymentMethod;
    return this;
  }

  /**
   * How the money arrived.
   * @return paymentMethod
   */
  
  @JsonProperty("paymentMethod")
  public @Nullable PaymentMethodEnum getPaymentMethod() {
    return paymentMethod;
  }

  @JsonProperty("paymentMethod")
  public void setPaymentMethod(@Nullable PaymentMethodEnum paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public Payment id(@Nullable UUID id) {
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

  public Payment institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution receiving the money.
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

  public Payment payerUserId(@Nullable UUID payerUserId) {
    this.payerUserId = payerUserId;
    return this;
  }

  /**
   * Payer.
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

  public Payment purpose(PurposeEnum purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * What the payment is for.
   * @return purpose
   */
  @NotNull 
  @JsonProperty("purpose")
  public PurposeEnum getPurpose() {
    return purpose;
  }

  @JsonProperty("purpose")
  public void setPurpose(PurposeEnum purpose) {
    this.purpose = purpose;
  }

  public Payment invoiceId(@Nullable UUID invoiceId) {
    this.invoiceId = invoiceId;
    return this;
  }

  /**
   * Invoice settled.
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

  public Payment lmsCourseId(@Nullable UUID lmsCourseId) {
    this.lmsCourseId = lmsCourseId;
    return this;
  }

  /**
   * Course purchased.
   * @return lmsCourseId
   */
  @Valid 
  @JsonProperty("lmsCourseId")
  public @Nullable UUID getLmsCourseId() {
    return lmsCourseId;
  }

  @JsonProperty("lmsCourseId")
  public void setLmsCourseId(@Nullable UUID lmsCourseId) {
    this.lmsCourseId = lmsCourseId;
  }

  public Payment lmsCohortId(@Nullable UUID lmsCohortId) {
    this.lmsCohortId = lmsCohortId;
    return this;
  }

  /**
   * Cohort purchased.
   * @return lmsCohortId
   */
  @Valid 
  @JsonProperty("lmsCohortId")
  public @Nullable UUID getLmsCohortId() {
    return lmsCohortId;
  }

  @JsonProperty("lmsCohortId")
  public void setLmsCohortId(@Nullable UUID lmsCohortId) {
    this.lmsCohortId = lmsCohortId;
  }

  public Payment admissionApplicationId(@Nullable UUID admissionApplicationId) {
    this.admissionApplicationId = admissionApplicationId;
    return this;
  }

  /**
   * Application paid for.
   * @return admissionApplicationId
   */
  @Valid 
  @JsonProperty("admissionApplicationId")
  public @Nullable UUID getAdmissionApplicationId() {
    return admissionApplicationId;
  }

  @JsonProperty("admissionApplicationId")
  public void setAdmissionApplicationId(@Nullable UUID admissionApplicationId) {
    this.admissionApplicationId = admissionApplicationId;
  }

  public Payment gatewayName(@Nullable String gatewayName) {
    this.gatewayName = gatewayName;
    return this;
  }

  /**
   * Payment gateway.
   * @return gatewayName
   */
  @Size(max = 60) 
  @JsonProperty("gatewayName")
  public @Nullable String getGatewayName() {
    return gatewayName;
  }

  @JsonProperty("gatewayName")
  public void setGatewayName(@Nullable String gatewayName) {
    this.gatewayName = gatewayName;
  }

  public Payment gatewayOrderId(@Nullable String gatewayOrderId) {
    this.gatewayOrderId = gatewayOrderId;
    return this;
  }

  /**
   * Gateway order id.
   * @return gatewayOrderId
   */
  @Size(max = 120) 
  @JsonProperty("gatewayOrderId")
  public @Nullable String getGatewayOrderId() {
    return gatewayOrderId;
  }

  @JsonProperty("gatewayOrderId")
  public void setGatewayOrderId(@Nullable String gatewayOrderId) {
    this.gatewayOrderId = gatewayOrderId;
  }

  public Payment gatewayPaymentId(@Nullable String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
    return this;
  }

  /**
   * Gateway payment id.
   * @return gatewayPaymentId
   */
  @Size(max = 120) 
  @JsonProperty("gatewayPaymentId")
  public @Nullable String getGatewayPaymentId() {
    return gatewayPaymentId;
  }

  @JsonProperty("gatewayPaymentId")
  public void setGatewayPaymentId(@Nullable String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
  }

  public Payment originalAmount(@Nullable String originalAmount) {
    this.originalAmount = originalAmount;
    return this;
  }

  /**
   * Amount before discount.
   * @return originalAmount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("originalAmount")
  public @Nullable String getOriginalAmount() {
    return originalAmount;
  }

  @JsonProperty("originalAmount")
  public void setOriginalAmount(@Nullable String originalAmount) {
    this.originalAmount = originalAmount;
  }

  public Payment discountAmount(@Nullable String discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  /**
   * Discount.
   * @return discountAmount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("discountAmount")
  public @Nullable String getDiscountAmount() {
    return discountAmount;
  }

  @JsonProperty("discountAmount")
  public void setDiscountAmount(@Nullable String discountAmount) {
    this.discountAmount = discountAmount;
  }

  public Payment amount(String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Amount payable before tax.
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

  public Payment amountWithTax(@Nullable String amountWithTax) {
    this.amountWithTax = amountWithTax;
    return this;
  }

  /**
   * Amount including tax.
   * @return amountWithTax
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("amountWithTax")
  public @Nullable String getAmountWithTax() {
    return amountWithTax;
  }

  @JsonProperty("amountWithTax")
  public void setAmountWithTax(@Nullable String amountWithTax) {
    this.amountWithTax = amountWithTax;
  }

  public Payment currencyId(UUID currencyId) {
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

  public Payment couponId(@Nullable UUID couponId) {
    this.couponId = couponId;
    return this;
  }

  /**
   * Coupon used.
   * @return couponId
   */
  @Valid 
  @JsonProperty("couponId")
  public @Nullable UUID getCouponId() {
    return couponId;
  }

  @JsonProperty("couponId")
  public void setCouponId(@Nullable UUID couponId) {
    this.couponId = couponId;
  }

  public Payment couponCode(@Nullable String couponCode) {
    this.couponCode = couponCode;
    return this;
  }

  /**
   * Coupon code entered.
   * @return couponCode
   */
  @Size(max = 40) 
  @JsonProperty("couponCode")
  public @Nullable String getCouponCode() {
    return couponCode;
  }

  @JsonProperty("couponCode")
  public void setCouponCode(@Nullable String couponCode) {
    this.couponCode = couponCode;
  }

  public Payment billingName(@Nullable String billingName) {
    this.billingName = billingName;
    return this;
  }

  /**
   * Billing name.
   * @return billingName
   */
  @Size(max = 200) 
  @JsonProperty("billingName")
  public @Nullable String getBillingName() {
    return billingName;
  }

  @JsonProperty("billingName")
  public void setBillingName(@Nullable String billingName) {
    this.billingName = billingName;
  }

  public Payment billingAddressId(@Nullable UUID billingAddressId) {
    this.billingAddressId = billingAddressId;
    return this;
  }

  /**
   * Billing address.
   * @return billingAddressId
   */
  @Valid 
  @JsonProperty("billingAddressId")
  public @Nullable UUID getBillingAddressId() {
    return billingAddressId;
  }

  @JsonProperty("billingAddressId")
  public void setBillingAddressId(@Nullable UUID billingAddressId) {
    this.billingAddressId = billingAddressId;
  }

  public Payment taxRegistrationNumber(@Nullable String taxRegistrationNumber) {
    this.taxRegistrationNumber = taxRegistrationNumber;
    return this;
  }

  /**
   * Buyer's tax registration (e.g. GSTIN).
   * @return taxRegistrationNumber
   */
  @Size(max = 30) 
  @JsonProperty("taxRegistrationNumber")
  public @Nullable String getTaxRegistrationNumber() {
    return taxRegistrationNumber;
  }

  @JsonProperty("taxRegistrationNumber")
  public void setTaxRegistrationNumber(@Nullable String taxRegistrationNumber) {
    this.taxRegistrationNumber = taxRegistrationNumber;
  }

  public Payment taxIdentity(@Nullable String taxIdentity) {
    this.taxIdentity = taxIdentity;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Buyer's tax identity number (e.g. PAN), encrypted.
   * @return taxIdentity
   */
  
  @JsonProperty("taxIdentity")
  public @Nullable String getTaxIdentity() {
    return taxIdentity;
  }

  @JsonProperty("taxIdentity")
  public void setTaxIdentity(@Nullable String taxIdentity) {
    this.taxIdentity = taxIdentity;
  }

  public Payment taxIdentityMasked(@Nullable String taxIdentityMasked) {
    this.taxIdentityMasked = taxIdentityMasked;
    return this;
  }

  /**
   * Masked form for display, e.g. \"••••1234\". Output only.
   * @return taxIdentityMasked
   */
  
  @JsonProperty("taxIdentityMasked")
  public @Nullable String getTaxIdentityMasked() {
    return taxIdentityMasked;
  }

  @JsonProperty("taxIdentityMasked")
  public void setTaxIdentityMasked(@Nullable String taxIdentityMasked) {
    this.taxIdentityMasked = taxIdentityMasked;
  }

  public Payment acquisitionSourceId(@Nullable UUID acquisitionSourceId) {
    this.acquisitionSourceId = acquisitionSourceId;
    return this;
  }

  /**
   * Marketing source.
   * @return acquisitionSourceId
   */
  @Valid 
  @JsonProperty("acquisitionSourceId")
  public @Nullable UUID getAcquisitionSourceId() {
    return acquisitionSourceId;
  }

  @JsonProperty("acquisitionSourceId")
  public void setAcquisitionSourceId(@Nullable UUID acquisitionSourceId) {
    this.acquisitionSourceId = acquisitionSourceId;
  }

  public Payment payerConsent(@Nullable Boolean payerConsent) {
    this.payerConsent = payerConsent;
    return this;
  }

  /**
   * Payer accepted terms.
   * @return payerConsent
   */
  
  @JsonProperty("payerConsent")
  public @Nullable Boolean getPayerConsent() {
    return payerConsent;
  }

  @JsonProperty("payerConsent")
  public void setPayerConsent(@Nullable Boolean payerConsent) {
    this.payerConsent = payerConsent;
  }

  public Payment status(@Nullable StatusEnum status) {
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

  public Payment receivedAt(@Nullable OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
    return this;
  }

  /**
   * When the money was received.
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

  public Payment createTime(@Nullable OffsetDateTime createTime) {
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

  public Payment createdBy(@Nullable UUID createdBy) {
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

  public Payment updateTime(@Nullable OffsetDateTime updateTime) {
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

  public Payment updatedBy(@Nullable UUID updatedBy) {
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

  public Payment etag(@Nullable String etag) {
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
    Payment payment = (Payment) o;
    return Objects.equals(this.allocations, payment.allocations) &&
        Objects.equals(this.instrumentDate, payment.instrumentDate) &&
        Objects.equals(this.bankName, payment.bankName) &&
        Objects.equals(this.gatewayAccountId, payment.gatewayAccountId) &&
        Objects.equals(this.receivedByUserId, payment.receivedByUserId) &&
        Objects.equals(this.instrumentReference, payment.instrumentReference) &&
        Objects.equals(this.payerName, payment.payerName) &&
        Objects.equals(this.payerPersonId, payment.payerPersonId) &&
        Objects.equals(this.receiptNumber, payment.receiptNumber) &&
        Objects.equals(this.paymentMethod, payment.paymentMethod) &&
        Objects.equals(this.id, payment.id) &&
        Objects.equals(this.institutionId, payment.institutionId) &&
        Objects.equals(this.payerUserId, payment.payerUserId) &&
        Objects.equals(this.purpose, payment.purpose) &&
        Objects.equals(this.invoiceId, payment.invoiceId) &&
        Objects.equals(this.lmsCourseId, payment.lmsCourseId) &&
        Objects.equals(this.lmsCohortId, payment.lmsCohortId) &&
        Objects.equals(this.admissionApplicationId, payment.admissionApplicationId) &&
        Objects.equals(this.gatewayName, payment.gatewayName) &&
        Objects.equals(this.gatewayOrderId, payment.gatewayOrderId) &&
        Objects.equals(this.gatewayPaymentId, payment.gatewayPaymentId) &&
        Objects.equals(this.originalAmount, payment.originalAmount) &&
        Objects.equals(this.discountAmount, payment.discountAmount) &&
        Objects.equals(this.amount, payment.amount) &&
        Objects.equals(this.amountWithTax, payment.amountWithTax) &&
        Objects.equals(this.currencyId, payment.currencyId) &&
        Objects.equals(this.couponId, payment.couponId) &&
        Objects.equals(this.couponCode, payment.couponCode) &&
        Objects.equals(this.billingName, payment.billingName) &&
        Objects.equals(this.billingAddressId, payment.billingAddressId) &&
        Objects.equals(this.taxRegistrationNumber, payment.taxRegistrationNumber) &&
        Objects.equals(this.taxIdentity, payment.taxIdentity) &&
        Objects.equals(this.taxIdentityMasked, payment.taxIdentityMasked) &&
        Objects.equals(this.acquisitionSourceId, payment.acquisitionSourceId) &&
        Objects.equals(this.payerConsent, payment.payerConsent) &&
        Objects.equals(this.status, payment.status) &&
        Objects.equals(this.receivedAt, payment.receivedAt) &&
        Objects.equals(this.createTime, payment.createTime) &&
        Objects.equals(this.createdBy, payment.createdBy) &&
        Objects.equals(this.updateTime, payment.updateTime) &&
        Objects.equals(this.updatedBy, payment.updatedBy) &&
        Objects.equals(this.etag, payment.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allocations, instrumentDate, bankName, gatewayAccountId, receivedByUserId, instrumentReference, payerName, payerPersonId, receiptNumber, paymentMethod, id, institutionId, payerUserId, purpose, invoiceId, lmsCourseId, lmsCohortId, admissionApplicationId, gatewayName, gatewayOrderId, gatewayPaymentId, originalAmount, discountAmount, amount, amountWithTax, currencyId, couponId, couponCode, billingName, billingAddressId, taxRegistrationNumber, taxIdentity, taxIdentityMasked, acquisitionSourceId, payerConsent, status, receivedAt, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Payment {\n");
    sb.append("    allocations: ").append(toIndentedString(allocations)).append("\n");
    sb.append("    instrumentDate: ").append(toIndentedString(instrumentDate)).append("\n");
    sb.append("    bankName: ").append(toIndentedString(bankName)).append("\n");
    sb.append("    gatewayAccountId: ").append(toIndentedString(gatewayAccountId)).append("\n");
    sb.append("    receivedByUserId: ").append(toIndentedString(receivedByUserId)).append("\n");
    sb.append("    instrumentReference: ").append(toIndentedString(instrumentReference)).append("\n");
    sb.append("    payerName: ").append(toIndentedString(payerName)).append("\n");
    sb.append("    payerPersonId: ").append(toIndentedString(payerPersonId)).append("\n");
    sb.append("    receiptNumber: ").append(toIndentedString(receiptNumber)).append("\n");
    sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    payerUserId: ").append(toIndentedString(payerUserId)).append("\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
    sb.append("    invoiceId: ").append(toIndentedString(invoiceId)).append("\n");
    sb.append("    lmsCourseId: ").append(toIndentedString(lmsCourseId)).append("\n");
    sb.append("    lmsCohortId: ").append(toIndentedString(lmsCohortId)).append("\n");
    sb.append("    admissionApplicationId: ").append(toIndentedString(admissionApplicationId)).append("\n");
    sb.append("    gatewayName: ").append(toIndentedString(gatewayName)).append("\n");
    sb.append("    gatewayOrderId: ").append(toIndentedString(gatewayOrderId)).append("\n");
    sb.append("    gatewayPaymentId: ").append(toIndentedString(gatewayPaymentId)).append("\n");
    sb.append("    originalAmount: ").append(toIndentedString(originalAmount)).append("\n");
    sb.append("    discountAmount: ").append(toIndentedString(discountAmount)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    amountWithTax: ").append(toIndentedString(amountWithTax)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    couponId: ").append(toIndentedString(couponId)).append("\n");
    sb.append("    couponCode: ").append(toIndentedString(couponCode)).append("\n");
    sb.append("    billingName: ").append(toIndentedString(billingName)).append("\n");
    sb.append("    billingAddressId: ").append(toIndentedString(billingAddressId)).append("\n");
    sb.append("    taxRegistrationNumber: ").append(toIndentedString(taxRegistrationNumber)).append("\n");
    sb.append("    taxIdentity: ").append(toIndentedString(taxIdentity)).append("\n");
    sb.append("    taxIdentityMasked: ").append(toIndentedString(taxIdentityMasked)).append("\n");
    sb.append("    acquisitionSourceId: ").append(toIndentedString(acquisitionSourceId)).append("\n");
    sb.append("    payerConsent: ").append(toIndentedString(payerConsent)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    receivedAt: ").append(toIndentedString(receivedAt)).append("\n");
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

