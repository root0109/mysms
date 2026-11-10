// Owned API contract of master (JAVA-CODING-RULES §7); first materialized from api/openapi/master.yaml on 2026-11-10.
package com.mysms.master.api.model;

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
 * Fields of City that can be updated (partial update; see updateMask).
 */

public class CityUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID stateProvinceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public CityUpdate countryId(@Nullable UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country of the city.
   * @return countryId
   */
  @Valid 
  @JsonProperty("countryId")
  public @Nullable UUID getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable UUID countryId) {
    this.countryId = countryId;
  }

  public CityUpdate stateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
    return this;
  }

  /**
   * State / province of the city.
   * @return stateProvinceId
   */
  @Valid 
  @JsonProperty("stateProvinceId")
  public @Nullable UUID getStateProvinceId() {
    return stateProvinceId;
  }

  @JsonProperty("stateProvinceId")
  public void setStateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
  }

  public CityUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * City name.
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

  public CityUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether selectable.
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
    CityUpdate cityUpdate = (CityUpdate) o;
    return Objects.equals(this.countryId, cityUpdate.countryId) &&
        Objects.equals(this.stateProvinceId, cityUpdate.stateProvinceId) &&
        Objects.equals(this.name, cityUpdate.name) &&
        Objects.equals(this.isActive, cityUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(countryId, stateProvinceId, name, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CityUpdate {\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    stateProvinceId: ").append(toIndentedString(stateProvinceId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

