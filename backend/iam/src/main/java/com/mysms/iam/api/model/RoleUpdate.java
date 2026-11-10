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
 * Fields of Role that can be updated (partial update; see updateMask).
 */

public class RoleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isSystem;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer authorityLevel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentRoleId;

  public RoleUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Role code, e.g. principal, class_teacher, lms_course_creator, lms_evaluator.
   * @return code
   */
  @Size(max = 80) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public RoleUpdate displayName(@Nullable String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Display name.
   * @return displayName
   */
  @Size(max = 120) 
  @JsonProperty("displayName")
  public @Nullable String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
  }

  public RoleUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Purpose of the role.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public RoleUpdate isSystem(@Nullable Boolean isSystem) {
    this.isSystem = isSystem;
    return this;
  }

  /**
   * Platform-managed role that tenants cannot edit.
   * @return isSystem
   */
  
  @JsonProperty("isSystem")
  public @Nullable Boolean getIsSystem() {
    return isSystem;
  }

  @JsonProperty("isSystem")
  public void setIsSystem(@Nullable Boolean isSystem) {
    this.isSystem = isSystem;
  }

  public RoleUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Role can be granted.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public RoleUpdate authorityLevel(@Nullable Integer authorityLevel) {
    this.authorityLevel = authorityLevel;
    return this;
  }

  /**
   * Rank used to decide which roles a user may grant or manage (lower = more authority).
   * @return authorityLevel
   */
  
  @JsonProperty("authorityLevel")
  public @Nullable Integer getAuthorityLevel() {
    return authorityLevel;
  }

  @JsonProperty("authorityLevel")
  public void setAuthorityLevel(@Nullable Integer authorityLevel) {
    this.authorityLevel = authorityLevel;
  }

  public RoleUpdate parentRoleId(@Nullable UUID parentRoleId) {
    this.parentRoleId = parentRoleId;
    return this;
  }

  /**
   * Parent role (inherits its permissions).
   * @return parentRoleId
   */
  @Valid 
  @JsonProperty("parentRoleId")
  public @Nullable UUID getParentRoleId() {
    return parentRoleId;
  }

  @JsonProperty("parentRoleId")
  public void setParentRoleId(@Nullable UUID parentRoleId) {
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

