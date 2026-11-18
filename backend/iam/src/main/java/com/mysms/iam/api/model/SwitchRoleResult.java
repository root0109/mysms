// Owned API contract of iam.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.iam.api.model.Me;
import com.mysms.iam.api.model.TokenPair;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SwitchRoleResult
 */

public class SwitchRoleResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private TokenPair tokens;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Me me;

  public SwitchRoleResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SwitchRoleResult(TokenPair tokens, Me me) {
    this.tokens = tokens;
    this.me = me;
  }

  public SwitchRoleResult tokens(TokenPair tokens) {
    this.tokens = tokens;
    return this;
  }

  /**
   * Get tokens
   * @return tokens
   */
  @NotNull @Valid 
  @JsonProperty("tokens")
  public TokenPair getTokens() {
    return tokens;
  }

  @JsonProperty("tokens")
  public void setTokens(TokenPair tokens) {
    this.tokens = tokens;
  }

  public SwitchRoleResult me(Me me) {
    this.me = me;
    return this;
  }

  /**
   * Get me
   * @return me
   */
  @NotNull @Valid 
  @JsonProperty("me")
  public Me getMe() {
    return me;
  }

  @JsonProperty("me")
  public void setMe(Me me) {
    this.me = me;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SwitchRoleResult switchRoleResult = (SwitchRoleResult) o;
    return Objects.equals(this.tokens, switchRoleResult.tokens) &&
        Objects.equals(this.me, switchRoleResult.me);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokens, me);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SwitchRoleResult {\n");
    sb.append("    tokens: ").append(toIndentedString(tokens)).append("\n");
    sb.append("    me: ").append(toIndentedString(me)).append("\n");
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

