// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of PlagiarismAccount that can be updated (partial update; see updateMask).
 */

public class PlagiarismAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String provider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subAccountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String apiUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String sharedSecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isDefault;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultTeacherUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String defaultTeacherEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String defaultTeacherFirstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String defaultTeacherLastName;

  public PlagiarismAccountUpdate provider(@Nullable String provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Provider.
   * @return provider
   */
  @Size(max = 40) 
  @JsonProperty("provider")
  public @Nullable String getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(@Nullable String provider) {
    this.provider = provider;
  }

  public PlagiarismAccountUpdate accountRef(@Nullable String accountRef) {
    this.accountRef = accountRef;
    return this;
  }

  /**
   * Provider account id.
   * @return accountRef
   */
  @Size(max = 100) 
  @JsonProperty("accountRef")
  public @Nullable String getAccountRef() {
    return accountRef;
  }

  @JsonProperty("accountRef")
  public void setAccountRef(@Nullable String accountRef) {
    this.accountRef = accountRef;
  }

  public PlagiarismAccountUpdate subAccountRef(@Nullable String subAccountRef) {
    this.subAccountRef = subAccountRef;
    return this;
  }

  /**
   * Provider sub-account id.
   * @return subAccountRef
   */
  @Size(max = 100) 
  @JsonProperty("subAccountRef")
  public @Nullable String getSubAccountRef() {
    return subAccountRef;
  }

  @JsonProperty("subAccountRef")
  public void setSubAccountRef(@Nullable String subAccountRef) {
    this.subAccountRef = subAccountRef;
  }

  public PlagiarismAccountUpdate apiUrl(@Nullable String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  /**
   * API endpoint.
   * @return apiUrl
   */
  @Size(max = 500) 
  @JsonProperty("apiUrl")
  public @Nullable String getApiUrl() {
    return apiUrl;
  }

  @JsonProperty("apiUrl")
  public void setApiUrl(@Nullable String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public PlagiarismAccountUpdate sharedSecret(@Nullable String sharedSecret) {
    this.sharedSecret = sharedSecret;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. API shared secret, encrypted by the application.
   * @return sharedSecret
   */
  
  @JsonProperty("sharedSecret")
  public @Nullable String getSharedSecret() {
    return sharedSecret;
  }

  @JsonProperty("sharedSecret")
  public void setSharedSecret(@Nullable String sharedSecret) {
    this.sharedSecret = sharedSecret;
  }

  public PlagiarismAccountUpdate isDefault(@Nullable Boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  /**
   * Default account of the tenant.
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

  public PlagiarismAccountUpdate defaultTeacherUserId(@Nullable UUID defaultTeacherUserId) {
    this.defaultTeacherUserId = defaultTeacherUserId;
    return this;
  }

  /**
   * Teacher used when a cohort has none.
   * @return defaultTeacherUserId
   */
  @Valid 
  @JsonProperty("defaultTeacherUserId")
  public @Nullable UUID getDefaultTeacherUserId() {
    return defaultTeacherUserId;
  }

  @JsonProperty("defaultTeacherUserId")
  public void setDefaultTeacherUserId(@Nullable UUID defaultTeacherUserId) {
    this.defaultTeacherUserId = defaultTeacherUserId;
  }

  public PlagiarismAccountUpdate defaultTeacherEmail(@Nullable String defaultTeacherEmail) {
    this.defaultTeacherEmail = defaultTeacherEmail;
    return this;
  }

  /**
   * Default teacher e-mail registered with the provider.
   * @return defaultTeacherEmail
   */
  @Size(max = 255) 
  @JsonProperty("defaultTeacherEmail")
  public @Nullable String getDefaultTeacherEmail() {
    return defaultTeacherEmail;
  }

  @JsonProperty("defaultTeacherEmail")
  public void setDefaultTeacherEmail(@Nullable String defaultTeacherEmail) {
    this.defaultTeacherEmail = defaultTeacherEmail;
  }

  public PlagiarismAccountUpdate defaultTeacherFirstName(@Nullable String defaultTeacherFirstName) {
    this.defaultTeacherFirstName = defaultTeacherFirstName;
    return this;
  }

  /**
   * Default teacher first name.
   * @return defaultTeacherFirstName
   */
  @Size(max = 100) 
  @JsonProperty("defaultTeacherFirstName")
  public @Nullable String getDefaultTeacherFirstName() {
    return defaultTeacherFirstName;
  }

  @JsonProperty("defaultTeacherFirstName")
  public void setDefaultTeacherFirstName(@Nullable String defaultTeacherFirstName) {
    this.defaultTeacherFirstName = defaultTeacherFirstName;
  }

  public PlagiarismAccountUpdate defaultTeacherLastName(@Nullable String defaultTeacherLastName) {
    this.defaultTeacherLastName = defaultTeacherLastName;
    return this;
  }

  /**
   * Default teacher last name.
   * @return defaultTeacherLastName
   */
  @Size(max = 100) 
  @JsonProperty("defaultTeacherLastName")
  public @Nullable String getDefaultTeacherLastName() {
    return defaultTeacherLastName;
  }

  @JsonProperty("defaultTeacherLastName")
  public void setDefaultTeacherLastName(@Nullable String defaultTeacherLastName) {
    this.defaultTeacherLastName = defaultTeacherLastName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlagiarismAccountUpdate plagiarismAccountUpdate = (PlagiarismAccountUpdate) o;
    return Objects.equals(this.provider, plagiarismAccountUpdate.provider) &&
        Objects.equals(this.accountRef, plagiarismAccountUpdate.accountRef) &&
        Objects.equals(this.subAccountRef, plagiarismAccountUpdate.subAccountRef) &&
        Objects.equals(this.apiUrl, plagiarismAccountUpdate.apiUrl) &&
        Objects.equals(this.sharedSecret, plagiarismAccountUpdate.sharedSecret) &&
        Objects.equals(this.isDefault, plagiarismAccountUpdate.isDefault) &&
        Objects.equals(this.defaultTeacherUserId, plagiarismAccountUpdate.defaultTeacherUserId) &&
        Objects.equals(this.defaultTeacherEmail, plagiarismAccountUpdate.defaultTeacherEmail) &&
        Objects.equals(this.defaultTeacherFirstName, plagiarismAccountUpdate.defaultTeacherFirstName) &&
        Objects.equals(this.defaultTeacherLastName, plagiarismAccountUpdate.defaultTeacherLastName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, accountRef, subAccountRef, apiUrl, sharedSecret, isDefault, defaultTeacherUserId, defaultTeacherEmail, defaultTeacherFirstName, defaultTeacherLastName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PlagiarismAccountUpdate {\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    accountRef: ").append(toIndentedString(accountRef)).append("\n");
    sb.append("    subAccountRef: ").append(toIndentedString(subAccountRef)).append("\n");
    sb.append("    apiUrl: ").append(toIndentedString(apiUrl)).append("\n");
    sb.append("    sharedSecret: ").append(toIndentedString(sharedSecret)).append("\n");
    sb.append("    isDefault: ").append(toIndentedString(isDefault)).append("\n");
    sb.append("    defaultTeacherUserId: ").append(toIndentedString(defaultTeacherUserId)).append("\n");
    sb.append("    defaultTeacherEmail: ").append(toIndentedString(defaultTeacherEmail)).append("\n");
    sb.append("    defaultTeacherFirstName: ").append(toIndentedString(defaultTeacherFirstName)).append("\n");
    sb.append("    defaultTeacherLastName: ").append(toIndentedString(defaultTeacherLastName)).append("\n");
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

