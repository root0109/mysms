// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.UserRole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * UserRoleList
 */

public class UserRoleList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid UserRole> items = new ArrayList<>();

  public UserRoleList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserRoleList(List<@Valid UserRole> items) {
    this.items = items;
  }

  public UserRoleList items(List<@Valid UserRole> items) {
    this.items = items;
    return this;
  }

  public UserRoleList addItemsItem(UserRole itemsItem) {
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
  public List<@Valid UserRole> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid UserRole> items) {
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
    UserRoleList userRoleList = (UserRoleList) o;
    return Objects.equals(this.items, userRoleList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserRoleList {\n");
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

