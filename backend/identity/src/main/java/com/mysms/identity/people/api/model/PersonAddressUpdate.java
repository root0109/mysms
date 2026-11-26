// Owned API contract of people.
package com.mysms.identity.people.api.model;

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
 * Fields of PersonAddress that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
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
  private @Nullable Optional<@NotNull AddressTypeEnum> addressType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> line1;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> line2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> cityName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> stateProvinceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 12) String> postalCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPrimary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> stateName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public PersonAddressUpdate addressType(@Nullable Optional<AddressTypeEnum> addressType) {
    this.addressType = addressType;
    return this;
  }

  /**
   * Purpose of the address.
   * @return addressType
   */
  
  @JsonProperty("addressType")
  public @Nullable Optional<AddressTypeEnum> getAddressType() {
    return addressType;
  }

  @JsonProperty("addressType")
  public void setAddressType(@Nullable Optional<AddressTypeEnum> addressType) {
    this.addressType = addressType;
  }

  public PersonAddressUpdate line1(@Nullable Optional<String> line1) {
    this.line1 = line1;
    return this;
  }

  /**
   * Address line 1.
   * @return line1
   */
  
  @JsonProperty("line1")
  public @Nullable Optional<String> getLine1() {
    return line1;
  }

  @JsonProperty("line1")
  public void setLine1(@Nullable Optional<String> line1) {
    this.line1 = line1;
  }

  public PersonAddressUpdate line2(@Nullable Optional<String> line2) {
    this.line2 = line2;
    return this;
  }

  /**
   * Address line 2.
   * @return line2
   */
  
  @JsonProperty("line2")
  public @Nullable Optional<String> getLine2() {
    return line2;
  }

  @JsonProperty("line2")
  public void setLine2(@Nullable Optional<String> line2) {
    this.line2 = line2;
  }

  public PersonAddressUpdate cityId(@Nullable Optional<UUID> cityId) {
    this.cityId = cityId;
    return this;
  }

  /**
   * City (from master).
   * @return cityId
   */
  
  @JsonProperty("cityId")
  public @Nullable Optional<UUID> getCityId() {
    return cityId;
  }

  @JsonProperty("cityId")
  public void setCityId(@Nullable Optional<UUID> cityId) {
    this.cityId = cityId;
  }

  public PersonAddressUpdate cityName(@Nullable Optional<String> cityName) {
    this.cityName = cityName;
    return this;
  }

  /**
   * City as free text when not in the master.
   * @return cityName
   */
  
  @JsonProperty("cityName")
  public @Nullable Optional<String> getCityName() {
    return cityName;
  }

  @JsonProperty("cityName")
  public void setCityName(@Nullable Optional<String> cityName) {
    this.cityName = cityName;
  }

  public PersonAddressUpdate stateProvinceId(@Nullable Optional<UUID> stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
    return this;
  }

  /**
   * State / province.
   * @return stateProvinceId
   */
  
  @JsonProperty("stateProvinceId")
  public @Nullable Optional<UUID> getStateProvinceId() {
    return stateProvinceId;
  }

  @JsonProperty("stateProvinceId")
  public void setStateProvinceId(@Nullable Optional<UUID> stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
  }

  public PersonAddressUpdate countryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  
  @JsonProperty("countryId")
  public @Nullable Optional<UUID> getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
  }

  public PersonAddressUpdate postalCode(@Nullable Optional<String> postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * Postal / PIN code.
   * @return postalCode
   */
  
  @JsonProperty("postalCode")
  public @Nullable Optional<String> getPostalCode() {
    return postalCode;
  }

  @JsonProperty("postalCode")
  public void setPostalCode(@Nullable Optional<String> postalCode) {
    this.postalCode = postalCode;
  }

  public PersonAddressUpdate isPrimary(@Nullable Optional<Boolean> isPrimary) {
    this.isPrimary = isPrimary;
    return this;
  }

  /**
   * Primary address of its type.
   * @return isPrimary
   */
  
  @JsonProperty("isPrimary")
  public @Nullable Optional<Boolean> getIsPrimary() {
    return isPrimary;
  }

  @JsonProperty("isPrimary")
  public void setIsPrimary(@Nullable Optional<Boolean> isPrimary) {
    this.isPrimary = isPrimary;
  }

  public PersonAddressUpdate stateName(@Nullable Optional<String> stateName) {
    this.stateName = stateName;
    return this;
  }

  /**
   * State / province as free text when not picked from the list.
   * @return stateName
   */
  
  @JsonProperty("stateName")
  public @Nullable Optional<String> getStateName() {
    return stateName;
  }

  @JsonProperty("stateName")
  public void setStateName(@Nullable Optional<String> stateName) {
    this.stateName = stateName;
  }

  public PersonAddressUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
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

