// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.BatchEnrollPreview;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * A page of BatchEnrollPreview.
 */

public class BatchEnrollPreviewList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid BatchEnrollPreview> items = new ArrayList<>();

  public BatchEnrollPreviewList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchEnrollPreviewList(List<@Valid BatchEnrollPreview> items) {
    this.items = items;
  }

  public BatchEnrollPreviewList items(List<@Valid BatchEnrollPreview> items) {
    this.items = items;
    return this;
  }

  public BatchEnrollPreviewList addItemsItem(BatchEnrollPreview itemsItem) {
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
  public List<@Valid BatchEnrollPreview> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid BatchEnrollPreview> items) {
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
    BatchEnrollPreviewList batchEnrollPreviewList = (BatchEnrollPreviewList) o;
    return Objects.equals(this.items, batchEnrollPreviewList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchEnrollPreviewList {\n");
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

