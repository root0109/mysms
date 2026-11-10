// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

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
 * Fields of FeeHead that can be updated (partial update; see updateMask).
 */

public class FeeHeadUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String defaultAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String ledgerAccountCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public FeeHeadUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide).
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public FeeHeadUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Fee head code.
   * @return code
   */
  @Size(max = 40) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public FeeHeadUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Fee head name.
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

  public FeeHeadUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

  public FeeHeadUpdate defaultAmount(@Nullable String defaultAmount) {
    this.defaultAmount = defaultAmount;
    return this;
  }

  /**
   * Default amount.
   * @return defaultAmount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("defaultAmount")
  public @Nullable String getDefaultAmount() {
    return defaultAmount;
  }

  @JsonProperty("defaultAmount")
  public void setDefaultAmount(@Nullable String defaultAmount) {
    this.defaultAmount = defaultAmount;
  }

  public FeeHeadUpdate currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency.
   * @return currencyId
   */
  @Valid 
  @JsonProperty("currencyId")
  public @Nullable UUID getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
  }

  public FeeHeadUpdate ledgerAccountCode(@Nullable String ledgerAccountCode) {
    this.ledgerAccountCode = ledgerAccountCode;
    return this;
  }

  /**
   * General-ledger account code for accounting export.
   * @return ledgerAccountCode
   */
  @Size(max = 40) 
  @JsonProperty("ledgerAccountCode")
  public @Nullable String getLedgerAccountCode() {
    return ledgerAccountCode;
  }

  @JsonProperty("ledgerAccountCode")
  public void setLedgerAccountCode(@Nullable String ledgerAccountCode) {
    this.ledgerAccountCode = ledgerAccountCode;
  }

  public FeeHeadUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether billable.
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
    FeeHeadUpdate feeHeadUpdate = (FeeHeadUpdate) o;
    return Objects.equals(this.institutionId, feeHeadUpdate.institutionId) &&
        Objects.equals(this.code, feeHeadUpdate.code) &&
        Objects.equals(this.name, feeHeadUpdate.name) &&
        Objects.equals(this.description, feeHeadUpdate.description) &&
        Objects.equals(this.defaultAmount, feeHeadUpdate.defaultAmount) &&
        Objects.equals(this.currencyId, feeHeadUpdate.currencyId) &&
        Objects.equals(this.ledgerAccountCode, feeHeadUpdate.ledgerAccountCode) &&
        Objects.equals(this.isActive, feeHeadUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, code, name, description, defaultAmount, currencyId, ledgerAccountCode, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeeHeadUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    defaultAmount: ").append(toIndentedString(defaultAmount)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    ledgerAccountCode: ").append(toIndentedString(ledgerAccountCode)).append("\n");
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

