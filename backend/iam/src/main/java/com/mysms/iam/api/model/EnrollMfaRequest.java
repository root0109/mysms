// Owned API contract of iam.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * EnrollMfaRequest
 */

public class EnrollMfaRequest {

  /**
   * Factor kind to enroll.
   */
  public enum CredentialTypeEnum {
    TOTP("totp"),
    
    SMS("sms"),
    
    EMAIL("email");

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
  private CredentialTypeEnum credentialType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String target;

  public EnrollMfaRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EnrollMfaRequest(CredentialTypeEnum credentialType) {
    this.credentialType = credentialType;
  }

  public EnrollMfaRequest credentialType(CredentialTypeEnum credentialType) {
    this.credentialType = credentialType;
    return this;
  }

  /**
   * Factor kind to enroll.
   * @return credentialType
   */
  @NotNull 
  @JsonProperty("credentialType")
  public CredentialTypeEnum getCredentialType() {
    return credentialType;
  }

  @JsonProperty("credentialType")
  public void setCredentialType(CredentialTypeEnum credentialType) {
    this.credentialType = credentialType;
  }

  public EnrollMfaRequest label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * User-facing label (device name, phone tail).
   * @return label
   */
  @Size(max = 100) 
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public EnrollMfaRequest target(@Nullable String target) {
    this.target = target;
    return this;
  }

  /**
   * Phone number or e-mail for OTP factors.
   * @return target
   */
  @Size(max = 150) 
  @JsonProperty("target")
  public @Nullable String getTarget() {
    return target;
  }

  @JsonProperty("target")
  public void setTarget(@Nullable String target) {
    this.target = target;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrollMfaRequest enrollMfaRequest = (EnrollMfaRequest) o;
    return Objects.equals(this.credentialType, enrollMfaRequest.credentialType) &&
        Objects.equals(this.label, enrollMfaRequest.label) &&
        Objects.equals(this.target, enrollMfaRequest.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentialType, label, target);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrollMfaRequest {\n");
    sb.append("    credentialType: ").append(toIndentedString(credentialType)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
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

