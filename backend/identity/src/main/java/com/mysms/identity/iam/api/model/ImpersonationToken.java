// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

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
 * ImpersonationToken
 */

public class ImpersonationToken {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String accessToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer expiresInSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID impersonatedUserId;

  public ImpersonationToken() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImpersonationToken(String accessToken, Integer expiresInSeconds, UUID impersonatedUserId) {
    this.accessToken = accessToken;
    this.expiresInSeconds = expiresInSeconds;
    this.impersonatedUserId = impersonatedUserId;
  }

  public ImpersonationToken accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * Access token acting as the target user (not refreshable).
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

  public ImpersonationToken expiresInSeconds(Integer expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
    return this;
  }

  /**
   * Lifetime (maximum 1800).
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

  public ImpersonationToken impersonatedUserId(UUID impersonatedUserId) {
    this.impersonatedUserId = impersonatedUserId;
    return this;
  }

  /**
   * Target user.
   * @return impersonatedUserId
   */
  @NotNull @Valid 
  @JsonProperty("impersonatedUserId")
  public UUID getImpersonatedUserId() {
    return impersonatedUserId;
  }

  @JsonProperty("impersonatedUserId")
  public void setImpersonatedUserId(UUID impersonatedUserId) {
    this.impersonatedUserId = impersonatedUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImpersonationToken impersonationToken = (ImpersonationToken) o;
    return Objects.equals(this.accessToken, impersonationToken.accessToken) &&
        Objects.equals(this.expiresInSeconds, impersonationToken.expiresInSeconds) &&
        Objects.equals(this.impersonatedUserId, impersonationToken.impersonatedUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, expiresInSeconds, impersonatedUserId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImpersonationToken {\n");
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    expiresInSeconds: ").append(toIndentedString(expiresInSeconds)).append("\n");
    sb.append("    impersonatedUserId: ").append(toIndentedString(impersonatedUserId)).append("\n");
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

