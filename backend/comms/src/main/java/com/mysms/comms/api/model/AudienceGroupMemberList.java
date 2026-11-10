// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.comms.api.model.AudienceGroupMember;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * AudienceGroupMemberList
 */

public class AudienceGroupMemberList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid AudienceGroupMember> items = new ArrayList<>();

  public AudienceGroupMemberList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AudienceGroupMemberList(List<@Valid AudienceGroupMember> items) {
    this.items = items;
  }

  public AudienceGroupMemberList items(List<@Valid AudienceGroupMember> items) {
    this.items = items;
    return this;
  }

  public AudienceGroupMemberList addItemsItem(AudienceGroupMember itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull 
  @JsonProperty("items")
  public List<@Valid AudienceGroupMember> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid AudienceGroupMember> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AudienceGroupMemberList audienceGroupMemberList = (AudienceGroupMemberList) o;
    return Objects.equals(this.items, audienceGroupMemberList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AudienceGroupMemberList {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

