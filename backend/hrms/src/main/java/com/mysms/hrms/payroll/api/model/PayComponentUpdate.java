// Owned API contract of payroll.
package com.mysms.hrms.payroll.api.model;

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
 * Fields of PayComponent that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PayComponentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  /**
   * Earning, deduction or employer-defined.
   */
  public enum ComponentTypeEnum {
    EARNING("earning"),
    
    DEDUCTION("deduction"),
    
    DEFINED("defined");

    private final String value;

    ComponentTypeEnum(String value) {
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
    public static ComponentTypeEnum fromValue(String value) {
      for (ComponentTypeEnum b : ComponentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ComponentTypeEnum> componentType;

  public PayComponentUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Component key used in formulas.
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

  public PayComponentUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Component name.
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

  public PayComponentUpdate componentType(@Nullable Optional<ComponentTypeEnum> componentType) {
    this.componentType = componentType;
    return this;
  }

  /**
   * Earning, deduction or employer-defined.
   * @return componentType
   */
  
  @JsonProperty("componentType")
  public @Nullable Optional<ComponentTypeEnum> getComponentType() {
    return componentType;
  }

  @JsonProperty("componentType")
  public void setComponentType(@Nullable Optional<ComponentTypeEnum> componentType) {
    this.componentType = componentType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PayComponentUpdate payComponentUpdate = (PayComponentUpdate) o;
    return Objects.equals(this.code, payComponentUpdate.code) &&
        Objects.equals(this.name, payComponentUpdate.name) &&
        Objects.equals(this.componentType, payComponentUpdate.componentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, name, componentType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PayComponentUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    componentType: ").append(toIndentedString(componentType)).append("\n");
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

