// Owned API contract of master.
package com.mysms.platform.master.api.model;

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
 * Fields of Currency that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CurrencyUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 3) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 8) String> symbol;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> minorUnits;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public CurrencyUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * ISO 4217 code, e.g. INR, USD.
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

  public CurrencyUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Currency name.
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

  public CurrencyUpdate symbol(@Nullable Optional<String> symbol) {
    this.symbol = symbol;
    return this;
  }

  /**
   * Display symbol.
   * @return symbol
   */
  
  @JsonProperty("symbol")
  public @Nullable Optional<String> getSymbol() {
    return symbol;
  }

  @JsonProperty("symbol")
  public void setSymbol(@Nullable Optional<String> symbol) {
    this.symbol = symbol;
  }

  public CurrencyUpdate minorUnits(@Nullable Optional<Integer> minorUnits) {
    this.minorUnits = minorUnits;
    return this;
  }

  /**
   * Number of decimal places.
   * @return minorUnits
   */
  
  @JsonProperty("minorUnits")
  public @Nullable Optional<Integer> getMinorUnits() {
    return minorUnits;
  }

  @JsonProperty("minorUnits")
  public void setMinorUnits(@Nullable Optional<Integer> minorUnits) {
    this.minorUnits = minorUnits;
  }

  public CurrencyUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
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
    CurrencyUpdate currencyUpdate = (CurrencyUpdate) o;
    return Objects.equals(this.code, currencyUpdate.code) &&
        Objects.equals(this.name, currencyUpdate.name) &&
        Objects.equals(this.symbol, currencyUpdate.symbol) &&
        Objects.equals(this.minorUnits, currencyUpdate.minorUnits) &&
        Objects.equals(this.isActive, currencyUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, name, symbol, minorUnits, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CurrencyUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    symbol: ").append(toIndentedString(symbol)).append("\n");
    sb.append("    minorUnits: ").append(toIndentedString(minorUnits)).append("\n");
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

