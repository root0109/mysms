// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.finance.api.model.Payment;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CheckoutSession
 */

public class CheckoutSession {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Payment payment;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String redirectUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayOrderId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayPublicKey;

  public CheckoutSession() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckoutSession(Payment payment) {
    this.payment = payment;
  }

  public CheckoutSession payment(Payment payment) {
    this.payment = payment;
    return this;
  }

  /**
   * Get payment
   * @return payment
   */
  @NotNull @Valid 
  @JsonProperty("payment")
  public Payment getPayment() {
    return payment;
  }

  @JsonProperty("payment")
  public void setPayment(Payment payment) {
    this.payment = payment;
  }

  public CheckoutSession redirectUrl(@Nullable String redirectUrl) {
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

  public CheckoutSession gatewayOrderId(@Nullable String gatewayOrderId) {
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

  public CheckoutSession gatewayPublicKey(@Nullable String gatewayPublicKey) {
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
    CheckoutSession checkoutSession = (CheckoutSession) o;
    return Objects.equals(this.payment, checkoutSession.payment) &&
        Objects.equals(this.redirectUrl, checkoutSession.redirectUrl) &&
        Objects.equals(this.gatewayOrderId, checkoutSession.gatewayOrderId) &&
        Objects.equals(this.gatewayPublicKey, checkoutSession.gatewayPublicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payment, redirectUrl, gatewayOrderId, gatewayPublicKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckoutSession {\n");
    sb.append("    payment: ").append(toIndentedString(payment)).append("\n");
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

