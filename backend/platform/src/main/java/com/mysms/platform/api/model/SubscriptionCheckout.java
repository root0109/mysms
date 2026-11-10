// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

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
 * Checkout started for a subscription purchase. References the finance payment by id only.
 */

public class SubscriptionCheckout {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID paymentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String redirectUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayOrderId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayPublicKey;

  public SubscriptionCheckout() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SubscriptionCheckout(UUID paymentId) {
    this.paymentId = paymentId;
  }

  public SubscriptionCheckout paymentId(UUID paymentId) {
    this.paymentId = paymentId;
    return this;
  }

  /**
   * Payment row created in finance.
   * @return paymentId
   */
  @NotNull @Valid 
  @JsonProperty("paymentId")
  public UUID getPaymentId() {
    return paymentId;
  }

  @JsonProperty("paymentId")
  public void setPaymentId(UUID paymentId) {
    this.paymentId = paymentId;
  }

  public SubscriptionCheckout redirectUrl(@Nullable String redirectUrl) {
    this.redirectUrl = redirectUrl;
    return this;
  }

  /**
   * Hosted payment page.
   * @return redirectUrl
   */
  
  @JsonProperty("redirectUrl")
  public @Nullable String getRedirectUrl() {
    return redirectUrl;
  }

  @JsonProperty("redirectUrl")
  public void setRedirectUrl(@Nullable String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  public SubscriptionCheckout gatewayOrderId(@Nullable String gatewayOrderId) {
    this.gatewayOrderId = gatewayOrderId;
    return this;
  }

  /**
   * Gateway order id for client SDKs.
   * @return gatewayOrderId
   */
  
  @JsonProperty("gatewayOrderId")
  public @Nullable String getGatewayOrderId() {
    return gatewayOrderId;
  }

  @JsonProperty("gatewayOrderId")
  public void setGatewayOrderId(@Nullable String gatewayOrderId) {
    this.gatewayOrderId = gatewayOrderId;
  }

  public SubscriptionCheckout gatewayPublicKey(@Nullable String gatewayPublicKey) {
    this.gatewayPublicKey = gatewayPublicKey;
    return this;
  }

  /**
   * Publishable key for client SDKs.
   * @return gatewayPublicKey
   */
  
  @JsonProperty("gatewayPublicKey")
  public @Nullable String getGatewayPublicKey() {
    return gatewayPublicKey;
  }

  @JsonProperty("gatewayPublicKey")
  public void setGatewayPublicKey(@Nullable String gatewayPublicKey) {
    this.gatewayPublicKey = gatewayPublicKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionCheckout subscriptionCheckout = (SubscriptionCheckout) o;
    return Objects.equals(this.paymentId, subscriptionCheckout.paymentId) &&
        Objects.equals(this.redirectUrl, subscriptionCheckout.redirectUrl) &&
        Objects.equals(this.gatewayOrderId, subscriptionCheckout.gatewayOrderId) &&
        Objects.equals(this.gatewayPublicKey, subscriptionCheckout.gatewayPublicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(paymentId, redirectUrl, gatewayOrderId, gatewayPublicKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubscriptionCheckout {\n");
    sb.append("    paymentId: ").append(toIndentedString(paymentId)).append("\n");
    sb.append("    redirectUrl: ").append(toIndentedString(redirectUrl)).append("\n");
    sb.append("    gatewayOrderId: ").append(toIndentedString(gatewayOrderId)).append("\n");
    sb.append("    gatewayPublicKey: ").append(toIndentedString(gatewayPublicKey)).append("\n");
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

