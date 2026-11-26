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
 * Fields of RolePermission that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RolePermissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> permissionId;

  public RolePermissionUpdate permissionId(@Nullable Optional<UUID> permissionId) {
    this.permissionId = permissionId;
    return this;
  }

  /**
   * Permission.
   * @return permissionId
   */
  
  @JsonProperty("permissionId")
  public @Nullable Optional<UUID> getPermissionId() {
    return permissionId;
  }

  @JsonProperty("permissionId")
  public void setPermissionId(@Nullable Optional<UUID> permissionId) {
    this.permissionId = permissionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RolePermissionUpdate rolePermissionUpdate = (RolePermissionUpdate) o;
    return Objects.equals(this.permissionId, rolePermissionUpdate.permissionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RolePermissionUpdate {\n");
    sb.append("    permissionId: ").append(toIndentedString(permissionId)).append("\n");
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

