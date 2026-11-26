// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ConferencingAccount that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ConferencingAccountUpdate {

  /**
   * Provider.
   */
  public enum ProviderEnum {
    ZOOM("zoom"),
    
    GOOGLE_MEET("google_meet");

    private final String value;

    ProviderEnum(String value) {
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
    public static ProviderEnum fromValue(String value) {
      for (ProviderEnum b : ProviderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ProviderEnum> provider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> accountName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> externalAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> clientId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> clientSecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> externalCalendarId;

  public ConferencingAccountUpdate provider(@Nullable Optional<ProviderEnum> provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Provider.
   * @return provider
   */
  
  @JsonProperty("provider")
  public @Nullable Optional<ProviderEnum> getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(@Nullable Optional<ProviderEnum> provider) {
    this.provider = provider;
  }

  public ConferencingAccountUpdate accountName(@Nullable Optional<String> accountName) {
    this.accountName = accountName;
    return this;
  }

  /**
   * Account label.
   * @return accountName
   */
  
  @JsonProperty("accountName")
  public @Nullable Optional<String> getAccountName() {
    return accountName;
  }

  @JsonProperty("accountName")
  public void setAccountName(@Nullable Optional<String> accountName) {
    this.accountName = accountName;
  }

  public ConferencingAccountUpdate ownerUserId(@Nullable Optional<UUID> ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Account owner.
   * @return ownerUserId
   */
  
  @JsonProperty("ownerUserId")
  public @Nullable Optional<UUID> getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable Optional<UUID> ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public ConferencingAccountUpdate isEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Whether usable.
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

  public ConferencingAccountUpdate externalAccountId(@Nullable Optional<String> externalAccountId) {
    this.externalAccountId = externalAccountId;
    return this;
  }

  /**
   * External provider account id (Zoom account id).
   * @return externalAccountId
   */
  
  @JsonProperty("externalAccountId")
  public @Nullable Optional<String> getExternalAccountId() {
    return externalAccountId;
  }

  @JsonProperty("externalAccountId")
  public void setExternalAccountId(@Nullable Optional<String> externalAccountId) {
    this.externalAccountId = externalAccountId;
  }

  public ConferencingAccountUpdate clientId(@Nullable Optional<String> clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * External OAuth client id issued by the provider.
   * @return clientId
   */
  
  @JsonProperty("clientId")
  public @Nullable Optional<String> getClientId() {
    return clientId;
  }

  @JsonProperty("clientId")
  public void setClientId(@Nullable Optional<String> clientId) {
    this.clientId = clientId;
  }

  public ConferencingAccountUpdate clientSecret(@Nullable Optional<String> clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. OAuth client secret, encrypted.
   * @return clientSecret
   */
  
  @JsonProperty("clientSecret")
  public @Nullable Optional<String> getClientSecret() {
    return clientSecret;
  }

  @JsonProperty("clientSecret")
  public void setClientSecret(@Nullable Optional<String> clientSecret) {
    this.clientSecret = clientSecret;
  }

  public ConferencingAccountUpdate externalCalendarId(@Nullable Optional<String> externalCalendarId) {
    this.externalCalendarId = externalCalendarId;
    return this;
  }

  /**
   * External Google Calendar id used for Meet events.
   * @return externalCalendarId
   */
  
  @JsonProperty("externalCalendarId")
  public @Nullable Optional<String> getExternalCalendarId() {
    return externalCalendarId;
  }

  @JsonProperty("externalCalendarId")
  public void setExternalCalendarId(@Nullable Optional<String> externalCalendarId) {
    this.externalCalendarId = externalCalendarId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConferencingAccountUpdate conferencingAccountUpdate = (ConferencingAccountUpdate) o;
    return Objects.equals(this.provider, conferencingAccountUpdate.provider) &&
        Objects.equals(this.accountName, conferencingAccountUpdate.accountName) &&
        Objects.equals(this.ownerUserId, conferencingAccountUpdate.ownerUserId) &&
        Objects.equals(this.isEnabled, conferencingAccountUpdate.isEnabled) &&
        Objects.equals(this.externalAccountId, conferencingAccountUpdate.externalAccountId) &&
        Objects.equals(this.clientId, conferencingAccountUpdate.clientId) &&
        Objects.equals(this.clientSecret, conferencingAccountUpdate.clientSecret) &&
        Objects.equals(this.externalCalendarId, conferencingAccountUpdate.externalCalendarId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, accountName, ownerUserId, isEnabled, externalAccountId, clientId, clientSecret, externalCalendarId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConferencingAccountUpdate {\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    accountName: ").append(toIndentedString(accountName)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    isEnabled: ").append(toIndentedString(isEnabled)).append("\n");
    sb.append("    externalAccountId: ").append(toIndentedString(externalAccountId)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    clientSecret: ").append(toIndentedString(clientSecret)).append("\n");
    sb.append("    externalCalendarId: ").append(toIndentedString(externalCalendarId)).append("\n");
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

