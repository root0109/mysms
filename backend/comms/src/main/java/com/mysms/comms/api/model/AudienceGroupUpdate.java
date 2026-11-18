// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of AudienceGroup that can be updated (partial update; see updateMask).
 */

public class AudienceGroupUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  /**
   * Scope.
   */
  public enum GroupScopeEnum {
    GLOBAL("global"),
    
    propertyClass("class");

    private final String value;

    GroupScopeEnum(String value) {
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
    public static GroupScopeEnum fromValue(String value) {
      for (GroupScopeEnum b : GroupScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GroupScopeEnum groupScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public AudienceGroupUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide group).
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

  public AudienceGroupUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (scope = class).
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public AudienceGroupUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Group name.
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

  public AudienceGroupUpdate groupScope(@Nullable GroupScopeEnum groupScope) {
    this.groupScope = groupScope;
    return this;
  }

  /**
   * Scope.
   * @return groupScope
   */
  
  @JsonProperty("groupScope")
  public @Nullable GroupScopeEnum getGroupScope() {
    return groupScope;
  }

  @JsonProperty("groupScope")
  public void setGroupScope(@Nullable GroupScopeEnum groupScope) {
    this.groupScope = groupScope;
  }

  public AudienceGroupUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Active.
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
    AudienceGroupUpdate audienceGroupUpdate = (AudienceGroupUpdate) o;
    return Objects.equals(this.institutionId, audienceGroupUpdate.institutionId) &&
        Objects.equals(this.classSectionId, audienceGroupUpdate.classSectionId) &&
        Objects.equals(this.name, audienceGroupUpdate.name) &&
        Objects.equals(this.groupScope, audienceGroupUpdate.groupScope) &&
        Objects.equals(this.isActive, audienceGroupUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, classSectionId, name, groupScope, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AudienceGroupUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    groupScope: ").append(toIndentedString(groupScope)).append("\n");
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

