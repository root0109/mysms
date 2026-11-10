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
 * TokenPair
 */

public class TokenPair {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String accessToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String refreshToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer expiresInSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tokenType;

  public TokenPair() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TokenPair(String accessToken, String refreshToken, Integer expiresInSeconds, String tokenType) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresInSeconds = expiresInSeconds;
    this.tokenType = tokenType;
  }

  public TokenPair accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * Short-lived JWT access token.
   * @return accessToken
   */
  @NotNull 
  @JsonProperty("accessToken")
  public String getAccessToken() {
    return accessToken;
  }

  @JsonProperty("accessToken")
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public TokenPair refreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

  /**
   * Refresh token (rotating).
   * @return refreshToken
   */
  @NotNull 
  @JsonProperty("refreshToken")
  public String getRefreshToken() {
    return refreshToken;
  }

  @JsonProperty("refreshToken")
  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public TokenPair expiresInSeconds(Integer expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
    return this;
  }

  /**
   * Access-token lifetime.
   * @return expiresInSeconds
   */
  @NotNull 
  @JsonProperty("expiresInSeconds")
  public Integer getExpiresInSeconds() {
    return expiresInSeconds;
  }

  @JsonProperty("expiresInSeconds")
  public void setExpiresInSeconds(Integer expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
  }

  public TokenPair tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  /**
   * Always \"Bearer\".
   * @return tokenType
   */
  @NotNull 
  @JsonProperty("tokenType")
  public String getTokenType() {
    return tokenType;
  }

  @JsonProperty("tokenType")
  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TokenPair tokenPair = (TokenPair) o;
    return Objects.equals(this.accessToken, tokenPair.accessToken) &&
        Objects.equals(this.refreshToken, tokenPair.refreshToken) &&
        Objects.equals(this.expiresInSeconds, tokenPair.expiresInSeconds) &&
        Objects.equals(this.tokenType, tokenPair.tokenType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, refreshToken, expiresInSeconds, tokenType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TokenPair {\n");
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    refreshToken: ").append(toIndentedString(refreshToken)).append("\n");
    sb.append("    expiresInSeconds: ").append(toIndentedString(expiresInSeconds)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
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

