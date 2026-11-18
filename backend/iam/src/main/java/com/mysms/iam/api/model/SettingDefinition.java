// Owned API contract of iam.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * A setting the product offers (theme, density, language...), with its type, allowed values, default, the scopes it may be set at and whether users may change it themselves. Product catalogue: read-only through the API.
 */

public class SettingDefinition {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> allowedValues = new ArrayList<>();

  /**
   * Gets or Sets allowedScopes
   */
  public enum AllowedScopesEnum {
    TENANT("tenant"),
    
    INSTITUTION("institution"),
    
    DEPARTMENT("department"),
    
    CLASS_SECTION("class_section"),
    
    USER("user");

    private final String value;

    AllowedScopesEnum(String value) {
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
    public static AllowedScopesEnum fromValue(String value) {
      for (AllowedScopesEnum b : AllowedScopesEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<AllowedScopesEnum> allowedScopes = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String settingKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String category;

  /**
   * Type of the value.
   */
  public enum ValueTypeEnum {
    STRING("string"),
    
    BOOLEAN("boolean"),
    
    INTEGER("integer"),
    
    CHOICE("choice");

    private final String value;

    ValueTypeEnum(String value) {
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
    public static ValueTypeEnum fromValue(String value) {
      for (ValueTypeEnum b : ValueTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ValueTypeEnum valueType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String defaultValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer minValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean userEditable;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public SettingDefinition() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SettingDefinition(String settingKey, ValueTypeEnum valueType) {
    this.settingKey = settingKey;
    this.valueType = valueType;
  }

  public SettingDefinition allowedValues(List<String> allowedValues) {
    this.allowedValues = allowedValues;
    return this;
  }

  public SettingDefinition addAllowedValuesItem(String allowedValuesItem) {
    if (this.allowedValues == null) {
      this.allowedValues = new ArrayList<>();
    }
    this.allowedValues.add(allowedValuesItem);
    return this;
  }

  /**
   * Allowed values of a choice setting.
   * @return allowedValues
   */
  
  @JsonProperty("allowedValues")
  public List<String> getAllowedValues() {
    return allowedValues;
  }

  @JsonProperty("allowedValues")
  public void setAllowedValues(List<String> allowedValues) {
    this.allowedValues = allowedValues;
  }

  public SettingDefinition allowedScopes(List<AllowedScopesEnum> allowedScopes) {
    this.allowedScopes = allowedScopes;
    return this;
  }

  public SettingDefinition addAllowedScopesItem(AllowedScopesEnum allowedScopesItem) {
    if (this.allowedScopes == null) {
      this.allowedScopes = new ArrayList<>();
    }
    this.allowedScopes.add(allowedScopesItem);
    return this;
  }

  /**
   * Scopes at which a value may be set.
   * @return allowedScopes
   */
  
  @JsonProperty("allowedScopes")
  public List<AllowedScopesEnum> getAllowedScopes() {
    return allowedScopes;
  }

  @JsonProperty("allowedScopes")
  public void setAllowedScopes(List<AllowedScopesEnum> allowedScopes) {
    this.allowedScopes = allowedScopes;
  }

  public SettingDefinition id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public SettingDefinition settingKey(String settingKey) {
    this.settingKey = settingKey;
    return this;
  }

  /**
   * Stable key, e.g. ui.theme.
   * @return settingKey
   */
  @Size(max = 100) 
  @JsonProperty("settingKey")
  public String getSettingKey() {
    return settingKey;
  }

  @JsonProperty("settingKey")
  public void setSettingKey(String settingKey) {
    this.settingKey = settingKey;
  }

  public SettingDefinition category(@Nullable String category) {
    this.category = category;
    return this;
  }

  /**
   * Grouping on the settings screens, e.g. appearance.
   * @return category
   */
  @Size(max = 60) 
  @JsonProperty("category")
  public @Nullable String getCategory() {
    return category;
  }

  @JsonProperty("category")
  public void setCategory(@Nullable String category) {
    this.category = category;
  }

  public SettingDefinition valueType(ValueTypeEnum valueType) {
    this.valueType = valueType;
    return this;
  }

  /**
   * Type of the value.
   * @return valueType
   */
  
  @JsonProperty("valueType")
  public ValueTypeEnum getValueType() {
    return valueType;
  }

  @JsonProperty("valueType")
  public void setValueType(ValueTypeEnum valueType) {
    this.valueType = valueType;
  }

  public SettingDefinition defaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * Value when no scope sets one.
   * @return defaultValue
   */
  @Size(max = 500) 
  @JsonProperty("defaultValue")
  public @Nullable String getDefaultValue() {
    return defaultValue;
  }

  @JsonProperty("defaultValue")
  public void setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public SettingDefinition minValue(@Nullable Integer minValue) {
    this.minValue = minValue;
    return this;
  }

  /**
   * Smallest allowed value (integer settings).
   * @return minValue
   */
  
  @JsonProperty("minValue")
  public @Nullable Integer getMinValue() {
    return minValue;
  }

  @JsonProperty("minValue")
  public void setMinValue(@Nullable Integer minValue) {
    this.minValue = minValue;
  }

  public SettingDefinition maxValue(@Nullable Integer maxValue) {
    this.maxValue = maxValue;
    return this;
  }

  /**
   * Largest allowed value (integer settings).
   * @return maxValue
   */
  
  @JsonProperty("maxValue")
  public @Nullable Integer getMaxValue() {
    return maxValue;
  }

  @JsonProperty("maxValue")
  public void setMaxValue(@Nullable Integer maxValue) {
    this.maxValue = maxValue;
  }

  public SettingDefinition userEditable(@Nullable Boolean userEditable) {
    this.userEditable = userEditable;
    return this;
  }

  /**
   * Whether users may set it for themselves.
   * @return userEditable
   */
  
  @JsonProperty("userEditable")
  public @Nullable Boolean getUserEditable() {
    return userEditable;
  }

  @JsonProperty("userEditable")
  public void setUserEditable(@Nullable Boolean userEditable) {
    this.userEditable = userEditable;
  }

  public SettingDefinition description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * What the setting changes.
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

  public SettingDefinition sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order within its category.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public SettingDefinition createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public SettingDefinition createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public SettingDefinition updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public SettingDefinition updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public SettingDefinition etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SettingDefinition settingDefinition = (SettingDefinition) o;
    return Objects.equals(this.allowedValues, settingDefinition.allowedValues) &&
        Objects.equals(this.allowedScopes, settingDefinition.allowedScopes) &&
        Objects.equals(this.id, settingDefinition.id) &&
        Objects.equals(this.settingKey, settingDefinition.settingKey) &&
        Objects.equals(this.category, settingDefinition.category) &&
        Objects.equals(this.valueType, settingDefinition.valueType) &&
        Objects.equals(this.defaultValue, settingDefinition.defaultValue) &&
        Objects.equals(this.minValue, settingDefinition.minValue) &&
        Objects.equals(this.maxValue, settingDefinition.maxValue) &&
        Objects.equals(this.userEditable, settingDefinition.userEditable) &&
        Objects.equals(this.description, settingDefinition.description) &&
        Objects.equals(this.sortOrder, settingDefinition.sortOrder) &&
        Objects.equals(this.createTime, settingDefinition.createTime) &&
        Objects.equals(this.createdBy, settingDefinition.createdBy) &&
        Objects.equals(this.updateTime, settingDefinition.updateTime) &&
        Objects.equals(this.updatedBy, settingDefinition.updatedBy) &&
        Objects.equals(this.etag, settingDefinition.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedValues, allowedScopes, id, settingKey, category, valueType, defaultValue, minValue, maxValue, userEditable, description, sortOrder, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SettingDefinition {\n");
    sb.append("    allowedValues: ").append(toIndentedString(allowedValues)).append("\n");
    sb.append("    allowedScopes: ").append(toIndentedString(allowedScopes)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    settingKey: ").append(toIndentedString(settingKey)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    valueType: ").append(toIndentedString(valueType)).append("\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    minValue: ").append(toIndentedString(minValue)).append("\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    userEditable: ").append(toIndentedString(userEditable)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

