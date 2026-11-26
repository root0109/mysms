// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.GradeEntryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * A page of GradeEntryResult.
 */

public class GradeEntryResultList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid GradeEntryResult> items = new ArrayList<>();

  public GradeEntryResultList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradeEntryResultList(List<@Valid GradeEntryResult> items) {
    this.items = items;
  }

  public GradeEntryResultList items(List<@Valid GradeEntryResult> items) {
    this.items = items;
    return this;
  }

  public GradeEntryResultList addItemsItem(GradeEntryResult itemsItem) {
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
  public List<@Valid GradeEntryResult> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid GradeEntryResult> items) {
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
    GradeEntryResultList gradeEntryResultList = (GradeEntryResultList) o;
    return Objects.equals(this.items, gradeEntryResultList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradeEntryResultList {\n");
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

