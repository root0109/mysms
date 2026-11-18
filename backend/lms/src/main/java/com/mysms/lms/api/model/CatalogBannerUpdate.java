// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of CatalogBanner that can be updated (partial update; see updateMask).
 */

public class CatalogBannerUpdate {

  /**
   * Where the banner appears.
   */
  public enum PlacementEnum {
    FEATURED("featured"),
    
    SHOWCASE("showcase");

    private final String value;

    PlacementEnum(String value) {
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
    public static PlacementEnum fromValue(String value) {
      for (PlacementEnum b : PlacementEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable PlacementEnum placement;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public CatalogBannerUpdate placement(@Nullable PlacementEnum placement) {
    this.placement = placement;
    return this;
  }

  /**
   * Where the banner appears.
   * @return placement
   */
  
  @JsonProperty("placement")
  public @Nullable PlacementEnum getPlacement() {
    return placement;
  }

  @JsonProperty("placement")
  public void setPlacement(@Nullable PlacementEnum placement) {
    this.placement = placement;
  }

  public CatalogBannerUpdate imageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Banner image.
   * @return imageFileId
   */
  @Valid 
  @JsonProperty("imageFileId")
  public @Nullable UUID getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
  }

  public CatalogBannerUpdate sortOrder(@Nullable Integer sortOrder) {
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
    CatalogBannerUpdate catalogBannerUpdate = (CatalogBannerUpdate) o;
    return Objects.equals(this.placement, catalogBannerUpdate.placement) &&
        Objects.equals(this.imageFileId, catalogBannerUpdate.imageFileId) &&
        Objects.equals(this.sortOrder, catalogBannerUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(placement, imageFileId, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CatalogBannerUpdate {\n");
    sb.append("    placement: ").append(toIndentedString(placement)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
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

