// Owned API contract of org.
package com.mysms.org.api.model;

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
 * Fields of Department that can be updated (partial update; see updateMask).
 */

public class DepartmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentDepartmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public DepartmentUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public DepartmentUpdate parentDepartmentId(@Nullable UUID parentDepartmentId) {
    this.parentDepartmentId = parentDepartmentId;
    return this;
  }

  /**
   * Parent department.
   * @return parentDepartmentId
   */
  @Valid 
  @JsonProperty("parentDepartmentId")
  public @Nullable UUID getParentDepartmentId() {
    return parentDepartmentId;
  }

  @JsonProperty("parentDepartmentId")
  public void setParentDepartmentId(@Nullable UUID parentDepartmentId) {
    this.parentDepartmentId = parentDepartmentId;
  }

  public DepartmentUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Department code.
   * @return code
   */
  @Size(max = 30) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public DepartmentUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Department name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public DepartmentUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether in use.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DepartmentUpdate departmentUpdate = (DepartmentUpdate) o;
    return Objects.equals(this.institutionId, departmentUpdate.institutionId) &&
        Objects.equals(this.parentDepartmentId, departmentUpdate.parentDepartmentId) &&
        Objects.equals(this.code, departmentUpdate.code) &&
        Objects.equals(this.name, departmentUpdate.name) &&
        Objects.equals(this.isActive, departmentUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, parentDepartmentId, code, name, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DepartmentUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    parentDepartmentId: ").append(toIndentedString(parentDepartmentId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

