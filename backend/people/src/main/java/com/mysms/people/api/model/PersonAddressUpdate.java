// Owned API contract of people (JAVA-CODING-RULES §7); first materialized from api/openapi/people.yaml on 2026-11-10.
package com.mysms.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PersonAddress that can be updated (partial update; see updateMask).
 */

public class PersonAddressUpdate {

  /**
   * Purpose of the address.
   */
  public enum AddressTypeEnum {
    PERMANENT("permanent"),
    
    COMMUNICATION("communication"),
    
    OFFICIAL("official"),
    
    BILLING("billing"),
    
    OTHER("other");

    private final String value;

    AddressTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static AddressTypeEnum fromValue(String value) {
      for (AddressTypeEnum b : AddressTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AddressTypeEnum addressType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String line1;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String line2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String cityName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID stateProvinceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String postalCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPrimary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String stateName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public PersonAddressUpdate addressType(@Nullable AddressTypeEnum addressType) {
    this.addressType = addressType;
    return this;
  }

  /**
   * Purpose of the address.
   * @return addressType
   */
  
  @JsonProperty("addressType")
  public @Nullable AddressTypeEnum getAddressType() {
    return addressType;
  }

  @JsonProperty("addressType")
  public void setAddressType(@Nullable AddressTypeEnum addressType) {
    this.addressType = addressType;
  }

  public PersonAddressUpdate line1(@Nullable String line1) {
    this.line1 = line1;
    return this;
  }

  /**
   * Address line 1.
   * @return line1
   */
  @Size(max = 255) 
  @JsonProperty("line1")
  public @Nullable String getLine1() {
    return line1;
  }

  @JsonProperty("line1")
  public void setLine1(@Nullable String line1) {
    this.line1 = line1;
  }

  public PersonAddressUpdate line2(@Nullable String line2) {
    this.line2 = line2;
    return this;
  }

  /**
   * Address line 2.
   * @return line2
   */
  @Size(max = 255) 
  @JsonProperty("line2")
  public @Nullable String getLine2() {
    return line2;
  }

  @JsonProperty("line2")
  public void setLine2(@Nullable String line2) {
    this.line2 = line2;
  }

  public PersonAddressUpdate cityId(@Nullable UUID cityId) {
    this.cityId = cityId;
    return this;
  }

  /**
   * City (from master).
   * @return cityId
   */
  @Valid 
  @JsonProperty("cityId")
  public @Nullable UUID getCityId() {
    return cityId;
  }

  @JsonProperty("cityId")
  public void setCityId(@Nullable UUID cityId) {
    this.cityId = cityId;
  }

  public PersonAddressUpdate cityName(@Nullable String cityName) {
    this.cityName = cityName;
    return this;
  }

  /**
   * City as free text when not in the master.
   * @return cityName
   */
  @Size(max = 120) 
  @JsonProperty("cityName")
  public @Nullable String getCityName() {
    return cityName;
  }

  @JsonProperty("cityName")
  public void setCityName(@Nullable String cityName) {
    this.cityName = cityName;
  }

  public PersonAddressUpdate stateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
    return this;
  }

  /**
   * State / province.
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

  public PersonAddressUpdate countryId(@Nullable UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
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

  public PersonAddressUpdate postalCode(@Nullable String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * Postal / PIN code.
   * @return postalCode
   */
  @Size(max = 12) 
  @JsonProperty("postalCode")
  public @Nullable String getPostalCode() {
    return postalCode;
  }

  @JsonProperty("postalCode")
  public void setPostalCode(@Nullable String postalCode) {
    this.postalCode = postalCode;
  }

  public PersonAddressUpdate isPrimary(@Nullable Boolean isPrimary) {
    this.isPrimary = isPrimary;
    return this;
  }

  /**
   * Primary address of its type.
   * @return isPrimary
   */
  
  @JsonProperty("isPrimary")
  public @Nullable Boolean getIsPrimary() {
    return isPrimary;
  }

  @JsonProperty("isPrimary")
  public void setIsPrimary(@Nullable Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  public PersonAddressUpdate stateName(@Nullable String stateName) {
    this.stateName = stateName;
    return this;
  }

  /**
   * State / province as free text when not picked from the list.
   * @return stateName
   */
  @Size(max = 100) 
  @JsonProperty("stateName")
  public @Nullable String getStateName() {
    return stateName;
  }

  @JsonProperty("stateName")
  public void setStateName(@Nullable String stateName) {
    this.stateName = stateName;
  }

  public PersonAddressUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonAddressUpdate personAddressUpdate = (PersonAddressUpdate) o;
    return Objects.equals(this.addressType, personAddressUpdate.addressType) &&
        Objects.equals(this.line1, personAddressUpdate.line1) &&
        Objects.equals(this.line2, personAddressUpdate.line2) &&
        Objects.equals(this.cityId, personAddressUpdate.cityId) &&
        Objects.equals(this.cityName, personAddressUpdate.cityName) &&
        Objects.equals(this.stateProvinceId, personAddressUpdate.stateProvinceId) &&
        Objects.equals(this.countryId, personAddressUpdate.countryId) &&
        Objects.equals(this.postalCode, personAddressUpdate.postalCode) &&
        Objects.equals(this.isPrimary, personAddressUpdate.isPrimary) &&
        Objects.equals(this.stateName, personAddressUpdate.stateName) &&
        Objects.equals(this.sortOrder, personAddressUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addressType, line1, line2, cityId, cityName, stateProvinceId, countryId, postalCode, isPrimary, stateName, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PersonAddressUpdate {\n");
    sb.append("    addressType: ").append(toIndentedString(addressType)).append("\n");
    sb.append("    line1: ").append(toIndentedString(line1)).append("\n");
    sb.append("    line2: ").append(toIndentedString(line2)).append("\n");
    sb.append("    cityId: ").append(toIndentedString(cityId)).append("\n");
    sb.append("    cityName: ").append(toIndentedString(cityName)).append("\n");
    sb.append("    stateProvinceId: ").append(toIndentedString(stateProvinceId)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    isPrimary: ").append(toIndentedString(isPrimary)).append("\n");
    sb.append("    stateName: ").append(toIndentedString(stateName)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

