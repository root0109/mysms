// Owned API contract of lms.
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
 * ReorderReleaseRulesRequest
 */

public class ReorderReleaseRulesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> releaseRuleIds = new ArrayList<>();

  public ReorderReleaseRulesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReorderReleaseRulesRequest(List<UUID> releaseRuleIds) {
    this.releaseRuleIds = releaseRuleIds;
  }

  public ReorderReleaseRulesRequest releaseRuleIds(List<UUID> releaseRuleIds) {
    this.releaseRuleIds = releaseRuleIds;
    return this;
  }

  public ReorderReleaseRulesRequest addReleaseRuleIdsItem(UUID releaseRuleIdsItem) {
    if (this.releaseRuleIds == null) {
      this.releaseRuleIds = new ArrayList<>();
    }
    this.releaseRuleIds.add(releaseRuleIdsItem);
    return this;
  }

  /**
   * Rules in evaluation order.
   * @return releaseRuleIds
   */
  @NotNull 
  @JsonProperty("releaseRuleIds")
  public List<UUID> getReleaseRuleIds() {
    return releaseRuleIds;
  }

  @JsonProperty("releaseRuleIds")
  public void setReleaseRuleIds(List<UUID> releaseRuleIds) {
    this.releaseRuleIds = releaseRuleIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReorderReleaseRulesRequest reorderReleaseRulesRequest = (ReorderReleaseRulesRequest) o;
    return Objects.equals(this.releaseRuleIds, reorderReleaseRulesRequest.releaseRuleIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(releaseRuleIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReorderReleaseRulesRequest {\n");
    sb.append("    releaseRuleIds: ").append(toIndentedString(releaseRuleIds)).append("\n");
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

