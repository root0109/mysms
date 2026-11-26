// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.TokenPair;
import com.mysms.identity.iam.api.model.UserAccount;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Either a completed sign-in (tokens + user, mfaRequired absent or false) or an MFA challenge (mfaRequired true + mfaToken only; no tokens and no user object are returned until iam.CompleteMfa succeeds).
 */

public class LoginResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TokenPair tokens;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UserAccount user;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean mfaRequired;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mfaToken;

  public LoginResult tokens(@Nullable TokenPair tokens) {
    this.tokens = tokens;
    return this;
  }

  /**
   * Get tokens
   * @return tokens
   */
  @Valid 
  @JsonProperty("tokens")
  public @Nullable TokenPair getTokens() {
    return tokens;
  }

  @JsonProperty("tokens")
  public void setTokens(@Nullable TokenPair tokens) {
    this.tokens = tokens;
  }

  public LoginResult user(@Nullable UserAccount user) {
    this.user = user;
    return this;
  }

  /**
   * Get user
   * @return user
   */
  @Valid 
  @JsonProperty("user")
  public @Nullable UserAccount getUser() {
    return user;
  }

  @JsonProperty("user")
  public void setUser(@Nullable UserAccount user) {
    this.user = user;
  }

  public LoginResult mfaRequired(@Nullable Boolean mfaRequired) {
    this.mfaRequired = mfaRequired;
    return this;
  }

  /**
   * A second factor must be completed before tokens are usable.
   * @return mfaRequired
   */
  
  @JsonProperty("mfaRequired")
  public @Nullable Boolean getMfaRequired() {
    return mfaRequired;
  }

  @JsonProperty("mfaRequired")
  public void setMfaRequired(@Nullable Boolean mfaRequired) {
    this.mfaRequired = mfaRequired;
  }

  public LoginResult mfaToken(@Nullable String mfaToken) {
    this.mfaToken = mfaToken;
    return this;
  }

  /**
   * Short-lived token to pass to iam.CompleteMfa together with the code. Present only when mfaRequired is true.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoginResult loginResult = (LoginResult) o;
    return Objects.equals(this.tokens, loginResult.tokens) &&
        Objects.equals(this.user, loginResult.user) &&
        Objects.equals(this.mfaRequired, loginResult.mfaRequired) &&
        Objects.equals(this.mfaToken, loginResult.mfaToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokens, user, mfaRequired, mfaToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoginResult {\n");
    sb.append("    tokens: ").append(toIndentedString(tokens)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    mfaRequired: ").append(toIndentedString(mfaRequired)).append("\n");
    sb.append("    mfaToken: ").append(toIndentedString(mfaToken)).append("\n");
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

