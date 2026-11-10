// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * OrderQuote
 */

public class OrderQuote {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String originalAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String discountAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String taxAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String totalAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String currencyCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean couponApplied;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String couponMessage;

  public OrderQuote() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OrderQuote(String originalAmount, String discountAmount, String taxAmount, String totalAmount, String currencyCode, Boolean couponApplied) {
    this.originalAmount = originalAmount;
    this.discountAmount = discountAmount;
    this.taxAmount = taxAmount;
    this.totalAmount = totalAmount;
    this.currencyCode = currencyCode;
    this.couponApplied = couponApplied;
  }

  public OrderQuote originalAmount(String originalAmount) {
    this.originalAmount = originalAmount;
    return this;
  }

  /**
   * List price.
   * @return originalAmount
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("originalAmount")
  public String getOriginalAmount() {
    return originalAmount;
  }

  @JsonProperty("originalAmount")
  public void setOriginalAmount(String originalAmount) {
    this.originalAmount = originalAmount;
  }

  public OrderQuote discountAmount(String discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  /**
   * Coupon discount.
   * @return discountAmount
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("discountAmount")
  public String getDiscountAmount() {
    return discountAmount;
  }

  @JsonProperty("discountAmount")
  public void setDiscountAmount(String discountAmount) {
    this.discountAmount = discountAmount;
  }

  public OrderQuote taxAmount(String taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  /**
   * Tax.
   * @return taxAmount
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("taxAmount")
  public String getTaxAmount() {
    return taxAmount;
  }

  @JsonProperty("taxAmount")
  public void setTaxAmount(String taxAmount) {
    this.taxAmount = taxAmount;
  }

  public OrderQuote totalAmount(String totalAmount) {
    this.totalAmount = totalAmount;
    return this;
  }

  /**
   * Payable.
   * @return totalAmount
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalAmount")
  public String getTotalAmount() {
    return totalAmount;
  }

  @JsonProperty("totalAmount")
  public void setTotalAmount(String totalAmount) {
    this.totalAmount = totalAmount;
  }

  public OrderQuote currencyCode(String currencyCode) {
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

  public OrderQuote couponApplied(Boolean couponApplied) {
    this.couponApplied = couponApplied;
    return this;
  }

  /**
   * Coupon valid and applied.
   * @return couponApplied
   */
  @NotNull 
  @JsonProperty("couponApplied")
  public Boolean getCouponApplied() {
    return couponApplied;
  }

  @JsonProperty("couponApplied")
  public void setCouponApplied(Boolean couponApplied) {
    this.couponApplied = couponApplied;
  }

  public OrderQuote couponMessage(@Nullable String couponMessage) {
    this.couponMessage = couponMessage;
    return this;
  }

  /**
   * Why a coupon was rejected.
   * @return couponMessage
   */
  
  @JsonProperty("couponMessage")
  public @Nullable String getCouponMessage() {
    return couponMessage;
  }

  @JsonProperty("couponMessage")
  public void setCouponMessage(@Nullable String couponMessage) {
    this.couponMessage = couponMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrderQuote orderQuote = (OrderQuote) o;
    return Objects.equals(this.originalAmount, orderQuote.originalAmount) &&
        Objects.equals(this.discountAmount, orderQuote.discountAmount) &&
        Objects.equals(this.taxAmount, orderQuote.taxAmount) &&
        Objects.equals(this.totalAmount, orderQuote.totalAmount) &&
        Objects.equals(this.currencyCode, orderQuote.currencyCode) &&
        Objects.equals(this.couponApplied, orderQuote.couponApplied) &&
        Objects.equals(this.couponMessage, orderQuote.couponMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(originalAmount, discountAmount, taxAmount, totalAmount, currencyCode, couponApplied, couponMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrderQuote {\n");
    sb.append("    originalAmount: ").append(toIndentedString(originalAmount)).append("\n");
    sb.append("    discountAmount: ").append(toIndentedString(discountAmount)).append("\n");
    sb.append("    taxAmount: ").append(toIndentedString(taxAmount)).append("\n");
    sb.append("    totalAmount: ").append(toIndentedString(totalAmount)).append("\n");
    sb.append("    currencyCode: ").append(toIndentedString(currencyCode)).append("\n");
    sb.append("    couponApplied: ").append(toIndentedString(couponApplied)).append("\n");
    sb.append("    couponMessage: ").append(toIndentedString(couponMessage)).append("\n");
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

