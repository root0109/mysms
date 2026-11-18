// Owned API contract of iam.
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
 * RequestPasswordResetRequest
 */

public class RequestPasswordResetRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tenantCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String identifier;

  public RequestPasswordResetRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RequestPasswordResetRequest(String tenantCode, String identifier) {
    this.tenantCode = tenantCode;
    this.identifier = identifier;
  }

  public RequestPasswordResetRequest tenantCode(String tenantCode) {
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

  public RequestPasswordResetRequest identifier(String identifier) {
    this.identifier = identifier;
    return this;
  }

  /**
   * E-mail or mobile.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RequestPasswordResetRequest requestPasswordResetRequest = (RequestPasswordResetRequest) o;
    return Objects.equals(this.tenantCode, requestPasswordResetRequest.tenantCode) &&
        Objects.equals(this.identifier, requestPasswordResetRequest.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantCode, identifier);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RequestPasswordResetRequest {\n");
    sb.append("    tenantCode: ").append(toIndentedString(tenantCode)).append("\n");
    sb.append("    identifier: ").append(toIndentedString(identifier)).append("\n");
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

