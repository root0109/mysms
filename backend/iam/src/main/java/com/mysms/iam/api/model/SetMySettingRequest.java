// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api.model;

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
 * The caller&#39;s own value for a setting.
 */

public class SetMySettingRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String value;

  public SetMySettingRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetMySettingRequest(String value) {
    this.value = value;
  }

  public SetMySettingRequest value(String value) {
    this.value = value;
    return this;
  }

  /**
   * The value, as text in the setting's type.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetMySettingRequest setMySettingRequest = (SetMySettingRequest) o;
    return Objects.equals(this.value, setMySettingRequest.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetMySettingRequest {\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

