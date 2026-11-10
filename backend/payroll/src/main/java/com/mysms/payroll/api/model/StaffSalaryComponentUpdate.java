// Owned API contract of payroll (JAVA-CODING-RULES §7); first materialized from api/openapi/payroll.yaml on 2026-11-10.
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
 * Fields of StaffSalaryComponent that can be updated (partial update; see updateMask).
 */

public class StaffSalaryComponentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID salaryTemplateComponentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String amount;

  public StaffSalaryComponentUpdate salaryTemplateComponentId(@Nullable UUID salaryTemplateComponentId) {
    this.salaryTemplateComponentId = salaryTemplateComponentId;
    return this;
  }

  /**
   * Template component.
   * @return salaryTemplateComponentId
   */
  @Valid 
  @JsonProperty("salaryTemplateComponentId")
  public @Nullable UUID getSalaryTemplateComponentId() {
    return salaryTemplateComponentId;
  }

  @JsonProperty("salaryTemplateComponentId")
  public void setSalaryTemplateComponentId(@Nullable UUID salaryTemplateComponentId) {
    this.salaryTemplateComponentId = salaryTemplateComponentId;
  }

  public StaffSalaryComponentUpdate amount(@Nullable String amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Amount.
   * @return amount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("amount")
  public @Nullable String getAmount() {
    return amount;
  }

  @JsonProperty("amount")
  public void setAmount(@Nullable String amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffSalaryComponentUpdate staffSalaryComponentUpdate = (StaffSalaryComponentUpdate) o;
    return Objects.equals(this.salaryTemplateComponentId, staffSalaryComponentUpdate.salaryTemplateComponentId) &&
        Objects.equals(this.amount, staffSalaryComponentUpdate.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(salaryTemplateComponentId, amount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffSalaryComponentUpdate {\n");
    sb.append("    salaryTemplateComponentId: ").append(toIndentedString(salaryTemplateComponentId)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
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

