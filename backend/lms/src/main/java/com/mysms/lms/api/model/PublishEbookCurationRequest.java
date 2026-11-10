// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * PublishEbookCurationRequest
 */

public class PublishEbookCurationRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> relatedMaterialIds = new ArrayList<>();

  public PublishEbookCurationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PublishEbookCurationRequest(List<UUID> relatedMaterialIds) {
    this.relatedMaterialIds = relatedMaterialIds;
  }

  public PublishEbookCurationRequest cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort (default course template).
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public PublishEbookCurationRequest relatedMaterialIds(List<UUID> relatedMaterialIds) {
    this.relatedMaterialIds = relatedMaterialIds;
    return this;
  }

  public PublishEbookCurationRequest addRelatedMaterialIdsItem(UUID relatedMaterialIdsItem) {
    if (this.relatedMaterialIds == null) {
      this.relatedMaterialIds = new ArrayList<>();
    }
    this.relatedMaterialIds.add(relatedMaterialIdsItem);
    return this;
  }

  /**
   * Materials kept, in order.
   * @return relatedMaterialIds
   */
  @NotNull 
  @JsonProperty("relatedMaterialIds")
  public List<UUID> getRelatedMaterialIds() {
    return relatedMaterialIds;
  }

  @JsonProperty("relatedMaterialIds")
  public void setRelatedMaterialIds(List<UUID> relatedMaterialIds) {
    this.relatedMaterialIds = relatedMaterialIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublishEbookCurationRequest publishEbookCurationRequest = (PublishEbookCurationRequest) o;
    return Objects.equals(this.cohortId, publishEbookCurationRequest.cohortId) &&
        Objects.equals(this.relatedMaterialIds, publishEbookCurationRequest.relatedMaterialIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, relatedMaterialIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublishEbookCurationRequest {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    relatedMaterialIds: ").append(toIndentedString(relatedMaterialIds)).append("\n");
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

