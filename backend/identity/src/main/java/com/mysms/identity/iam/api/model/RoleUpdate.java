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
 * Fields of Role that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RoleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 80) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isSystem;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> authorityLevel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> parentRoleId;

  public RoleUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Role code, e.g. principal, class_teacher, lms_course_creator, lms_evaluator.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public RoleUpdate displayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Display name.
   * @return displayName
   */
  
  @JsonProperty("displayName")
  public @Nullable Optional<String> getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
  }

  public RoleUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Purpose of the role.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public RoleUpdate isSystem(@Nullable Optional<Boolean> isSystem) {
    this.isSystem = isSystem;
    return this;
  }

  /**
   * Platform-managed role that tenants cannot edit.
   * @return isSystem
   */
  
  @JsonProperty("isSystem")
  public @Nullable Optional<Boolean> getIsSystem() {
    return isSystem;
  }

  @JsonProperty("isSystem")
  public void setIsSystem(@Nullable Optional<Boolean> isSystem) {
    this.isSystem = isSystem;
  }

  public RoleUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Role can be granted.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public RoleUpdate authorityLevel(@Nullable Optional<Integer> authorityLevel) {
    this.authorityLevel = authorityLevel;
    return this;
  }

  /**
   * Rank used to decide which roles a user may grant or manage (lower = more authority).
   * @return authorityLevel
   */
  
  @JsonProperty("authorityLevel")
  public @Nullable Optional<Integer> getAuthorityLevel() {
    return authorityLevel;
  }

  @JsonProperty("authorityLevel")
  public void setAuthorityLevel(@Nullable Optional<Integer> authorityLevel) {
    this.authorityLevel = authorityLevel;
  }

  public RoleUpdate parentRoleId(@Nullable Optional<UUID> parentRoleId) {
    this.parentRoleId = parentRoleId;
    return this;
  }

  /**
   * Parent role (inherits its permissions).
   * @return parentRoleId
   */
  
  @JsonProperty("parentRoleId")
  public @Nullable Optional<UUID> getParentRoleId() {
    return parentRoleId;
  }

  @JsonProperty("parentRoleId")
  public void setParentRoleId(@Nullable Optional<UUID> parentRoleId) {
    this.parentRoleId = parentRoleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoleUpdate roleUpdate = (RoleUpdate) o;
    return Objects.equals(this.code, roleUpdate.code) &&
        Objects.equals(this.displayName, roleUpdate.displayName) &&
        Objects.equals(this.description, roleUpdate.description) &&
        Objects.equals(this.isSystem, roleUpdate.isSystem) &&
        Objects.equals(this.isActive, roleUpdate.isActive) &&
        Objects.equals(this.authorityLevel, roleUpdate.authorityLevel) &&
        Objects.equals(this.parentRoleId, roleUpdate.parentRoleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, displayName, description, isSystem, isActive, authorityLevel, parentRoleId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoleUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isSystem: ").append(toIndentedString(isSystem)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    authorityLevel: ").append(toIndentedString(authorityLevel)).append("\n");
    sb.append("    parentRoleId: ").append(toIndentedString(parentRoleId)).append("\n");
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

