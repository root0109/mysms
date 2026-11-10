// Owned API contract of attendance (JAVA-CODING-RULES §7); first materialized from api/openapi/attendance.yaml on 2026-11-10.
package com.mysms.attendance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.attendance.api.model.AttendanceRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * AttendanceRecordList
 */

public class AttendanceRecordList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid AttendanceRecord> items = new ArrayList<>();

  public AttendanceRecordList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceRecordList(List<@Valid AttendanceRecord> items) {
    this.items = items;
  }

  public AttendanceRecordList items(List<@Valid AttendanceRecord> items) {
    this.items = items;
    return this;
  }

  public AttendanceRecordList addItemsItem(AttendanceRecord itemsItem) {
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
  public List<@Valid AttendanceRecord> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid AttendanceRecord> items) {
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
    AttendanceRecordList attendanceRecordList = (AttendanceRecordList) o;
    return Objects.equals(this.items, attendanceRecordList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceRecordList {\n");
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

