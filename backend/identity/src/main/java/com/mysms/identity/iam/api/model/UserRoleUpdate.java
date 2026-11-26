// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of UserRole that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class UserRoleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> roleId;

  /**
   * Scope kind the grant is limited to.
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
  private @Nullable Optional<@NotNull ScopeTypeEnum> scopeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> scopeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> validFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> validTo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public UserRoleUpdate roleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Role granted.
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

  public UserRoleUpdate scopeType(@Nullable Optional<ScopeTypeEnum> scopeType) {
    this.scopeType = scopeType;
    return this;
  }

  /**
   * Scope kind the grant is limited to.
   * @return scopeType
   */
  
  @JsonProperty("scopeType")
  public @Nullable Optional<ScopeTypeEnum> getScopeType() {
    return scopeType;
  }

  @JsonProperty("scopeType")
  public void setScopeType(@Nullable Optional<ScopeTypeEnum> scopeType) {
    this.scopeType = scopeType;
  }

  public UserRoleUpdate scopeId(@Nullable Optional<UUID> scopeId) {
    this.scopeId = scopeId;
    return this;
  }

  /**
   * Polymorphic scope record id (NULL when scope_type = tenant).
   * @return scopeId
   */
  
  @JsonProperty("scopeId")
  public @Nullable Optional<UUID> getScopeId() {
    return scopeId;
  }

  @JsonProperty("scopeId")
  public void setScopeId(@Nullable Optional<UUID> scopeId) {
    this.scopeId = scopeId;
  }

  public UserRoleUpdate validFrom(@Nullable Optional<LocalDate> validFrom) {
    this.validFrom = validFrom;
    return this;
  }

  /**
   * Grant start.
   * @return validFrom
   */
  
  @JsonProperty("validFrom")
  public @Nullable Optional<LocalDate> getValidFrom() {
    return validFrom;
  }

  @JsonProperty("validFrom")
  public void setValidFrom(@Nullable Optional<LocalDate> validFrom) {
    this.validFrom = validFrom;
  }

  public UserRoleUpdate validTo(@Nullable Optional<LocalDate> validTo) {
    this.validTo = validTo;
    return this;
  }

  /**
   * Grant end.
   * @return validTo
   */
  
  @JsonProperty("validTo")
  public @Nullable Optional<LocalDate> getValidTo() {
    return validTo;
  }

  @JsonProperty("validTo")
  public void setValidTo(@Nullable Optional<LocalDate> validTo) {
    this.validTo = validTo;
  }

  public UserRoleUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Grant currently in force (can be suspended without deleting).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserRoleUpdate userRoleUpdate = (UserRoleUpdate) o;
    return Objects.equals(this.roleId, userRoleUpdate.roleId) &&
        Objects.equals(this.scopeType, userRoleUpdate.scopeType) &&
        Objects.equals(this.scopeId, userRoleUpdate.scopeId) &&
        Objects.equals(this.validFrom, userRoleUpdate.validFrom) &&
        Objects.equals(this.validTo, userRoleUpdate.validTo) &&
        Objects.equals(this.isActive, userRoleUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, scopeType, scopeId, validFrom, validTo, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserRoleUpdate {\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    scopeType: ").append(toIndentedString(scopeType)).append("\n");
    sb.append("    scopeId: ").append(toIndentedString(scopeId)).append("\n");
    sb.append("    validFrom: ").append(toIndentedString(validFrom)).append("\n");
    sb.append("    validTo: ").append(toIndentedString(validTo)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

