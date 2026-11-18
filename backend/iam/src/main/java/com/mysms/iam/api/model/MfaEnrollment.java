// Owned API contract of iam.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Enrollment material. The secret appears only once, here.
 */

public class MfaEnrollment {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mfaToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID mfaCredentialId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String secret;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String otpauthUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> recoveryCodes = new ArrayList<>();

  public MfaEnrollment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MfaEnrollment(UUID mfaCredentialId) {
    this.mfaCredentialId = mfaCredentialId;
  }

  public MfaEnrollment mfaToken(@Nullable String mfaToken) {
    this.mfaToken = mfaToken;
    return this;
  }

  /**
   * Short-lived token to pass to iam.CompleteMfa with the first valid code, which verifies the enrollment.
   * @return mfaToken
   */
  
  @JsonProperty("mfaToken")
  public @Nullable String getMfaToken() {
    return mfaToken;
  }

  @JsonProperty("mfaToken")
  public void setMfaToken(@Nullable String mfaToken) {
    this.mfaToken = mfaToken;
  }

  public MfaEnrollment mfaCredentialId(UUID mfaCredentialId) {
    this.mfaCredentialId = mfaCredentialId;
    return this;
  }

  /**
   * Credential being enrolled.
   * @return mfaCredentialId
   */
  @NotNull @Valid 
  @JsonProperty("mfaCredentialId")
  public UUID getMfaCredentialId() {
    return mfaCredentialId;
  }

  @JsonProperty("mfaCredentialId")
  public void setMfaCredentialId(UUID mfaCredentialId) {
    this.mfaCredentialId = mfaCredentialId;
  }

  public MfaEnrollment secret(@Nullable String secret) {
    this.secret = secret;
    return this;
  }

  /**
   * TOTP shared secret (base32), present for totp only; shown once.
   * @return secret
   */
  
  @JsonProperty("secret")
  public @Nullable String getSecret() {
    return secret;
  }

  @JsonProperty("secret")
  public void setSecret(@Nullable String secret) {
    this.secret = secret;
  }

  public MfaEnrollment otpauthUrl(@Nullable String otpauthUrl) {
    this.otpauthUrl = otpauthUrl;
    return this;
  }

  /**
   * otpauth:// URL for authenticator apps, present for totp only.
   * @return otpauthUrl
   */
  
  @JsonProperty("otpauthUrl")
  public @Nullable String getOtpauthUrl() {
    return otpauthUrl;
  }

  @JsonProperty("otpauthUrl")
  public void setOtpauthUrl(@Nullable String otpauthUrl) {
    this.otpauthUrl = otpauthUrl;
  }

  public MfaEnrollment recoveryCodes(List<String> recoveryCodes) {
    this.recoveryCodes = recoveryCodes;
    return this;
  }

  public MfaEnrollment addRecoveryCodesItem(String recoveryCodesItem) {
    if (this.recoveryCodes == null) {
      this.recoveryCodes = new ArrayList<>();
    }
    this.recoveryCodes.add(recoveryCodesItem);
    return this;
  }

  /**
   * One-time recovery codes; shown once.
   * @return recoveryCodes
   */
  
  @JsonProperty("recoveryCodes")
  public List<String> getRecoveryCodes() {
    return recoveryCodes;
  }

  @JsonProperty("recoveryCodes")
  public void setRecoveryCodes(List<String> recoveryCodes) {
    this.recoveryCodes = recoveryCodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MfaEnrollment mfaEnrollment = (MfaEnrollment) o;
    return Objects.equals(this.mfaToken, mfaEnrollment.mfaToken) &&
        Objects.equals(this.mfaCredentialId, mfaEnrollment.mfaCredentialId) &&
        Objects.equals(this.secret, mfaEnrollment.secret) &&
        Objects.equals(this.otpauthUrl, mfaEnrollment.otpauthUrl) &&
        Objects.equals(this.recoveryCodes, mfaEnrollment.recoveryCodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mfaToken, mfaCredentialId, secret, otpauthUrl, recoveryCodes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MfaEnrollment {\n");
    sb.append("    mfaToken: ").append(toIndentedString(mfaToken)).append("\n");
    sb.append("    mfaCredentialId: ").append(toIndentedString(mfaCredentialId)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    otpauthUrl: ").append(toIndentedString(otpauthUrl)).append("\n");
    sb.append("    recoveryCodes: ").append(toIndentedString(recoveryCodes)).append("\n");
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

