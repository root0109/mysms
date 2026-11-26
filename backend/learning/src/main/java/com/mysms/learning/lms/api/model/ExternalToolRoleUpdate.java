// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ExternalToolRole that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ExternalToolRoleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> roleId;

  public ExternalToolRoleUpdate roleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Role.
   * @return roleId
   */
  
  @JsonProperty("roleId")
  public @Nullable Optional<UUID> getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalToolRoleUpdate externalToolRoleUpdate = (ExternalToolRoleUpdate) o;
    return Objects.equals(this.roleId, externalToolRoleUpdate.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalToolRoleUpdate {\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
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

