// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.AuthToken;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ApiTokenCreated
 */

public class ApiTokenCreated {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String token;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private AuthToken authToken;

  public ApiTokenCreated() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiTokenCreated(String token, AuthToken authToken) {
    this.token = token;
    this.authToken = authToken;
  }

  public ApiTokenCreated token(String token) {
    this.token = token;
    return this;
  }

  /**
   * The secret token. Returned only in this response; store it securely.
   * @return token
   */
  @NotNull 
  @JsonProperty("token")
  public String getToken() {
    return token;
  }

  @JsonProperty("token")
  public void setToken(String token) {
    this.token = token;
  }

  public ApiTokenCreated authToken(AuthToken authToken) {
    this.authToken = authToken;
    return this;
  }

  /**
   * Get authToken
   * @return authToken
   */
  @NotNull @Valid 
  @JsonProperty("authToken")
  public AuthToken getAuthToken() {
    return authToken;
  }

  @JsonProperty("authToken")
  public void setAuthToken(AuthToken authToken) {
    this.authToken = authToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiTokenCreated apiTokenCreated = (ApiTokenCreated) o;
    return Objects.equals(this.token, apiTokenCreated.token) &&
        Objects.equals(this.authToken, apiTokenCreated.authToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, authToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiTokenCreated {\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    authToken: ").append(toIndentedString(authToken)).append("\n");
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

