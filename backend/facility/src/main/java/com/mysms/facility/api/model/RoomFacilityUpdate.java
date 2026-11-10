// Owned API contract of facility (JAVA-CODING-RULES §7); first materialized from api/openapi/facility.yaml on 2026-11-10.
package com.mysms.facility.api.model;

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
 * Fields of RoomFacility that can be updated (partial update; see updateMask).
 */

public class RoomFacilityUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID facilityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String quantity;

  public RoomFacilityUpdate facilityId(@Nullable UUID facilityId) {
    this.facilityId = facilityId;
    return this;
  }

  /**
   * Facility.
   * @return facilityId
   */
  @Valid 
  @JsonProperty("facilityId")
  public @Nullable UUID getFacilityId() {
    return facilityId;
  }

  @JsonProperty("facilityId")
  public void setFacilityId(@Nullable UUID facilityId) {
    this.facilityId = facilityId;
  }

  public RoomFacilityUpdate quantity(@Nullable String quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Quantity.
   * @return quantity
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("quantity")
  public @Nullable String getQuantity() {
    return quantity;
  }

  @JsonProperty("quantity")
  public void setQuantity(@Nullable String quantity) {
    this.quantity = quantity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomFacilityUpdate roomFacilityUpdate = (RoomFacilityUpdate) o;
    return Objects.equals(this.facilityId, roomFacilityUpdate.facilityId) &&
        Objects.equals(this.quantity, roomFacilityUpdate.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(facilityId, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomFacilityUpdate {\n");
    sb.append("    facilityId: ").append(toIndentedString(facilityId)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
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

