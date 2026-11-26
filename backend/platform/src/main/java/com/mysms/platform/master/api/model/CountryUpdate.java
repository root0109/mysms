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
 * Fields of Country that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CountryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 2) String> iso2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 3) String> iso3;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> shortName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 8) String> phonePrefix;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public CountryUpdate iso2(@Nullable Optional<String> iso2) {
    this.iso2 = iso2;
    return this;
  }

  /**
   * ISO 3166-1 alpha-2 code.
   * @return iso2
   */
  
  @JsonProperty("iso2")
  public @Nullable Optional<String> getIso2() {
    return iso2;
  }

  @JsonProperty("iso2")
  public void setIso2(@Nullable Optional<String> iso2) {
    this.iso2 = iso2;
  }

  public CountryUpdate iso3(@Nullable Optional<String> iso3) {
    this.iso3 = iso3;
    return this;
  }

  /**
   * ISO 3166-1 alpha-3 code.
   * @return iso3
   */
  
  @JsonProperty("iso3")
  public @Nullable Optional<String> getIso3() {
    return iso3;
  }

  @JsonProperty("iso3")
  public void setIso3(@Nullable Optional<String> iso3) {
    this.iso3 = iso3;
  }

  public CountryUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Country name.
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

  public CountryUpdate shortName(@Nullable Optional<String> shortName) {
    this.shortName = shortName;
    return this;
  }

  /**
   * Short/common name.
   * @return shortName
   */
  
  @JsonProperty("shortName")
  public @Nullable Optional<String> getShortName() {
    return shortName;
  }

  @JsonProperty("shortName")
  public void setShortName(@Nullable Optional<String> shortName) {
    this.shortName = shortName;
  }

  public CountryUpdate phonePrefix(@Nullable Optional<String> phonePrefix) {
    this.phonePrefix = phonePrefix;
    return this;
  }

  /**
   * International dialling prefix, e.g. +91.
   * @return phonePrefix
   */
  
  @JsonProperty("phonePrefix")
  public @Nullable Optional<String> getPhonePrefix() {
    return phonePrefix;
  }

  @JsonProperty("phonePrefix")
  public void setPhonePrefix(@Nullable Optional<String> phonePrefix) {
    this.phonePrefix = phonePrefix;
  }

  public CountryUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order in pick-lists.
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

  public CountryUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether the country is selectable.
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
    CountryUpdate countryUpdate = (CountryUpdate) o;
    return Objects.equals(this.iso2, countryUpdate.iso2) &&
        Objects.equals(this.iso3, countryUpdate.iso3) &&
        Objects.equals(this.name, countryUpdate.name) &&
        Objects.equals(this.shortName, countryUpdate.shortName) &&
        Objects.equals(this.phonePrefix, countryUpdate.phonePrefix) &&
        Objects.equals(this.sortOrder, countryUpdate.sortOrder) &&
        Objects.equals(this.isActive, countryUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(iso2, iso3, name, shortName, phonePrefix, sortOrder, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CountryUpdate {\n");
    sb.append("    iso2: ").append(toIndentedString(iso2)).append("\n");
    sb.append("    iso3: ").append(toIndentedString(iso3)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    shortName: ").append(toIndentedString(shortName)).append("\n");
    sb.append("    phonePrefix: ").append(toIndentedString(phonePrefix)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

