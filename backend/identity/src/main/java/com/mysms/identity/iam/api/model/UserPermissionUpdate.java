// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of UserPermission that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class UserPermissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> permissionId;

  /**
   * Whether the permission is added or removed.
   */
  public enum EffectEnum {
    GRANT("grant"),
    
    DENY("deny");

    private final String value;

    EffectEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static EffectEnum fromValue(String value) {
      for (EffectEnum b : EffectEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull EffectEnum> effect;

  public UserPermissionUpdate permissionId(@Nullable Optional<UUID> permissionId) {
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

  public UserPermissionUpdate effect(@Nullable Optional<EffectEnum> effect) {
    this.effect = effect;
    return this;
  }

  /**
   * Whether the permission is added or removed.
   * @return effect
   */
  
  @JsonProperty("effect")
  public @Nullable Optional<EffectEnum> getEffect() {
    return effect;
  }

  @JsonProperty("effect")
  public void setEffect(@Nullable Optional<EffectEnum> effect) {
    this.effect = effect;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPermissionUpdate userPermissionUpdate = (UserPermissionUpdate) o;
    return Objects.equals(this.permissionId, userPermissionUpdate.permissionId) &&
        Objects.equals(this.effect, userPermissionUpdate.effect);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionId, effect);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserPermissionUpdate {\n");
    sb.append("    permissionId: ").append(toIndentedString(permissionId)).append("\n");
    sb.append("    effect: ").append(toIndentedString(effect)).append("\n");
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

