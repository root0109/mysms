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
 * QuoteCheckoutRequest
 */

public class QuoteCheckoutRequest {

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
  private @Nullable String countryCode;

  public QuoteCheckoutRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuoteCheckoutRequest(PurposeEnum purpose, UUID targetId) {
    this.purpose = purpose;
    this.targetId = targetId;
  }

  public QuoteCheckoutRequest purpose(PurposeEnum purpose) {
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

  public QuoteCheckoutRequest targetId(UUID targetId) {
    this.targetId = targetId;
    return this;
  }

  /**
   * Invoice / course / cohort / application.
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

  public QuoteCheckoutRequest couponCode(@Nullable String couponCode) {
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

  public QuoteCheckoutRequest countryCode(@Nullable String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  /**
   * Buyer country.
   * @return countryCode
   */
  
  @JsonProperty("countryCode")
  public @Nullable String getCountryCode() {
    return countryCode;
  }

  @JsonProperty("countryCode")
  public void setCountryCode(@Nullable String countryCode) {
    this.countryCode = countryCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuoteCheckoutRequest quoteCheckoutRequest = (QuoteCheckoutRequest) o;
    return Objects.equals(this.purpose, quoteCheckoutRequest.purpose) &&
        Objects.equals(this.targetId, quoteCheckoutRequest.targetId) &&
        Objects.equals(this.couponCode, quoteCheckoutRequest.couponCode) &&
        Objects.equals(this.countryCode, quoteCheckoutRequest.countryCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purpose, targetId, couponCode, countryCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuoteCheckoutRequest {\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
    sb.append("    targetId: ").append(toIndentedString(targetId)).append("\n");
    sb.append("    couponCode: ").append(toIndentedString(couponCode)).append("\n");
    sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
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

