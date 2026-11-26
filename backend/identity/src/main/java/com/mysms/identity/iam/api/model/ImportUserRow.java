// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * One user of iam.ImportUsers, given inline.
 */

public class ImportUserRow {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String username;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mobileNo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UserAccount.UserTypeEnum userType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid RoleGrant> grants;

  public ImportUserRow() {
    super();
  }

  public ImportUserRow username(@Nullable String username) {
    this.username = username;
    return this;
  }

  /**
   * Login name (at least one of username, email, mobileNo).
   * @return username
   */
  @Size(max = 150) 
  @JsonProperty("username")
  public @Nullable String getUsername() {
    return username;
  }

  @JsonProperty("username")
  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  public ImportUserRow email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * E-mail address.
   * @return email
   */
  @Size(max = 320) 
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public ImportUserRow mobileNo(@Nullable String mobileNo) {
    this.mobileNo = mobileNo;
    return this;
  }

  /**
   * Mobile number.
   * @return mobileNo
   */
  @Size(max = 20) 
  @JsonProperty("mobileNo")
  public @Nullable String getMobileNo() {
    return mobileNo;
  }

  @JsonProperty("mobileNo")
  public void setMobileNo(@Nullable String mobileNo) {
    this.mobileNo = mobileNo;
  }

  public ImportUserRow userType(@Nullable UserAccount.UserTypeEnum userType) {
    this.userType = userType;
    return this;
  }

  /**
   * Default persona hint (as UserAccount.userType).
   * @return userType
   */
  
  @JsonProperty("userType")
  public @Nullable UserAccount.UserTypeEnum getUserType() {
    return userType;
  }

  @JsonProperty("userType")
  public void setUserType(@Nullable UserAccount.UserTypeEnum userType) {
    this.userType = userType;
  }

  public ImportUserRow grants(@Nullable List<@Valid RoleGrant> grants) {
    this.grants = grants;
    return this;
  }

  /**
   * Role grants of the new user (each within the importer's own permissions and scopes).
   * @return grants
   */
  @Size(max = 20) 
  @JsonProperty("grants")
  public @Nullable List<@Valid RoleGrant> getGrants() {
    return grants;
  }

  @JsonProperty("grants")
  public void setGrants(@Nullable List<@Valid RoleGrant> grants) {
    this.grants = grants;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportUserRow importUserRow = (ImportUserRow) o;
    return Objects.equals(this.username, importUserRow.username) &&
        Objects.equals(this.email, importUserRow.email) &&
        Objects.equals(this.mobileNo, importUserRow.mobileNo) &&
        Objects.equals(this.userType, importUserRow.userType) &&
        Objects.equals(this.grants, importUserRow.grants);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, email, mobileNo, userType, grants);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportUserRow {\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    mobileNo: ").append(toIndentedString(mobileNo)).append("\n");
    sb.append("    userType: ").append(toIndentedString(userType)).append("\n");
    sb.append("    grants: ").append(toIndentedString(grants)).append("\n");
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
