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
 * SetUserCredentialsRequest
 */

public class SetUserCredentialsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String username;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String temporaryPassword;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean mustResetPassword;

  public SetUserCredentialsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetUserCredentialsRequest(String temporaryPassword) {
    this.temporaryPassword = temporaryPassword;
  }

  public SetUserCredentialsRequest username(@Nullable String username) {
    this.username = username;
    return this;
  }

  /**
   * New login name.
   * @return username
   */
  
  @JsonProperty("username")
  public @Nullable String getUsername() {
    return username;
  }

  @JsonProperty("username")
  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  public SetUserCredentialsRequest temporaryPassword(String temporaryPassword) {
    this.temporaryPassword = temporaryPassword;
    return this;
  }

  /**
   * Temporary password.
   * @return temporaryPassword
   */
  @NotNull 
  @JsonProperty("temporaryPassword")
  public String getTemporaryPassword() {
    return temporaryPassword;
  }

  @JsonProperty("temporaryPassword")
  public void setTemporaryPassword(String temporaryPassword) {
    this.temporaryPassword = temporaryPassword;
  }

  public SetUserCredentialsRequest mustResetPassword(@Nullable Boolean mustResetPassword) {
    this.mustResetPassword = mustResetPassword;
    return this;
  }

  /**
   * Force change at next login (default true).
   * @return mustResetPassword
   */
  
  @JsonProperty("mustResetPassword")
  public @Nullable Boolean getMustResetPassword() {
    return mustResetPassword;
  }

  @JsonProperty("mustResetPassword")
  public void setMustResetPassword(@Nullable Boolean mustResetPassword) {
    this.mustResetPassword = mustResetPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetUserCredentialsRequest setUserCredentialsRequest = (SetUserCredentialsRequest) o;
    return Objects.equals(this.username, setUserCredentialsRequest.username) &&
        Objects.equals(this.temporaryPassword, setUserCredentialsRequest.temporaryPassword) &&
        Objects.equals(this.mustResetPassword, setUserCredentialsRequest.mustResetPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, temporaryPassword, mustResetPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetUserCredentialsRequest {\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    temporaryPassword: ").append(toIndentedString(temporaryPassword)).append("\n");
    sb.append("    mustResetPassword: ").append(toIndentedString(mustResetPassword)).append("\n");
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

