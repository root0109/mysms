// Owned API contract of platform.
package com.mysms.platform.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/**
 * What the gateway's checkout hands the client after a successful payment.
 */

public class ConfirmSubscriptionCheckoutRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String gatewayOrderId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String gatewayPaymentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String gatewaySignature;

  public ConfirmSubscriptionCheckoutRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfirmSubscriptionCheckoutRequest(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature) {
    this.gatewayOrderId = gatewayOrderId;
    this.gatewayPaymentId = gatewayPaymentId;
    this.gatewaySignature = gatewaySignature;
  }

  public ConfirmSubscriptionCheckoutRequest gatewayOrderId(String gatewayOrderId) {
    this.gatewayOrderId = gatewayOrderId;
    return this;
  }

  /**
   * Gateway order id (razorpay_order_id).
   * @return gatewayOrderId
   */
  @NotNull @Size(max = 40) 
  @JsonProperty("gatewayOrderId")
  public String getGatewayOrderId() {
    return gatewayOrderId;
  }

  @JsonProperty("gatewayOrderId")
  public void setGatewayOrderId(String gatewayOrderId) {
    this.gatewayOrderId = gatewayOrderId;
  }

  public ConfirmSubscriptionCheckoutRequest gatewayPaymentId(String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
    return this;
  }

  /**
   * Gateway payment id (razorpay_payment_id).
   * @return gatewayPaymentId
   */
  @NotNull @Size(max = 40) 
  @JsonProperty("gatewayPaymentId")
  public String getGatewayPaymentId() {
    return gatewayPaymentId;
  }

  @JsonProperty("gatewayPaymentId")
  public void setGatewayPaymentId(String gatewayPaymentId) {
    this.gatewayPaymentId = gatewayPaymentId;
  }

  public ConfirmSubscriptionCheckoutRequest gatewaySignature(String gatewaySignature) {
    this.gatewaySignature = gatewaySignature;
    return this;
  }

  /**
   * Checkout signature (razorpay_signature).
   * @return gatewaySignature
   */
  @NotNull @Size(max = 128) 
  @JsonProperty("gatewaySignature")
  public String getGatewaySignature() {
    return gatewaySignature;
  }

  @JsonProperty("gatewaySignature")
  public void setGatewaySignature(String gatewaySignature) {
    this.gatewaySignature = gatewaySignature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfirmSubscriptionCheckoutRequest other = (ConfirmSubscriptionCheckoutRequest) o;
    return Objects.equals(this.gatewayOrderId, other.gatewayOrderId) &&
        Objects.equals(this.gatewayPaymentId, other.gatewayPaymentId) &&
        Objects.equals(this.gatewaySignature, other.gatewaySignature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gatewayOrderId, gatewayPaymentId, gatewaySignature);
  }

  @Override
  public String toString() {
    // the signature is a proof of payment: never printed
    return "class ConfirmSubscriptionCheckoutRequest {\n    gatewayOrderId: " + gatewayOrderId
        + "\n    gatewayPaymentId: " + gatewayPaymentId + "\n}";
  }
}
