// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of PlagiarismAccount that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PlagiarismAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> provider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> accountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> subAccountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> apiUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> sharedSecret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isDefault;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> defaultTeacherUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> defaultTeacherEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> defaultTeacherFirstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> defaultTeacherLastName;

  public PlagiarismAccountUpdate provider(@Nullable Optional<String> provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Provider.
   * @return provider
   */
  
  @JsonProperty("provider")
  public @Nullable Optional<String> getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(@Nullable Optional<String> provider) {
    this.provider = provider;
  }

  public PlagiarismAccountUpdate accountRef(@Nullable Optional<String> accountRef) {
    this.accountRef = accountRef;
    return this;
  }

  /**
   * Provider account id.
   * @return accountRef
   */
  
  @JsonProperty("accountRef")
  public @Nullable Optional<String> getAccountRef() {
    return accountRef;
  }

  @JsonProperty("accountRef")
  public void setAccountRef(@Nullable Optional<String> accountRef) {
    this.accountRef = accountRef;
  }

  public PlagiarismAccountUpdate subAccountRef(@Nullable Optional<String> subAccountRef) {
    this.subAccountRef = subAccountRef;
    return this;
  }

  /**
   * Provider sub-account id.
   * @return subAccountRef
   */
  
  @JsonProperty("subAccountRef")
  public @Nullable Optional<String> getSubAccountRef() {
    return subAccountRef;
  }

  @JsonProperty("subAccountRef")
  public void setSubAccountRef(@Nullable Optional<String> subAccountRef) {
    this.subAccountRef = subAccountRef;
  }

  public PlagiarismAccountUpdate apiUrl(@Nullable Optional<String> apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  /**
   * API endpoint.
   * @return apiUrl
   */
  
  @JsonProperty("apiUrl")
  public @Nullable Optional<String> getApiUrl() {
    return apiUrl;
  }

  @JsonProperty("apiUrl")
  public void setApiUrl(@Nullable Optional<String> apiUrl) {
    this.apiUrl = apiUrl;
  }

  public PlagiarismAccountUpdate sharedSecret(@Nullable Optional<String> sharedSecret) {
    this.sharedSecret = sharedSecret;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. API shared secret, encrypted by the application.
   * @return sharedSecret
   */
  
  @JsonProperty("sharedSecret")
  public @Nullable Optional<String> getSharedSecret() {
    return sharedSecret;
  }

  @JsonProperty("sharedSecret")
  public void setSharedSecret(@Nullable Optional<String> sharedSecret) {
    this.sharedSecret = sharedSecret;
  }

  public PlagiarismAccountUpdate isDefault(@Nullable Optional<Boolean> isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  /**
   * Default account of the tenant.
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

  public PlagiarismAccountUpdate defaultTeacherUserId(@Nullable Optional<UUID> defaultTeacherUserId) {
    this.defaultTeacherUserId = defaultTeacherUserId;
    return this;
  }

  /**
   * Teacher used when a cohort has none.
   * @return defaultTeacherUserId
   */
  
  @JsonProperty("defaultTeacherUserId")
  public @Nullable Optional<UUID> getDefaultTeacherUserId() {
    return defaultTeacherUserId;
  }

  @JsonProperty("defaultTeacherUserId")
  public void setDefaultTeacherUserId(@Nullable Optional<UUID> defaultTeacherUserId) {
    this.defaultTeacherUserId = defaultTeacherUserId;
  }

  public PlagiarismAccountUpdate defaultTeacherEmail(@Nullable Optional<String> defaultTeacherEmail) {
    this.defaultTeacherEmail = defaultTeacherEmail;
    return this;
  }

  /**
   * Default teacher e-mail registered with the provider.
   * @return defaultTeacherEmail
   */
  
  @JsonProperty("defaultTeacherEmail")
  public @Nullable Optional<String> getDefaultTeacherEmail() {
    return defaultTeacherEmail;
  }

  @JsonProperty("defaultTeacherEmail")
  public void setDefaultTeacherEmail(@Nullable Optional<String> defaultTeacherEmail) {
    this.defaultTeacherEmail = defaultTeacherEmail;
  }

  public PlagiarismAccountUpdate defaultTeacherFirstName(@Nullable Optional<String> defaultTeacherFirstName) {
    this.defaultTeacherFirstName = defaultTeacherFirstName;
    return this;
  }

  /**
   * Default teacher first name.
   * @return defaultTeacherFirstName
   */
  
  @JsonProperty("defaultTeacherFirstName")
  public @Nullable Optional<String> getDefaultTeacherFirstName() {
    return defaultTeacherFirstName;
  }

  @JsonProperty("defaultTeacherFirstName")
  public void setDefaultTeacherFirstName(@Nullable Optional<String> defaultTeacherFirstName) {
    this.defaultTeacherFirstName = defaultTeacherFirstName;
  }

  public PlagiarismAccountUpdate defaultTeacherLastName(@Nullable Optional<String> defaultTeacherLastName) {
    this.defaultTeacherLastName = defaultTeacherLastName;
    return this;
  }

  /**
   * Default teacher last name.
   * @return defaultTeacherLastName
   */
  
  @JsonProperty("defaultTeacherLastName")
  public @Nullable Optional<String> getDefaultTeacherLastName() {
    return defaultTeacherLastName;
  }

  @JsonProperty("defaultTeacherLastName")
  public void setDefaultTeacherLastName(@Nullable Optional<String> defaultTeacherLastName) {
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

