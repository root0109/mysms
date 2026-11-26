// Owned API contract of finance.
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
 * Fields of FeeHead that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class FeeHeadUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> defaultAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> ledgerAccountCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public FeeHeadUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide).
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public FeeHeadUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Fee head code.
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

  public FeeHeadUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Fee head name.
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

  public FeeHeadUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public FeeHeadUpdate defaultAmount(@Nullable Optional<String> defaultAmount) {
    this.defaultAmount = defaultAmount;
    return this;
  }

  /**
   * Default amount.
   * @return defaultAmount
   */
  
  @JsonProperty("defaultAmount")
  public @Nullable Optional<String> getDefaultAmount() {
    return defaultAmount;
  }

  @JsonProperty("defaultAmount")
  public void setDefaultAmount(@Nullable Optional<String> defaultAmount) {
    this.defaultAmount = defaultAmount;
  }

  public FeeHeadUpdate currencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency.
   * @return currencyId
   */
  
  @JsonProperty("currencyId")
  public @Nullable Optional<UUID> getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
  }

  public FeeHeadUpdate ledgerAccountCode(@Nullable Optional<String> ledgerAccountCode) {
    this.ledgerAccountCode = ledgerAccountCode;
    return this;
  }

  /**
   * General-ledger account code for accounting export.
   * @return ledgerAccountCode
   */
  
  @JsonProperty("ledgerAccountCode")
  public @Nullable Optional<String> getLedgerAccountCode() {
    return ledgerAccountCode;
  }

  @JsonProperty("ledgerAccountCode")
  public void setLedgerAccountCode(@Nullable Optional<String> ledgerAccountCode) {
    this.ledgerAccountCode = ledgerAccountCode;
  }

  public FeeHeadUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether billable.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
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

