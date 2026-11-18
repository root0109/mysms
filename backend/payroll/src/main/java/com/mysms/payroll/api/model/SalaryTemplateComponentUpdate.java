// Owned API contract of payroll.
package com.mysms.payroll.api.model;

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
 * Fields of SalaryTemplateComponent that can be updated (partial update; see updateMask).
 */

public class SalaryTemplateComponentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID payComponentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID payCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String value;

  public SalaryTemplateComponentUpdate payComponentId(@Nullable UUID payComponentId) {
    this.payComponentId = payComponentId;
    return this;
  }

  /**
   * Component.
   * @return payComponentId
   */
  @Valid 
  @JsonProperty("payComponentId")
  public @Nullable UUID getPayComponentId() {
    return payComponentId;
  }

  @JsonProperty("payComponentId")
  public void setPayComponentId(@Nullable UUID payComponentId) {
    this.payComponentId = payComponentId;
  }

  public SalaryTemplateComponentUpdate payCategoryId(@Nullable UUID payCategoryId) {
    this.payCategoryId = payCategoryId;
    return this;
  }

  /**
   * Calculation basis.
   * @return payCategoryId
   */
  @Valid 
  @JsonProperty("payCategoryId")
  public @Nullable UUID getPayCategoryId() {
    return payCategoryId;
  }

  @JsonProperty("payCategoryId")
  public void setPayCategoryId(@Nullable UUID payCategoryId) {
    this.payCategoryId = payCategoryId;
  }

  public SalaryTemplateComponentUpdate value(@Nullable String value) {
    this.value = value;
    return this;
  }

  /**
   * Amount or rate for the calculation basis.
   * @return value
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("value")
  public @Nullable String getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(@Nullable String value) {
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
    SalaryTemplateComponentUpdate salaryTemplateComponentUpdate = (SalaryTemplateComponentUpdate) o;
    return Objects.equals(this.payComponentId, salaryTemplateComponentUpdate.payComponentId) &&
        Objects.equals(this.payCategoryId, salaryTemplateComponentUpdate.payCategoryId) &&
        Objects.equals(this.value, salaryTemplateComponentUpdate.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payComponentId, payCategoryId, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SalaryTemplateComponentUpdate {\n");
    sb.append("    payComponentId: ").append(toIndentedString(payComponentId)).append("\n");
    sb.append("    payCategoryId: ").append(toIndentedString(payCategoryId)).append("\n");
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

