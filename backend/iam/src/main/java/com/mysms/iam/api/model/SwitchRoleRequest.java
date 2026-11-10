// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api.model;

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
 * SwitchRoleRequest
 */

public class SwitchRoleRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID scopeId;

  public SwitchRoleRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SwitchRoleRequest(UUID roleId) {
    this.roleId = roleId;
  }

  public SwitchRoleRequest roleId(UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Role to work in.
   * @return roleId
   */
  @NotNull @Valid 
  @JsonProperty("roleId")
  public UUID getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(UUID roleId) {
    this.roleId = roleId;
  }

  public SwitchRoleRequest scopeId(@Nullable UUID scopeId) {
    this.scopeId = scopeId;
    return this;
  }

  /**
   * Scope (institution / cohort).
   * @return scopeId
   */
  @Valid 
  @JsonProperty("scopeId")
  public @Nullable UUID getScopeId() {
    return scopeId;
  }

  @JsonProperty("scopeId")
  public void setScopeId(@Nullable UUID scopeId) {
    this.scopeId = scopeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SwitchRoleRequest switchRoleRequest = (SwitchRoleRequest) o;
    return Objects.equals(this.roleId, switchRoleRequest.roleId) &&
        Objects.equals(this.scopeId, switchRoleRequest.scopeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, scopeId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SwitchRoleRequest {\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    scopeId: ").append(toIndentedString(scopeId)).append("\n");
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

