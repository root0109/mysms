// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.EffectiveSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * The caller&#39;s effective settings.
 */

public class EffectiveSettingList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid EffectiveSetting> items = new ArrayList<>();

  public EffectiveSettingList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EffectiveSettingList(List<@Valid EffectiveSetting> items) {
    this.items = items;
  }

  public EffectiveSettingList items(List<@Valid EffectiveSetting> items) {
    this.items = items;
    return this;
  }

  public EffectiveSettingList addItemsItem(EffectiveSetting itemsItem) {
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
  public List<@Valid EffectiveSetting> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid EffectiveSetting> items) {
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
    EffectiveSettingList effectiveSettingList = (EffectiveSettingList) o;
    return Objects.equals(this.items, effectiveSettingList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EffectiveSettingList {\n");
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

