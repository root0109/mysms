// Owned API contract of payroll.
package com.mysms.hrms.payroll.api.model;

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
 * Fields of StaffSalaryComponent that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StaffSalaryComponentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> salaryTemplateComponentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> amount;

  public StaffSalaryComponentUpdate salaryTemplateComponentId(@Nullable Optional<UUID> salaryTemplateComponentId) {
    this.salaryTemplateComponentId = salaryTemplateComponentId;
    return this;
  }

  /**
   * Template component.
   * @return salaryTemplateComponentId
   */
  
  @JsonProperty("salaryTemplateComponentId")
  public @Nullable Optional<UUID> getSalaryTemplateComponentId() {
    return salaryTemplateComponentId;
  }

  @JsonProperty("salaryTemplateComponentId")
  public void setSalaryTemplateComponentId(@Nullable Optional<UUID> salaryTemplateComponentId) {
    this.salaryTemplateComponentId = salaryTemplateComponentId;
  }

  public StaffSalaryComponentUpdate amount(@Nullable Optional<String> amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Amount.
   * @return amount
   */
  
  @JsonProperty("amount")
  public @Nullable Optional<String> getAmount() {
    return amount;
  }

  @JsonProperty("amount")
  public void setAmount(@Nullable Optional<String> amount) {
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

