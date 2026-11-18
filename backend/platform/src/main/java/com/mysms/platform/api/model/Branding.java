// Owned API contract of platform.
package com.mysms.platform.api.model;

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
 * Branding
 */

public class Branding {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tenantName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String institutionName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String logoFileUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String primaryColor;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String locale;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  public Branding() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Branding(String tenantName) {
    this.tenantName = tenantName;
  }

  public Branding tenantName(String tenantName) {
    this.tenantName = tenantName;
    return this;
  }

  /**
   * Tenant name.
   * @return tenantName
   */
  @NotNull 
  @JsonProperty("tenantName")
  public String getTenantName() {
    return tenantName;
  }

  @JsonProperty("tenantName")
  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public Branding institutionName(@Nullable String institutionName) {
    this.institutionName = institutionName;
    return this;
  }

  /**
   * Institution name.
   * @return institutionName
   */
  
  @JsonProperty("institutionName")
  public @Nullable String getInstitutionName() {
    return institutionName;
  }

  @JsonProperty("institutionName")
  public void setInstitutionName(@Nullable String institutionName) {
    this.institutionName = institutionName;
  }

  public Branding logoFileUrl(@Nullable String logoFileUrl) {
    this.logoFileUrl = logoFileUrl;
    return this;
  }

  /**
   * Logo URL.
   * @return logoFileUrl
   */
  
  @JsonProperty("logoFileUrl")
  public @Nullable String getLogoFileUrl() {
    return logoFileUrl;
  }

  @JsonProperty("logoFileUrl")
  public void setLogoFileUrl(@Nullable String logoFileUrl) {
    this.logoFileUrl = logoFileUrl;
  }

  public Branding primaryColor(@Nullable String primaryColor) {
    this.primaryColor = primaryColor;
    return this;
  }

  /**
   * Brand colour.
   * @return primaryColor
   */
  
  @JsonProperty("primaryColor")
  public @Nullable String getPrimaryColor() {
    return primaryColor;
  }

  @JsonProperty("primaryColor")
  public void setPrimaryColor(@Nullable String primaryColor) {
    this.primaryColor = primaryColor;
  }

  public Branding locale(@Nullable String locale) {
    this.locale = locale;
    return this;
  }

  /**
   * Default locale.
   * @return locale
   */
  
  @JsonProperty("locale")
  public @Nullable String getLocale() {
    return locale;
  }

  @JsonProperty("locale")
  public void setLocale(@Nullable String locale) {
    this.locale = locale;
  }

  public Branding timezone(@Nullable String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Default time zone.
   * @return timezone
   */
  
  @JsonProperty("timezone")
  public @Nullable String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable String timezone) {
    this.timezone = timezone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Branding branding = (Branding) o;
    return Objects.equals(this.tenantName, branding.tenantName) &&
        Objects.equals(this.institutionName, branding.institutionName) &&
        Objects.equals(this.logoFileUrl, branding.logoFileUrl) &&
        Objects.equals(this.primaryColor, branding.primaryColor) &&
        Objects.equals(this.locale, branding.locale) &&
        Objects.equals(this.timezone, branding.timezone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantName, institutionName, logoFileUrl, primaryColor, locale, timezone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Branding {\n");
    sb.append("    tenantName: ").append(toIndentedString(tenantName)).append("\n");
    sb.append("    institutionName: ").append(toIndentedString(institutionName)).append("\n");
    sb.append("    logoFileUrl: ").append(toIndentedString(logoFileUrl)).append("\n");
    sb.append("    primaryColor: ").append(toIndentedString(primaryColor)).append("\n");
    sb.append("    locale: ").append(toIndentedString(locale)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
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

