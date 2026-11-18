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
 * ChangePasswordRequest
 */

public class ChangePasswordRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String currentPassword;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String newPassword;

  public ChangePasswordRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ChangePasswordRequest(String currentPassword, String newPassword) {
    this.currentPassword = currentPassword;
    this.newPassword = newPassword;
  }

  public ChangePasswordRequest currentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
    return this;
  }

  /**
   * Current password.
   * @return currentPassword
   */
  @NotNull 
  @JsonProperty("currentPassword")
  public String getCurrentPassword() {
    return currentPassword;
  }

  @JsonProperty("currentPassword")
  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public ChangePasswordRequest newPassword(String newPassword) {
    this.newPassword = newPassword;
    return this;
  }

  /**
   * New password.
   * @return newPassword
   */
  @NotNull 
  @JsonProperty("newPassword")
  public String getNewPassword() {
    return newPassword;
  }

  @JsonProperty("newPassword")
  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangePasswordRequest changePasswordRequest = (ChangePasswordRequest) o;
    return Objects.equals(this.currentPassword, changePasswordRequest.currentPassword) &&
        Objects.equals(this.newPassword, changePasswordRequest.newPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentPassword, newPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChangePasswordRequest {\n");
    sb.append("    currentPassword: ").append(toIndentedString(currentPassword)).append("\n");
    sb.append("    newPassword: ").append(toIndentedString(newPassword)).append("\n");
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

