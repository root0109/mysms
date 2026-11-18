// Owned API contract of master.
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
 * Fields of StateProvince that can be updated (partial update; see updateMask).
 */

public class StateProvinceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public StateProvinceUpdate countryId(@Nullable UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country the state belongs to.
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

  public StateProvinceUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Official subdivision code (ISO 3166-2 suffix).
   * @return code
   */
  @Size(max = 10) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public StateProvinceUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * State / province name.
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

  public StateProvinceUpdate isActive(@Nullable Boolean isActive) {
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
    StateProvinceUpdate stateProvinceUpdate = (StateProvinceUpdate) o;
    return Objects.equals(this.countryId, stateProvinceUpdate.countryId) &&
        Objects.equals(this.code, stateProvinceUpdate.code) &&
        Objects.equals(this.name, stateProvinceUpdate.name) &&
        Objects.equals(this.isActive, stateProvinceUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(countryId, code, name, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StateProvinceUpdate {\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
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

