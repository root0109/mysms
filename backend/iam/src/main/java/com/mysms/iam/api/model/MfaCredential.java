// Owned API contract of iam.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * One enrolled second factor (no secret material).
 */

public class MfaCredential {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  /**
   * Factor kind.
   */
  public enum CredentialTypeEnum {
    TOTP("totp"),
    
    SMS("sms"),
    
    EMAIL("email"),
    
    RECOVERY_CODES("recovery_codes");

    private final String value;

    CredentialTypeEnum(String value) {
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
    public static CredentialTypeEnum fromValue(String value) {
      for (CredentialTypeEnum b : CredentialTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable CredentialTypeEnum credentialType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime verifiedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastUsedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public MfaCredential id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public MfaCredential credentialType(@Nullable CredentialTypeEnum credentialType) {
    this.credentialType = credentialType;
    return this;
  }

  /**
   * Factor kind.
   * @return credentialType
   */
  
  @JsonProperty("credentialType")
  public @Nullable CredentialTypeEnum getCredentialType() {
    return credentialType;
  }

  @JsonProperty("credentialType")
  public void setCredentialType(@Nullable CredentialTypeEnum credentialType) {
    this.credentialType = credentialType;
  }

  public MfaCredential label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * User-facing label (device name, phone tail).
   * @return label
   */
  
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public MfaCredential verifiedAt(@Nullable OffsetDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
    return this;
  }

  /**
   * When enrollment was proven with a valid code.
   * @return verifiedAt
   */
  @Valid 
  @JsonProperty("verifiedAt")
  public @Nullable OffsetDateTime getVerifiedAt() {
    return verifiedAt;
  }

  @JsonProperty("verifiedAt")
  public void setVerifiedAt(@Nullable OffsetDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public MfaCredential lastUsedAt(@Nullable OffsetDateTime lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
    return this;
  }

  /**
   * Last successful use.
   * @return lastUsedAt
   */
  @Valid 
  @JsonProperty("lastUsedAt")
  public @Nullable OffsetDateTime getLastUsedAt() {
    return lastUsedAt;
  }

  @JsonProperty("lastUsedAt")
  public void setLastUsedAt(@Nullable OffsetDateTime lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public MfaCredential isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable at sign-in.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
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
    MfaCredential mfaCredential = (MfaCredential) o;
    return Objects.equals(this.id, mfaCredential.id) &&
        Objects.equals(this.credentialType, mfaCredential.credentialType) &&
        Objects.equals(this.label, mfaCredential.label) &&
        Objects.equals(this.verifiedAt, mfaCredential.verifiedAt) &&
        Objects.equals(this.lastUsedAt, mfaCredential.lastUsedAt) &&
        Objects.equals(this.isActive, mfaCredential.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, credentialType, label, verifiedAt, lastUsedAt, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MfaCredential {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    credentialType: ").append(toIndentedString(credentialType)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    verifiedAt: ").append(toIndentedString(verifiedAt)).append("\n");
    sb.append("    lastUsedAt: ").append(toIndentedString(lastUsedAt)).append("\n");
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

