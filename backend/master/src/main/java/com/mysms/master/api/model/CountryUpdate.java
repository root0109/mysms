// Owned API contract of master.
package com.mysms.master.api.model;

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
 * Fields of Country that can be updated (partial update; see updateMask).
 */

public class CountryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String iso2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String iso3;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String shortName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String phonePrefix;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public CountryUpdate iso2(@Nullable String iso2) {
    this.iso2 = iso2;
    return this;
  }

  /**
   * ISO 3166-1 alpha-2 code.
   * @return iso2
   */
  @Size(max = 2) 
  @JsonProperty("iso2")
  public @Nullable String getIso2() {
    return iso2;
  }

  @JsonProperty("iso2")
  public void setIso2(@Nullable String iso2) {
    this.iso2 = iso2;
  }

  public CountryUpdate iso3(@Nullable String iso3) {
    this.iso3 = iso3;
    return this;
  }

  /**
   * ISO 3166-1 alpha-3 code.
   * @return iso3
   */
  @Size(max = 3) 
  @JsonProperty("iso3")
  public @Nullable String getIso3() {
    return iso3;
  }

  @JsonProperty("iso3")
  public void setIso3(@Nullable String iso3) {
    this.iso3 = iso3;
  }

  public CountryUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Country name.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public CountryUpdate shortName(@Nullable String shortName) {
    this.shortName = shortName;
    return this;
  }

  /**
   * Short/common name.
   * @return shortName
   */
  @Size(max = 60) 
  @JsonProperty("shortName")
  public @Nullable String getShortName() {
    return shortName;
  }

  @JsonProperty("shortName")
  public void setShortName(@Nullable String shortName) {
    this.shortName = shortName;
  }

  public CountryUpdate phonePrefix(@Nullable String phonePrefix) {
    this.phonePrefix = phonePrefix;
    return this;
  }

  /**
   * International dialling prefix, e.g. +91.
   * @return phonePrefix
   */
  @Size(max = 8) 
  @JsonProperty("phonePrefix")
  public @Nullable String getPhonePrefix() {
    return phonePrefix;
  }

  @JsonProperty("phonePrefix")
  public void setPhonePrefix(@Nullable String phonePrefix) {
    this.phonePrefix = phonePrefix;
  }

  public CountryUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order in pick-lists.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public CountryUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether the country is selectable.
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

