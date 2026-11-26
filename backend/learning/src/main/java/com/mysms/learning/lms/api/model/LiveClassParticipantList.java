// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.LiveClassParticipant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * LiveClassParticipantList
 */

public class LiveClassParticipantList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid LiveClassParticipant> items = new ArrayList<>();

  public LiveClassParticipantList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LiveClassParticipantList(List<@Valid LiveClassParticipant> items) {
    this.items = items;
  }

  public LiveClassParticipantList items(List<@Valid LiveClassParticipant> items) {
    this.items = items;
    return this;
  }

  public LiveClassParticipantList addItemsItem(LiveClassParticipant itemsItem) {
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
  public List<@Valid LiveClassParticipant> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid LiveClassParticipant> items) {
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
    LiveClassParticipantList liveClassParticipantList = (LiveClassParticipantList) o;
    return Objects.equals(this.items, liveClassParticipantList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LiveClassParticipantList {\n");
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

