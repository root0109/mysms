// Owned API contract of org.
package com.mysms.platform.org.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.platform.org.api.model.AcademicTerm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * AcademicTermList
 */

public class AcademicTermList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid AcademicTerm> items = new ArrayList<>();

  public AcademicTermList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AcademicTermList(List<@Valid AcademicTerm> items) {
    this.items = items;
  }

  public AcademicTermList items(List<@Valid AcademicTerm> items) {
    this.items = items;
    return this;
  }

  public AcademicTermList addItemsItem(AcademicTerm itemsItem) {
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
  public List<@Valid AcademicTerm> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid AcademicTerm> items) {
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
    AcademicTermList academicTermList = (AcademicTermList) o;
    return Objects.equals(this.items, academicTermList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AcademicTermList {\n");
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

