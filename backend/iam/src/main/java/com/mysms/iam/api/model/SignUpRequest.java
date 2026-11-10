// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api.model;

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
 * SignUpRequest
 */

public class SignUpRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tenantCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String fullName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean acceptTerms;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String personaCategory;

  public SignUpRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SignUpRequest(String tenantCode, String email, String fullName, Boolean acceptTerms) {
    this.tenantCode = tenantCode;
    this.email = email;
    this.fullName = fullName;
    this.acceptTerms = acceptTerms;
  }

  public SignUpRequest tenantCode(String tenantCode) {
    this.tenantCode = tenantCode;
    return this;
  }

  /**
   * Tenant code.
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

  public SignUpRequest email(String email) {
    this.email = email;
    return this;
  }

  /**
   * E-mail.
   * @return email
   */
  @NotNull 
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(String email) {
    this.email = email;
  }

  public SignUpRequest fullName(String fullName) {
    this.fullName = fullName;
    return this;
  }

  /**
   * Full name.
   * @return fullName
   */
  @NotNull 
  @JsonProperty("fullName")
  public String getFullName() {
    return fullName;
  }

  @JsonProperty("fullName")
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public SignUpRequest acceptTerms(Boolean acceptTerms) {
    this.acceptTerms = acceptTerms;
    return this;
  }

  /**
   * Terms accepted.
   * @return acceptTerms
   */
  @NotNull 
  @JsonProperty("acceptTerms")
  public Boolean getAcceptTerms() {
    return acceptTerms;
  }

  @JsonProperty("acceptTerms")
  public void setAcceptTerms(Boolean acceptTerms) {
    this.acceptTerms = acceptTerms;
  }

  public SignUpRequest personaCategory(@Nullable String personaCategory) {
    this.personaCategory = personaCategory;
    return this;
  }

  /**
   * Persona.
   * @return personaCategory
   */
  
  @JsonProperty("personaCategory")
  public @Nullable String getPersonaCategory() {
    return personaCategory;
  }

  @JsonProperty("personaCategory")
  public void setPersonaCategory(@Nullable String personaCategory) {
    this.personaCategory = personaCategory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignUpRequest signUpRequest = (SignUpRequest) o;
    return Objects.equals(this.tenantCode, signUpRequest.tenantCode) &&
        Objects.equals(this.email, signUpRequest.email) &&
        Objects.equals(this.fullName, signUpRequest.fullName) &&
        Objects.equals(this.acceptTerms, signUpRequest.acceptTerms) &&
        Objects.equals(this.personaCategory, signUpRequest.personaCategory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantCode, email, fullName, acceptTerms, personaCategory);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SignUpRequest {\n");
    sb.append("    tenantCode: ").append(toIndentedString(tenantCode)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    fullName: ").append(toIndentedString(fullName)).append("\n");
    sb.append("    acceptTerms: ").append(toIndentedString(acceptTerms)).append("\n");
    sb.append("    personaCategory: ").append(toIndentedString(personaCategory)).append("\n");
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

