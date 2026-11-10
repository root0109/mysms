// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

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
 * Fields of PaymentGatewayAccount that can be updated (partial update; see updateMask).
 */

public class PaymentGatewayAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String keySecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String webhookSecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String merchantId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String keyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isDefault;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public PaymentGatewayAccountUpdate keySecret(@Nullable String keySecret) {
    this.keySecret = keySecret;
    return this;
  }

  /**
   * API key secret; stored encrypted, never returned.
   * @return keySecret
   */
  
  @JsonProperty("keySecret")
  public @Nullable String getKeySecret() {
    return keySecret;
  }

  @JsonProperty("keySecret")
  public void setKeySecret(@Nullable String keySecret) {
    this.keySecret = keySecret;
  }

  public PaymentGatewayAccountUpdate webhookSecret(@Nullable String webhookSecret) {
    this.webhookSecret = webhookSecret;
    return this;
  }

  /**
   * Webhook signing secret; stored encrypted, never returned.
   * @return webhookSecret
   */
  
  @JsonProperty("webhookSecret")
  public @Nullable String getWebhookSecret() {
    return webhookSecret;
  }

  @JsonProperty("webhookSecret")
  public void setWebhookSecret(@Nullable String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public PaymentGatewayAccountUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (absent = tenant-wide).
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

  public PaymentGatewayAccountUpdate gatewayName(@Nullable String gatewayName) {
    this.gatewayName = gatewayName;
    return this;
  }

  /**
   * Gateway identifier (razorpay, stripe, payu...).
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

  public PaymentGatewayAccountUpdate displayName(@Nullable String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Admin-facing name of this account.
   * @return displayName
   */
  @Size(max = 150) 
  @JsonProperty("displayName")
  public @Nullable String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
  }

  public PaymentGatewayAccountUpdate merchantId(@Nullable String merchantId) {
    this.merchantId = merchantId;
    return this;
  }

  /**
   * Merchant/account id at the gateway.
   * @return merchantId
   */
  @Size(max = 120) 
  @JsonProperty("merchantId")
  public @Nullable String getMerchantId() {
    return merchantId;
  }

  @JsonProperty("merchantId")
  public void setMerchantId(@Nullable String merchantId) {
    this.merchantId = merchantId;
  }

  public PaymentGatewayAccountUpdate keyId(@Nullable String keyId) {
    this.keyId = keyId;
    return this;
  }

  /**
   * API key id (public part).
   * @return keyId
   */
  @Size(max = 120) 
  @JsonProperty("keyId")
  public @Nullable String getKeyId() {
    return keyId;
  }

  @JsonProperty("keyId")
  public void setKeyId(@Nullable String keyId) {
    this.keyId = keyId;
  }

  public PaymentGatewayAccountUpdate currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Settlement currency.
   * @return currencyId
   */
  @Valid 
  @JsonProperty("currencyId")
  public @Nullable UUID getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
  }

  public PaymentGatewayAccountUpdate isDefault(@Nullable Boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  /**
   * Default account for the tenant/institution.
   * @return isDefault
   */
  
  @JsonProperty("isDefault")
  public @Nullable Boolean getIsDefault() {
    return isDefault;
  }

  @JsonProperty("isDefault")
  public void setIsDefault(@Nullable Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public PaymentGatewayAccountUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable for new payments.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaymentGatewayAccountUpdate paymentGatewayAccountUpdate = (PaymentGatewayAccountUpdate) o;
    return Objects.equals(this.keySecret, paymentGatewayAccountUpdate.keySecret) &&
        Objects.equals(this.webhookSecret, paymentGatewayAccountUpdate.webhookSecret) &&
        Objects.equals(this.institutionId, paymentGatewayAccountUpdate.institutionId) &&
        Objects.equals(this.gatewayName, paymentGatewayAccountUpdate.gatewayName) &&
        Objects.equals(this.displayName, paymentGatewayAccountUpdate.displayName) &&
        Objects.equals(this.merchantId, paymentGatewayAccountUpdate.merchantId) &&
        Objects.equals(this.keyId, paymentGatewayAccountUpdate.keyId) &&
        Objects.equals(this.currencyId, paymentGatewayAccountUpdate.currencyId) &&
        Objects.equals(this.isDefault, paymentGatewayAccountUpdate.isDefault) &&
        Objects.equals(this.isActive, paymentGatewayAccountUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keySecret, webhookSecret, institutionId, gatewayName, displayName, merchantId, keyId, currencyId, isDefault, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaymentGatewayAccountUpdate {\n");
    sb.append("    keySecret: ").append(toIndentedString(keySecret)).append("\n");
    sb.append("    webhookSecret: ").append(toIndentedString(webhookSecret)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    gatewayName: ").append(toIndentedString(gatewayName)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    merchantId: ").append(toIndentedString(merchantId)).append("\n");
    sb.append("    keyId: ").append(toIndentedString(keyId)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    isDefault: ").append(toIndentedString(isDefault)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

