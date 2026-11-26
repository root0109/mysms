// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ApplyPromotionsRequest
 */

public class ApplyPromotionsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> promotionIds = new ArrayList<>();

  public ApplyPromotionsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApplyPromotionsRequest(List<UUID> promotionIds) {
    this.promotionIds = promotionIds;
  }

  public ApplyPromotionsRequest promotionIds(List<UUID> promotionIds) {
    this.promotionIds = promotionIds;
    return this;
  }

  public ApplyPromotionsRequest addPromotionIdsItem(UUID promotionIdsItem) {
    if (this.promotionIds == null) {
      this.promotionIds = new ArrayList<>();
    }
    this.promotionIds.add(promotionIdsItem);
    return this;
  }

  /**
   * Approved promotions.
   * @return promotionIds
   */
  @NotNull 
  @JsonProperty("promotionIds")
  public List<UUID> getPromotionIds() {
    return promotionIds;
  }

  @JsonProperty("promotionIds")
  public void setPromotionIds(List<UUID> promotionIds) {
    this.promotionIds = promotionIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplyPromotionsRequest applyPromotionsRequest = (ApplyPromotionsRequest) o;
    return Objects.equals(this.promotionIds, applyPromotionsRequest.promotionIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(promotionIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplyPromotionsRequest {\n");
    sb.append("    promotionIds: ").append(toIndentedString(promotionIds)).append("\n");
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

