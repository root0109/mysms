// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.MfaCredential;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * MfaCredentialList
 */

public class MfaCredentialList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid MfaCredential> items = new ArrayList<>();

  public MfaCredentialList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MfaCredentialList(List<@Valid MfaCredential> items) {
    this.items = items;
  }

  public MfaCredentialList items(List<@Valid MfaCredential> items) {
    this.items = items;
    return this;
  }

  public MfaCredentialList addItemsItem(MfaCredential itemsItem) {
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
  public List<@Valid MfaCredential> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid MfaCredential> items) {
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
    MfaCredentialList mfaCredentialList = (MfaCredentialList) o;
    return Objects.equals(this.items, mfaCredentialList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MfaCredentialList {\n");
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

