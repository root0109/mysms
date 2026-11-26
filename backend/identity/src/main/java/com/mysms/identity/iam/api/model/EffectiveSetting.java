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
 * The caller&#39;s effective value of one setting and where it comes from.
 */

public class EffectiveSetting {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String settingKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String value;

  /**
   * Level the value comes from.
   */
  public enum SourceScopeEnum {
    DEFAULT("default"),
    
    TENANT("tenant"),
    
    INSTITUTION("institution"),
    
    DEPARTMENT("department"),
    
    CLASS_SECTION("class_section"),
    
    USER("user");

    private final String value;

    SourceScopeEnum(String value) {
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
    public static SourceScopeEnum fromValue(String value) {
      for (SourceScopeEnum b : SourceScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private SourceScopeEnum sourceScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID sourceScopeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean locked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean userEditable;

  public EffectiveSetting() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EffectiveSetting(String settingKey, String value, SourceScopeEnum sourceScope, Boolean locked, Boolean userEditable) {
    this.settingKey = settingKey;
    this.value = value;
    this.sourceScope = sourceScope;
    this.locked = locked;
    this.userEditable = userEditable;
  }

  public EffectiveSetting settingKey(String settingKey) {
    this.settingKey = settingKey;
    return this;
  }

  /**
   * The setting.
   * @return settingKey
   */
  @NotNull @Size(max = 100) 
  @JsonProperty("settingKey")
  public String getSettingKey() {
    return settingKey;
  }

  @JsonProperty("settingKey")
  public void setSettingKey(String settingKey) {
    this.settingKey = settingKey;
  }

  public EffectiveSetting value(String value) {
    this.value = value;
    return this;
  }

  /**
   * Effective value.
   * @return value
   */
  @NotNull @Size(max = 500) 
  @JsonProperty("value")
  public String getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(String value) {
    this.value = value;
  }

  public EffectiveSetting sourceScope(SourceScopeEnum sourceScope) {
    this.sourceScope = sourceScope;
    return this;
  }

  /**
   * Level the value comes from.
   * @return sourceScope
   */
  @NotNull 
  @JsonProperty("sourceScope")
  public SourceScopeEnum getSourceScope() {
    return sourceScope;
  }

  @JsonProperty("sourceScope")
  public void setSourceScope(SourceScopeEnum sourceScope) {
    this.sourceScope = sourceScope;
  }

  public EffectiveSetting sourceScopeId(@Nullable UUID sourceScopeId) {
    this.sourceScopeId = sourceScopeId;
    return this;
  }

  /**
   * Institution, department, class section or user the value comes from.
   * @return sourceScopeId
   */
  @Valid 
  @JsonProperty("sourceScopeId")
  public @Nullable UUID getSourceScopeId() {
    return sourceScopeId;
  }

  @JsonProperty("sourceScopeId")
  public void setSourceScopeId(@Nullable UUID sourceScopeId) {
    this.sourceScopeId = sourceScopeId;
  }

  public EffectiveSetting locked(Boolean locked) {
    this.locked = locked;
    return this;
  }

  /**
   * A broader scope locked the value; the caller cannot change it.
   * @return locked
   */
  @NotNull 
  @JsonProperty("locked")
  public Boolean getLocked() {
    return locked;
  }

  @JsonProperty("locked")
  public void setLocked(Boolean locked) {
    this.locked = locked;
  }

  public EffectiveSetting userEditable(Boolean userEditable) {
    this.userEditable = userEditable;
    return this;
  }

  /**
   * The caller may set this setting for themselves (when not locked).
   * @return userEditable
   */
  @NotNull 
  @JsonProperty("userEditable")
  public Boolean getUserEditable() {
    return userEditable;
  }

  @JsonProperty("userEditable")
  public void setUserEditable(Boolean userEditable) {
    this.userEditable = userEditable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EffectiveSetting effectiveSetting = (EffectiveSetting) o;
    return Objects.equals(this.settingKey, effectiveSetting.settingKey) &&
        Objects.equals(this.value, effectiveSetting.value) &&
        Objects.equals(this.sourceScope, effectiveSetting.sourceScope) &&
        Objects.equals(this.sourceScopeId, effectiveSetting.sourceScopeId) &&
        Objects.equals(this.locked, effectiveSetting.locked) &&
        Objects.equals(this.userEditable, effectiveSetting.userEditable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(settingKey, value, sourceScope, sourceScopeId, locked, userEditable);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EffectiveSetting {\n");
    sb.append("    settingKey: ").append(toIndentedString(settingKey)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    sourceScope: ").append(toIndentedString(sourceScope)).append("\n");
    sb.append("    sourceScopeId: ").append(toIndentedString(sourceScopeId)).append("\n");
    sb.append("    locked: ").append(toIndentedString(locked)).append("\n");
    sb.append("    userEditable: ").append(toIndentedString(userEditable)).append("\n");
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

