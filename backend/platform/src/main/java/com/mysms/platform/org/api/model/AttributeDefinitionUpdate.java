// Owned API contract of org.
package com.mysms.platform.org.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AttributeDefinition that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AttributeDefinitionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  /**
   * Data type of the value.
   */
  public enum DataTypeEnum {
    TEXT("text"),
    
    NUMBER("number"),
    
    DATE("date"),
    
    BOOLEAN("boolean"),
    
    EMAIL("email"),
    
    URL("url"),
    
    PHONE("phone"),
    
    SELECT("select");

    private final String value;

    DataTypeEnum(String value) {
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
    public static DataTypeEnum fromValue(String value) {
      for (DataTypeEnum b : DataTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull DataTypeEnum> dataType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public AttributeDefinitionUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Attribute label.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public AttributeDefinitionUpdate dataType(@Nullable Optional<DataTypeEnum> dataType) {
    this.dataType = dataType;
    return this;
  }

  /**
   * Data type of the value.
   * @return dataType
   */
  
  @JsonProperty("dataType")
  public @Nullable Optional<DataTypeEnum> getDataType() {
    return dataType;
  }

  @JsonProperty("dataType")
  public void setDataType(@Nullable Optional<DataTypeEnum> dataType) {
    this.dataType = dataType;
  }

  public AttributeDefinitionUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttributeDefinitionUpdate attributeDefinitionUpdate = (AttributeDefinitionUpdate) o;
    return Objects.equals(this.name, attributeDefinitionUpdate.name) &&
        Objects.equals(this.dataType, attributeDefinitionUpdate.dataType) &&
        Objects.equals(this.sortOrder, attributeDefinitionUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, dataType, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttributeDefinitionUpdate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    dataType: ").append(toIndentedString(dataType)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

