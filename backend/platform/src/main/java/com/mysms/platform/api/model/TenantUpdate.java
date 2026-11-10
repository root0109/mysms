// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

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
 * Fields of Tenant that can be updated (partial update; see updateMask).
 */

public class TenantUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String legalName;

  /**
   * Tenant lifecycle state.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    SUSPENDED("suspended"),
    
    CLOSED("closed");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String primaryEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String primaryPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultCurrencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String locale;

  public TenantUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Short unique tenant code used in URLs and logins.
   * @return code
   */
  @Size(max = 50) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public TenantUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Display name.
   * @return name
   */
  @Size(max = 200) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public TenantUpdate legalName(@Nullable String legalName) {
    this.legalName = legalName;
    return this;
  }

  /**
   * Registered legal name.
   * @return legalName
   */
  @Size(max = 200) 
  @JsonProperty("legalName")
  public @Nullable String getLegalName() {
    return legalName;
  }

  @JsonProperty("legalName")
  public void setLegalName(@Nullable String legalName) {
    this.legalName = legalName;
  }

  public TenantUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Tenant lifecycle state.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public TenantUpdate primaryEmail(@Nullable String primaryEmail) {
    this.primaryEmail = primaryEmail;
    return this;
  }

  /**
   * Main contact e-mail.
   * @return primaryEmail
   */
  @Size(max = 320) 
  @JsonProperty("primaryEmail")
  public @Nullable String getPrimaryEmail() {
    return primaryEmail;
  }

  @JsonProperty("primaryEmail")
  public void setPrimaryEmail(@Nullable String primaryEmail) {
    this.primaryEmail = primaryEmail;
  }

  public TenantUpdate primaryPhone(@Nullable String primaryPhone) {
    this.primaryPhone = primaryPhone;
    return this;
  }

  /**
   * Main contact phone.
   * @return primaryPhone
   */
  @Size(max = 20) 
  @JsonProperty("primaryPhone")
  public @Nullable String getPrimaryPhone() {
    return primaryPhone;
  }

  @JsonProperty("primaryPhone")
  public void setPrimaryPhone(@Nullable String primaryPhone) {
    this.primaryPhone = primaryPhone;
  }

  public TenantUpdate countryId(@Nullable UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country of registration.
   * @return countryId
   */
  @Valid 
  @JsonProperty("countryId")
  public @Nullable UUID getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable UUID countryId) {
    this.countryId = countryId;
  }

  public TenantUpdate defaultCurrencyId(@Nullable UUID defaultCurrencyId) {
    this.defaultCurrencyId = defaultCurrencyId;
    return this;
  }

  /**
   * Default currency for fees and payments.
   * @return defaultCurrencyId
   */
  @Valid 
  @JsonProperty("defaultCurrencyId")
  public @Nullable UUID getDefaultCurrencyId() {
    return defaultCurrencyId;
  }

  @JsonProperty("defaultCurrencyId")
  public void setDefaultCurrencyId(@Nullable UUID defaultCurrencyId) {
    this.defaultCurrencyId = defaultCurrencyId;
  }

  public TenantUpdate timezone(@Nullable String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * IANA time zone, e.g. Asia/Kolkata.
   * @return timezone
   */
  @Size(max = 64) 
  @JsonProperty("timezone")
  public @Nullable String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable String timezone) {
    this.timezone = timezone;
  }

  public TenantUpdate locale(@Nullable String locale) {
    this.locale = locale;
    return this;
  }

  /**
   * Default locale.
   * @return locale
   */
  @Size(max = 10) 
  @JsonProperty("locale")
  public @Nullable String getLocale() {
    return locale;
  }

  @JsonProperty("locale")
  public void setLocale(@Nullable String locale) {
    this.locale = locale;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TenantUpdate tenantUpdate = (TenantUpdate) o;
    return Objects.equals(this.code, tenantUpdate.code) &&
        Objects.equals(this.name, tenantUpdate.name) &&
        Objects.equals(this.legalName, tenantUpdate.legalName) &&
        Objects.equals(this.status, tenantUpdate.status) &&
        Objects.equals(this.primaryEmail, tenantUpdate.primaryEmail) &&
        Objects.equals(this.primaryPhone, tenantUpdate.primaryPhone) &&
        Objects.equals(this.countryId, tenantUpdate.countryId) &&
        Objects.equals(this.defaultCurrencyId, tenantUpdate.defaultCurrencyId) &&
        Objects.equals(this.timezone, tenantUpdate.timezone) &&
        Objects.equals(this.locale, tenantUpdate.locale);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, name, legalName, status, primaryEmail, primaryPhone, countryId, defaultCurrencyId, timezone, locale);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    legalName: ").append(toIndentedString(legalName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    primaryEmail: ").append(toIndentedString(primaryEmail)).append("\n");
    sb.append("    primaryPhone: ").append(toIndentedString(primaryPhone)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    defaultCurrencyId: ").append(toIndentedString(defaultCurrencyId)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    locale: ").append(toIndentedString(locale)).append("\n");
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

