// Owned API contract of iam.
package com.mysms.iam.api.model;

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
 * RoleGrant
 */

public class RoleGrant {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String roleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roleId;

  /**
   * Scope type (same values as iam.user_role.scope_type).
   */
  public enum ScopeTypeEnum {
    TENANT("tenant"),
    
    INSTITUTION("institution"),
    
    DEPARTMENT("department"),
    
    CLASS_SECTION("class_section"),
    
    COURSE("course"),
    
    LMS_COURSE("lms_course"),
    
    LMS_COHORT("lms_cohort");

    private final String value;

    ScopeTypeEnum(String value) {
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
    public static ScopeTypeEnum fromValue(String value) {
      for (ScopeTypeEnum b : ScopeTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeTypeEnum scopeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID scopeId;

  public RoleGrant() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RoleGrant(String roleCode, ScopeTypeEnum scopeType) {
    this.roleCode = roleCode;
    this.scopeType = scopeType;
  }

  public RoleGrant roleCode(String roleCode) {
    this.roleCode = roleCode;
    return this;
  }

  /**
   * Role code.
   * @return roleCode
   */
  @NotNull 
  @JsonProperty("roleCode")
  public String getRoleCode() {
    return roleCode;
  }

  @JsonProperty("roleCode")
  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public RoleGrant roleId(@Nullable UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * The role's id (output only: iam.SwitchRole takes it).
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

  public RoleGrant scopeType(ScopeTypeEnum scopeType) {
    this.scopeType = scopeType;
    return this;
  }

  /**
   * Scope type (same values as iam.user_role.scope_type).
   * @return scopeType
   */
  @NotNull 
  @JsonProperty("scopeType")
  public ScopeTypeEnum getScopeType() {
    return scopeType;
  }

  @JsonProperty("scopeType")
  public void setScopeType(ScopeTypeEnum scopeType) {
    this.scopeType = scopeType;
  }

  public RoleGrant scopeId(@Nullable UUID scopeId) {
    this.scopeId = scopeId;
    return this;
  }

  /**
   * Scope record.
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
    RoleGrant roleGrant = (RoleGrant) o;
    return Objects.equals(this.roleCode, roleGrant.roleCode) &&
        Objects.equals(this.roleId, roleGrant.roleId) &&
        Objects.equals(this.scopeType, roleGrant.scopeType) &&
        Objects.equals(this.scopeId, roleGrant.scopeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleCode, roleId, scopeType, scopeId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoleGrant {\n");
    sb.append("    roleCode: ").append(toIndentedString(roleCode)).append("\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    scopeType: ").append(toIndentedString(scopeType)).append("\n");
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

