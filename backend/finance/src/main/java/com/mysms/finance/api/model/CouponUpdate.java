// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Coupon that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CouponUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> code;

  /**
   * Discount type.
   */
  public enum DiscountTypeEnum {
    PERCENTAGE("percentage"),
    
    FIXED_AMOUNT("fixed_amount");

    private final String value;

    DiscountTypeEnum(String value) {
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
    public static DiscountTypeEnum fromValue(String value) {
      for (DiscountTypeEnum b : DiscountTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull DiscountTypeEnum> discountType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> percentageDiscount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> fixedDiscount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> expiresOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> usageLimit;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> redemptionCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isEnabled;

  public CouponUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Coupon code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public CouponUpdate discountType(@Nullable Optional<DiscountTypeEnum> discountType) {
    this.discountType = discountType;
    return this;
  }

  /**
   * Discount type.
   * @return discountType
   */
  
  @JsonProperty("discountType")
  public @Nullable Optional<DiscountTypeEnum> getDiscountType() {
    return discountType;
  }

  @JsonProperty("discountType")
  public void setDiscountType(@Nullable Optional<DiscountTypeEnum> discountType) {
    this.discountType = discountType;
  }

  public CouponUpdate percentageDiscount(@Nullable Optional<String> percentageDiscount) {
    this.percentageDiscount = percentageDiscount;
    return this;
  }

  /**
   * Percentage off.
   * @return percentageDiscount
   */
  
  @JsonProperty("percentageDiscount")
  public @Nullable Optional<String> getPercentageDiscount() {
    return percentageDiscount;
  }

  @JsonProperty("percentageDiscount")
  public void setPercentageDiscount(@Nullable Optional<String> percentageDiscount) {
    this.percentageDiscount = percentageDiscount;
  }

  public CouponUpdate fixedDiscount(@Nullable Optional<String> fixedDiscount) {
    this.fixedDiscount = fixedDiscount;
    return this;
  }

  /**
   * Amount off.
   * @return fixedDiscount
   */
  
  @JsonProperty("fixedDiscount")
  public @Nullable Optional<String> getFixedDiscount() {
    return fixedDiscount;
  }

  @JsonProperty("fixedDiscount")
  public void setFixedDiscount(@Nullable Optional<String> fixedDiscount) {
    this.fixedDiscount = fixedDiscount;
  }

  public CouponUpdate expiresOn(@Nullable Optional<LocalDate> expiresOn) {
    this.expiresOn = expiresOn;
    return this;
  }

  /**
   * Expiry.
   * @return expiresOn
   */
  
  @JsonProperty("expiresOn")
  public @Nullable Optional<LocalDate> getExpiresOn() {
    return expiresOn;
  }

  @JsonProperty("expiresOn")
  public void setExpiresOn(@Nullable Optional<LocalDate> expiresOn) {
    this.expiresOn = expiresOn;
  }

  public CouponUpdate usageLimit(@Nullable Optional<Integer> usageLimit) {
    this.usageLimit = usageLimit;
    return this;
  }

  /**
   * Maximum redemptions.
   * @return usageLimit
   */
  
  @JsonProperty("usageLimit")
  public @Nullable Optional<Integer> getUsageLimit() {
    return usageLimit;
  }

  @JsonProperty("usageLimit")
  public void setUsageLimit(@Nullable Optional<Integer> usageLimit) {
    this.usageLimit = usageLimit;
  }

  public CouponUpdate redemptionCount(@Nullable Optional<Integer> redemptionCount) {
    this.redemptionCount = redemptionCount;
    return this;
  }

  /**
   * Redemptions so far.
   * @return redemptionCount
   */
  
  @JsonProperty("redemptionCount")
  public @Nullable Optional<Integer> getRedemptionCount() {
    return redemptionCount;
  }

  @JsonProperty("redemptionCount")
  public void setRedemptionCount(@Nullable Optional<Integer> redemptionCount) {
    this.redemptionCount = redemptionCount;
  }

  public CouponUpdate isEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Whether active.
   * @return isEnabled
   */
  
  @JsonProperty("isEnabled")
  public @Nullable Optional<Boolean> getIsEnabled() {
    return isEnabled;
  }

  @JsonProperty("isEnabled")
  public void setIsEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CouponUpdate couponUpdate = (CouponUpdate) o;
    return Objects.equals(this.code, couponUpdate.code) &&
        Objects.equals(this.discountType, couponUpdate.discountType) &&
        Objects.equals(this.percentageDiscount, couponUpdate.percentageDiscount) &&
        Objects.equals(this.fixedDiscount, couponUpdate.fixedDiscount) &&
        Objects.equals(this.expiresOn, couponUpdate.expiresOn) &&
        Objects.equals(this.usageLimit, couponUpdate.usageLimit) &&
        Objects.equals(this.redemptionCount, couponUpdate.redemptionCount) &&
        Objects.equals(this.isEnabled, couponUpdate.isEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, discountType, percentageDiscount, fixedDiscount, expiresOn, usageLimit, redemptionCount, isEnabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CouponUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    discountType: ").append(toIndentedString(discountType)).append("\n");
    sb.append("    percentageDiscount: ").append(toIndentedString(percentageDiscount)).append("\n");
    sb.append("    fixedDiscount: ").append(toIndentedString(fixedDiscount)).append("\n");
    sb.append("    expiresOn: ").append(toIndentedString(expiresOn)).append("\n");
    sb.append("    usageLimit: ").append(toIndentedString(usageLimit)).append("\n");
    sb.append("    redemptionCount: ").append(toIndentedString(redemptionCount)).append("\n");
    sb.append("    isEnabled: ").append(toIndentedString(isEnabled)).append("\n");
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

