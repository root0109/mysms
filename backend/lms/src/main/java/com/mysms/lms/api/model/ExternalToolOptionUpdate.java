// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of ExternalToolOption that can be updated (partial update; see updateMask).
 */

public class ExternalToolOptionUpdate {

  /**
   * Whether the row is a tool type or a display placement.
   */
  public enum OptionKindEnum {
    TOOL_TYPE("tool_type"),
    
    PLACEMENT("placement");

    private final String value;

    OptionKindEnum(String value) {
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
    public static OptionKindEnum fromValue(String value) {
      for (OptionKindEnum b : OptionKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable OptionKindEnum optionKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String optionValue;

  public ExternalToolOptionUpdate optionKind(@Nullable OptionKindEnum optionKind) {
    this.optionKind = optionKind;
    return this;
  }

  /**
   * Whether the row is a tool type or a display placement.
   * @return optionKind
   */
  
  @JsonProperty("optionKind")
  public @Nullable OptionKindEnum getOptionKind() {
    return optionKind;
  }

  @JsonProperty("optionKind")
  public void setOptionKind(@Nullable OptionKindEnum optionKind) {
    this.optionKind = optionKind;
  }

  public ExternalToolOptionUpdate optionValue(@Nullable String optionValue) {
    this.optionValue = optionValue;
    return this;
  }

  /**
   * Tool type code, or placement (editor, content_page, connect).
   * @return optionValue
   */
  @Size(max = 40) 
  @JsonProperty("optionValue")
  public @Nullable String getOptionValue() {
    return optionValue;
  }

  @JsonProperty("optionValue")
  public void setOptionValue(@Nullable String optionValue) {
    this.optionValue = optionValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalToolOptionUpdate externalToolOptionUpdate = (ExternalToolOptionUpdate) o;
    return Objects.equals(this.optionKind, externalToolOptionUpdate.optionKind) &&
        Objects.equals(this.optionValue, externalToolOptionUpdate.optionValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(optionKind, optionValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalToolOptionUpdate {\n");
    sb.append("    optionKind: ").append(toIndentedString(optionKind)).append("\n");
    sb.append("    optionValue: ").append(toIndentedString(optionValue)).append("\n");
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

