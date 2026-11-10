// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.iam.api.model.UserDevice;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * LoginRequest
 */

public class LoginRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tenantCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String identifier;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String password;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UserDevice device;

  public LoginRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LoginRequest(String tenantCode, String identifier, String password) {
    this.tenantCode = tenantCode;
    this.identifier = identifier;
    this.password = password;
  }

  public LoginRequest tenantCode(String tenantCode) {
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

  public LoginRequest identifier(String identifier) {
    this.identifier = identifier;
    return this;
  }

  /**
   * Username, e-mail or mobile.
   * @return identifier
   */
  @NotNull 
  @JsonProperty("identifier")
  public String getIdentifier() {
    return identifier;
  }

  @JsonProperty("identifier")
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public LoginRequest password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Password.
   * @return password
   */
  @NotNull 
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  @JsonProperty("password")
  public void setPassword(String password) {
    this.password = password;
  }

  public LoginRequest device(@Nullable UserDevice device) {
    this.device = device;
    return this;
  }

  /**
   * Get device
   * @return device
   */
  @Valid 
  @JsonProperty("device")
  public @Nullable UserDevice getDevice() {
    return device;
  }

  @JsonProperty("device")
  public void setDevice(@Nullable UserDevice device) {
    this.device = device;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoginRequest loginRequest = (LoginRequest) o;
    return Objects.equals(this.tenantCode, loginRequest.tenantCode) &&
        Objects.equals(this.identifier, loginRequest.identifier) &&
        Objects.equals(this.password, loginRequest.password) &&
        Objects.equals(this.device, loginRequest.device);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantCode, identifier, password, device);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoginRequest {\n");
    sb.append("    tenantCode: ").append(toIndentedString(tenantCode)).append("\n");
    sb.append("    identifier: ").append(toIndentedString(identifier)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    device: ").append(toIndentedString(device)).append("\n");
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

