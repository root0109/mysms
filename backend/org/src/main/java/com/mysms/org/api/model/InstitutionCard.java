// Owned API contract of org.
package com.mysms.org.api.model;

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
 * InstitutionCard
 */

public class InstitutionCard {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tenantCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String city;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String logoFileUrl;

  public InstitutionCard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InstitutionCard(UUID institutionId, String tenantCode, String name) {
    this.institutionId = institutionId;
    this.tenantCode = tenantCode;
    this.name = name;
  }

  public InstitutionCard institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public InstitutionCard tenantCode(String tenantCode) {
    this.tenantCode = tenantCode;
    return this;
  }

  /**
   * Tenant code to sign in with.
   * @return tenantCode
   */
  @NotNull 
  @JsonProperty("tenantCode")
  public String getTenantCode() {
    return tenantCode;
  }

  @JsonProperty("tenantCode")
  public void setTenantCode(String tenantCode) {
    this.tenantCode = tenantCode;
  }

  public InstitutionCard name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public InstitutionCard city(@Nullable String city) {
    this.city = city;
    return this;
  }

  /**
   * City.
   * @return city
   */
  
  @JsonProperty("city")
  public @Nullable String getCity() {
    return city;
  }

  @JsonProperty("city")
  public void setCity(@Nullable String city) {
    this.city = city;
  }

  public InstitutionCard logoFileUrl(@Nullable String logoFileUrl) {
    this.logoFileUrl = logoFileUrl;
    return this;
  }

  /**
   * Logo.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstitutionCard institutionCard = (InstitutionCard) o;
    return Objects.equals(this.institutionId, institutionCard.institutionId) &&
        Objects.equals(this.tenantCode, institutionCard.tenantCode) &&
        Objects.equals(this.name, institutionCard.name) &&
        Objects.equals(this.city, institutionCard.city) &&
        Objects.equals(this.logoFileUrl, institutionCard.logoFileUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, tenantCode, name, city, logoFileUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InstitutionCard {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    tenantCode: ").append(toIndentedString(tenantCode)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    logoFileUrl: ").append(toIndentedString(logoFileUrl)).append("\n");
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

