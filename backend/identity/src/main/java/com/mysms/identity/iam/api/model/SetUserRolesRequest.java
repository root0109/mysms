// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.RoleGrant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SetUserRolesRequest
 */

public class SetUserRolesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid RoleGrant> grants = new ArrayList<>();

  public SetUserRolesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetUserRolesRequest(List<@Valid RoleGrant> grants) {
    this.grants = grants;
  }

  public SetUserRolesRequest grants(List<@Valid RoleGrant> grants) {
    this.grants = grants;
    return this;
  }

  public SetUserRolesRequest addGrantsItem(RoleGrant grantsItem) {
    if (this.grants == null) {
      this.grants = new ArrayList<>();
    }
    this.grants.add(grantsItem);
    return this;
  }

  /**
   * Complete set of role grants (replaces existing).
   * @return grants
   */
  @NotNull 
  @JsonProperty("grants")
  public List<@Valid RoleGrant> getGrants() {
    return grants;
  }

  @JsonProperty("grants")
  public void setGrants(List<@Valid RoleGrant> grants) {
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
    SetUserRolesRequest setUserRolesRequest = (SetUserRolesRequest) o;
    return Objects.equals(this.grants, setUserRolesRequest.grants);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grants);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetUserRolesRequest {\n");
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

