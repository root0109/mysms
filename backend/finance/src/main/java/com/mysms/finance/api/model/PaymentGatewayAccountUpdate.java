// Owned API contract of finance.
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
 * Fields of PaymentGatewayAccount that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PaymentGatewayAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> keySecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> webhookSecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> gatewayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> merchantId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> keyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isDefault;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public PaymentGatewayAccountUpdate keySecret(@Nullable Optional<String> keySecret) {
    this.keySecret = keySecret;
    return this;
  }

  /**
   * API key secret; stored encrypted, never returned.
   * @return keySecret
   */
  
  @JsonProperty("keySecret")
  public @Nullable Optional<String> getKeySecret() {
    return keySecret;
  }

  @JsonProperty("keySecret")
  public void setKeySecret(@Nullable Optional<String> keySecret) {
    this.keySecret = keySecret;
  }

  public PaymentGatewayAccountUpdate webhookSecret(@Nullable Optional<String> webhookSecret) {
    this.webhookSecret = webhookSecret;
    return this;
  }

  /**
   * Webhook signing secret; stored encrypted, never returned.
   * @return webhookSecret
   */
  
  @JsonProperty("webhookSecret")
  public @Nullable Optional<String> getWebhookSecret() {
    return webhookSecret;
  }

  @JsonProperty("webhookSecret")
  public void setWebhookSecret(@Nullable Optional<String> webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public PaymentGatewayAccountUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (absent = tenant-wide).
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public PaymentGatewayAccountUpdate gatewayName(@Nullable Optional<String> gatewayName) {
    this.gatewayName = gatewayName;
    return this;
  }

  /**
   * Gateway identifier (razorpay, stripe, payu...).
   * @return gatewayName
   */
  
  @JsonProperty("gatewayName")
  public @Nullable Optional<String> getGatewayName() {
    return gatewayName;
  }

  @JsonProperty("gatewayName")
  public void setGatewayName(@Nullable Optional<String> gatewayName) {
    this.gatewayName = gatewayName;
  }

  public PaymentGatewayAccountUpdate displayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Admin-facing name of this account.
   * @return displayName
   */
  
  @JsonProperty("displayName")
  public @Nullable Optional<String> getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
  }

  public PaymentGatewayAccountUpdate merchantId(@Nullable Optional<String> merchantId) {
    this.merchantId = merchantId;
    return this;
  }

  /**
   * Merchant/account id at the gateway.
   * @return merchantId
   */
  
  @JsonProperty("merchantId")
  public @Nullable Optional<String> getMerchantId() {
    return merchantId;
  }

  @JsonProperty("merchantId")
  public void setMerchantId(@Nullable Optional<String> merchantId) {
    this.merchantId = merchantId;
  }

  public PaymentGatewayAccountUpdate keyId(@Nullable Optional<String> keyId) {
    this.keyId = keyId;
    return this;
  }

  /**
   * API key id (public part).
   * @return keyId
   */
  
  @JsonProperty("keyId")
  public @Nullable Optional<String> getKeyId() {
    return keyId;
  }

  @JsonProperty("keyId")
  public void setKeyId(@Nullable Optional<String> keyId) {
    this.keyId = keyId;
  }

  public PaymentGatewayAccountUpdate currencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Settlement currency.
   * @return currencyId
   */
  
  @JsonProperty("currencyId")
  public @Nullable Optional<UUID> getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
  }

  public PaymentGatewayAccountUpdate isDefault(@Nullable Optional<Boolean> isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  /**
   * Default account for the tenant/institution.
   * @return isDefault
   */
  
  @JsonProperty("isDefault")
  public @Nullable Optional<Boolean> getIsDefault() {
    return isDefault;
  }

  @JsonProperty("isDefault")
  public void setIsDefault(@Nullable Optional<Boolean> isDefault) {
    this.isDefault = isDefault;
  }

  public PaymentGatewayAccountUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable for new payments.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
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

