// Owned API contract of facility.
package com.mysms.campus.facility.api.model;

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
 * Fields of RoomAsset that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RoomAssetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> assetItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> tagCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> quantity;

  public RoomAssetUpdate assetItemId(@Nullable Optional<UUID> assetItemId) {
    this.assetItemId = assetItemId;
    return this;
  }

  /**
   * Asset.
   * @return assetItemId
   */
  
  @JsonProperty("assetItemId")
  public @Nullable Optional<UUID> getAssetItemId() {
    return assetItemId;
  }

  @JsonProperty("assetItemId")
  public void setAssetItemId(@Nullable Optional<UUID> assetItemId) {
    this.assetItemId = assetItemId;
  }

  public RoomAssetUpdate tagCode(@Nullable Optional<String> tagCode) {
    this.tagCode = tagCode;
    return this;
  }

  /**
   * Asset tag / serial code.
   * @return tagCode
   */
  
  @JsonProperty("tagCode")
  public @Nullable Optional<String> getTagCode() {
    return tagCode;
  }

  @JsonProperty("tagCode")
  public void setTagCode(@Nullable Optional<String> tagCode) {
    this.tagCode = tagCode;
  }

  public RoomAssetUpdate quantity(@Nullable Optional<String> quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * Quantity.
   * @return quantity
   */
  
  @JsonProperty("quantity")
  public @Nullable Optional<String> getQuantity() {
    return quantity;
  }

  @JsonProperty("quantity")
  public void setQuantity(@Nullable Optional<String> quantity) {
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

