// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * CopyReleaseRuleRequest
 */

public class CopyReleaseRuleRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> sourceContentItemIds = new ArrayList<>();

  public CopyReleaseRuleRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CopyReleaseRuleRequest(List<UUID> sourceContentItemIds) {
    this.sourceContentItemIds = sourceContentItemIds;
  }

  public CopyReleaseRuleRequest sourceContentItemIds(List<UUID> sourceContentItemIds) {
    this.sourceContentItemIds = sourceContentItemIds;
    return this;
  }

  public CopyReleaseRuleRequest addSourceContentItemIdsItem(UUID sourceContentItemIdsItem) {
    if (this.sourceContentItemIds == null) {
      this.sourceContentItemIds = new ArrayList<>();
    }
    this.sourceContentItemIds.add(sourceContentItemIdsItem);
    return this;
  }

  /**
   * Other source items to copy the rule to.
   * @return sourceContentItemIds
   */
  @NotNull 
  @JsonProperty("sourceContentItemIds")
  public List<UUID> getSourceContentItemIds() {
    return sourceContentItemIds;
  }

  @JsonProperty("sourceContentItemIds")
  public void setSourceContentItemIds(List<UUID> sourceContentItemIds) {
    this.sourceContentItemIds = sourceContentItemIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CopyReleaseRuleRequest copyReleaseRuleRequest = (CopyReleaseRuleRequest) o;
    return Objects.equals(this.sourceContentItemIds, copyReleaseRuleRequest.sourceContentItemIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceContentItemIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CopyReleaseRuleRequest {\n");
    sb.append("    sourceContentItemIds: ").append(toIndentedString(sourceContentItemIds)).append("\n");
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

