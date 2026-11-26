// Owned API contract of people.
package com.mysms.identity.people.api.model;

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
 * Fields of Certification that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CertificationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> issuingOrganization;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> issueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> expires;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> expirationDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  public CertificationUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Certification name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public CertificationUpdate issuingOrganization(@Nullable Optional<String> issuingOrganization) {
    this.issuingOrganization = issuingOrganization;
    return this;
  }

  /**
   * Issuer.
   * @return issuingOrganization
   */
  
  @JsonProperty("issuingOrganization")
  public @Nullable Optional<String> getIssuingOrganization() {
    return issuingOrganization;
  }

  @JsonProperty("issuingOrganization")
  public void setIssuingOrganization(@Nullable Optional<String> issuingOrganization) {
    this.issuingOrganization = issuingOrganization;
  }

  public CertificationUpdate issueDate(@Nullable Optional<LocalDate> issueDate) {
    this.issueDate = issueDate;
    return this;
  }

  /**
   * Issue date.
   * @return issueDate
   */
  
  @JsonProperty("issueDate")
  public @Nullable Optional<LocalDate> getIssueDate() {
    return issueDate;
  }

  @JsonProperty("issueDate")
  public void setIssueDate(@Nullable Optional<LocalDate> issueDate) {
    this.issueDate = issueDate;
  }

  public CertificationUpdate expires(@Nullable Optional<Boolean> expires) {
    this.expires = expires;
    return this;
  }

  /**
   * Whether it expires.
   * @return expires
   */
  
  @JsonProperty("expires")
  public @Nullable Optional<Boolean> getExpires() {
    return expires;
  }

  @JsonProperty("expires")
  public void setExpires(@Nullable Optional<Boolean> expires) {
    this.expires = expires;
  }

  public CertificationUpdate expirationDate(@Nullable Optional<LocalDate> expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  /**
   * Expiry date.
   * @return expirationDate
   */
  
  @JsonProperty("expirationDate")
  public @Nullable Optional<LocalDate> getExpirationDate() {
    return expirationDate;
  }

  @JsonProperty("expirationDate")
  public void setExpirationDate(@Nullable Optional<LocalDate> expirationDate) {
    this.expirationDate = expirationDate;
  }

  public CertificationUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Details.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
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

