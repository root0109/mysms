// Owned API contract of people.
package com.mysms.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Certification that can be updated (partial update; see updateMask).
 */

public class CertificationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String issuingOrganization;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate issueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean expires;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate expirationDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  public CertificationUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Certification name.
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

  public CertificationUpdate issuingOrganization(@Nullable String issuingOrganization) {
    this.issuingOrganization = issuingOrganization;
    return this;
  }

  /**
   * Issuer.
   * @return issuingOrganization
   */
  @Size(max = 200) 
  @JsonProperty("issuingOrganization")
  public @Nullable String getIssuingOrganization() {
    return issuingOrganization;
  }

  @JsonProperty("issuingOrganization")
  public void setIssuingOrganization(@Nullable String issuingOrganization) {
    this.issuingOrganization = issuingOrganization;
  }

  public CertificationUpdate issueDate(@Nullable LocalDate issueDate) {
    this.issueDate = issueDate;
    return this;
  }

  /**
   * Issue date.
   * @return issueDate
   */
  @Valid 
  @JsonProperty("issueDate")
  public @Nullable LocalDate getIssueDate() {
    return issueDate;
  }

  @JsonProperty("issueDate")
  public void setIssueDate(@Nullable LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public CertificationUpdate expires(@Nullable Boolean expires) {
    this.expires = expires;
    return this;
  }

  /**
   * Whether it expires.
   * @return expires
   */
  
  @JsonProperty("expires")
  public @Nullable Boolean getExpires() {
    return expires;
  }

  @JsonProperty("expires")
  public void setExpires(@Nullable Boolean expires) {
    this.expires = expires;
  }

  public CertificationUpdate expirationDate(@Nullable LocalDate expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  /**
   * Expiry date.
   * @return expirationDate
   */
  @Valid 
  @JsonProperty("expirationDate")
  public @Nullable LocalDate getExpirationDate() {
    return expirationDate;
  }

  @JsonProperty("expirationDate")
  public void setExpirationDate(@Nullable LocalDate expirationDate) {
    this.expirationDate = expirationDate;
  }

  public CertificationUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Details.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CertificationUpdate certificationUpdate = (CertificationUpdate) o;
    return Objects.equals(this.name, certificationUpdate.name) &&
        Objects.equals(this.issuingOrganization, certificationUpdate.issuingOrganization) &&
        Objects.equals(this.issueDate, certificationUpdate.issueDate) &&
        Objects.equals(this.expires, certificationUpdate.expires) &&
        Objects.equals(this.expirationDate, certificationUpdate.expirationDate) &&
        Objects.equals(this.description, certificationUpdate.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, issuingOrganization, issueDate, expires, expirationDate, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CertificationUpdate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    issuingOrganization: ").append(toIndentedString(issuingOrganization)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
    sb.append("    expires: ").append(toIndentedString(expires)).append("\n");
    sb.append("    expirationDate: ").append(toIndentedString(expirationDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

