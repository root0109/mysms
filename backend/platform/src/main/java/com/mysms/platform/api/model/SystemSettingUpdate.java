// Owned API contract of platform.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of SystemSetting that can be updated (partial update; see updateMask).
 */

public class SystemSettingUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String settingKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String value;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String inputType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public SystemSettingUpdate settingKey(@Nullable String settingKey) {
    this.settingKey = settingKey;
    return this;
  }

  /**
   * Unique key.
   * @return settingKey
   */
  @Size(max = 120) 
  @JsonProperty("settingKey")
  public @Nullable String getSettingKey() {
    return settingKey;
  }

  @JsonProperty("settingKey")
  public void setSettingKey(@Nullable String settingKey) {
    this.settingKey = settingKey;
  }

  public SystemSettingUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Display name.
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

  public SystemSettingUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Explanation.
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

  public SystemSettingUpdate value(@Nullable String value) {
    this.value = value;
    return this;
  }

  /**
   * Current value (serialised).
   * @return value
   */
  
  @JsonProperty("value")
  public @Nullable String getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(@Nullable String value) {
    this.value = value;
  }

  public SystemSettingUpdate inputType(@Nullable String inputType) {
    this.inputType = inputType;
    return this;
  }

  /**
   * Editor control used for the value (text, bool, select, json).
   * @return inputType
   */
  @Size(max = 50) 
  @JsonProperty("inputType")
  public @Nullable String getInputType() {
    return inputType;
  }

  @JsonProperty("inputType")
  public void setInputType(@Nullable String inputType) {
    this.inputType = inputType;
  }

  public SystemSettingUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether the setting is in effect.
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
    SystemSettingUpdate systemSettingUpdate = (SystemSettingUpdate) o;
    return Objects.equals(this.settingKey, systemSettingUpdate.settingKey) &&
        Objects.equals(this.name, systemSettingUpdate.name) &&
        Objects.equals(this.description, systemSettingUpdate.description) &&
        Objects.equals(this.value, systemSettingUpdate.value) &&
        Objects.equals(this.inputType, systemSettingUpdate.inputType) &&
        Objects.equals(this.isActive, systemSettingUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(settingKey, name, description, value, inputType, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SystemSettingUpdate {\n");
    sb.append("    settingKey: ").append(toIndentedString(settingKey)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    inputType: ").append(toIndentedString(inputType)).append("\n");
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

