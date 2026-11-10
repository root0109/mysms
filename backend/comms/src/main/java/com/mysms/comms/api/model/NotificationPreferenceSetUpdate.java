// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

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
 * Fields of NotificationPreferenceSet that can be updated (partial update; see updateMask).
 */

public class NotificationPreferenceSetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roleId;

  public NotificationPreferenceSetUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public NotificationPreferenceSetUpdate roleId(@Nullable UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Role the preferences apply to (NULL = all roles).
   * @return roleId
   */
  @Valid 
  @JsonProperty("roleId")
  public @Nullable UUID getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable UUID roleId) {
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
    NotificationPreferenceSetUpdate notificationPreferenceSetUpdate = (NotificationPreferenceSetUpdate) o;
    return Objects.equals(this.userId, notificationPreferenceSetUpdate.userId) &&
        Objects.equals(this.roleId, notificationPreferenceSetUpdate.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationPreferenceSetUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

