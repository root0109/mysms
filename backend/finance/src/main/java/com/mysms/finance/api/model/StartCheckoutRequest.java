// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * StartCheckoutRequest
 */

public class StartCheckoutRequest {

  /**
   * What is bought.
   */
  public enum PurposeEnum {
    INVOICE("invoice"),
    
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
  private UUID targetId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String couponCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String billingName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID billingAddressId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String taxRegistrationNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String taxIdentity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean payerConsent;

  public StartCheckoutRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StartCheckoutRequest(PurposeEnum purpose, UUID targetId, String billingName, Boolean payerConsent) {
    this.purpose = purpose;
    this.targetId = targetId;
    this.billingName = billingName;
    this.payerConsent = payerConsent;
  }

  public StartCheckoutRequest purpose(PurposeEnum purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * What is bought.
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

  public StartCheckoutRequest targetId(UUID targetId) {
    this.targetId = targetId;
    return this;
  }

  /**
   * Target.
   * @return targetId
   */
  @NotNull @Valid 
  @JsonProperty("targetId")
  public UUID getTargetId() {
    return targetId;
  }

  @JsonProperty("targetId")
  public void setTargetId(UUID targetId) {
    this.targetId = targetId;
  }

  public StartCheckoutRequest couponCode(@Nullable String couponCode) {
    this.couponCode = couponCode;
    return this;
  }

  /**
   * Coupon.
   * @return couponCode
   */
  
  @JsonProperty("couponCode")
  public @Nullable String getCouponCode() {
    return couponCode;
  }

  @JsonProperty("couponCode")
  public void setCouponCode(@Nullable String couponCode) {
    this.couponCode = couponCode;
  }

  public StartCheckoutRequest billingName(String billingName) {
    this.billingName = billingName;
    return this;
  }

  /**
   * Billing name.
   * @return billingName
   */
  @NotNull 
  @JsonProperty("billingName")
  public String getBillingName() {
    return billingName;
  }

  @JsonProperty("billingName")
  public void setBillingName(String billingName) {
    this.billingName = billingName;
  }

  public StartCheckoutRequest billingAddressId(@Nullable UUID billingAddressId) {
    this.billingAddressId = billingAddressId;
    return this;
  }

  /**
   * Address.
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

  public StartCheckoutRequest taxRegistrationNumber(@Nullable String taxRegistrationNumber) {
    this.taxRegistrationNumber = taxRegistrationNumber;
    return this;
  }

  /**
   * GSTIN etc.
   * @return taxRegistrationNumber
   */
  
  @JsonProperty("taxRegistrationNumber")
  public @Nullable String getTaxRegistrationNumber() {
    return taxRegistrationNumber;
  }

  @JsonProperty("taxRegistrationNumber")
  public void setTaxRegistrationNumber(@Nullable String taxRegistrationNumber) {
    this.taxRegistrationNumber = taxRegistrationNumber;
  }

  public StartCheckoutRequest taxIdentity(@Nullable String taxIdentity) {
    this.taxIdentity = taxIdentity;
    return this;
  }

  /**
   * PAN etc.
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

  public StartCheckoutRequest payerConsent(Boolean payerConsent) {
    this.payerConsent = payerConsent;
    return this;
  }

  /**
   * Terms accepted.
   * @return payerConsent
   */
  @NotNull 
  @JsonProperty("payerConsent")
  public Boolean getPayerConsent() {
    return payerConsent;
  }

  @JsonProperty("payerConsent")
  public void setPayerConsent(Boolean payerConsent) {
    this.payerConsent = payerConsent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartCheckoutRequest startCheckoutRequest = (StartCheckoutRequest) o;
    return Objects.equals(this.purpose, startCheckoutRequest.purpose) &&
        Objects.equals(this.targetId, startCheckoutRequest.targetId) &&
        Objects.equals(this.couponCode, startCheckoutRequest.couponCode) &&
        Objects.equals(this.billingName, startCheckoutRequest.billingName) &&
        Objects.equals(this.billingAddressId, startCheckoutRequest.billingAddressId) &&
        Objects.equals(this.taxRegistrationNumber, startCheckoutRequest.taxRegistrationNumber) &&
        Objects.equals(this.taxIdentity, startCheckoutRequest.taxIdentity) &&
        Objects.equals(this.payerConsent, startCheckoutRequest.payerConsent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purpose, targetId, couponCode, billingName, billingAddressId, taxRegistrationNumber, taxIdentity, payerConsent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartCheckoutRequest {\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
    sb.append("    targetId: ").append(toIndentedString(targetId)).append("\n");
    sb.append("    couponCode: ").append(toIndentedString(couponCode)).append("\n");
    sb.append("    billingName: ").append(toIndentedString(billingName)).append("\n");
    sb.append("    billingAddressId: ").append(toIndentedString(billingAddressId)).append("\n");
    sb.append("    taxRegistrationNumber: ").append(toIndentedString(taxRegistrationNumber)).append("\n");
    sb.append("    taxIdentity: ").append(toIndentedString(taxIdentity)).append("\n");
    sb.append("    payerConsent: ").append(toIndentedString(payerConsent)).append("\n");
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

