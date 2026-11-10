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
 * Fields of RoomAsset that can be updated (partial update; see updateMask).
 */

public class RoomAssetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assetItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String tagCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String quantity;

  public RoomAssetUpdate assetItemId(@Nullable UUID assetItemId) {
    this.assetItemId = assetItemId;
    return this;
  }

  /**
   * Asset.
   * @return assetItemId
   */
  @Valid 
  @JsonProperty("assetItemId")
  public @Nullable UUID getAssetItemId() {
    return assetItemId;
  }

  @JsonProperty("assetItemId")
  public void setAssetItemId(@Nullable UUID assetItemId) {
    this.assetItemId = assetItemId;
  }

  public RoomAssetUpdate tagCode(@Nullable String tagCode) {
    this.tagCode = tagCode;
    return this;
  }

  /**
   * Asset tag / serial code.
   * @return tagCode
   */
  @Size(max = 40) 
  @JsonProperty("tagCode")
  public @Nullable String getTagCode() {
    return tagCode;
  }

  @JsonProperty("tagCode")
  public void setTagCode(@Nullable String tagCode) {
    this.tagCode = tagCode;
  }

  public RoomAssetUpdate quantity(@Nullable String quantity) {
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
    RoomAssetUpdate roomAssetUpdate = (RoomAssetUpdate) o;
    return Objects.equals(this.assetItemId, roomAssetUpdate.assetItemId) &&
        Objects.equals(this.tagCode, roomAssetUpdate.tagCode) &&
        Objects.equals(this.quantity, roomAssetUpdate.quantity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assetItemId, tagCode, quantity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomAssetUpdate {\n");
    sb.append("    assetItemId: ").append(toIndentedString(assetItemId)).append("\n");
    sb.append("    tagCode: ").append(toIndentedString(tagCode)).append("\n");
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

