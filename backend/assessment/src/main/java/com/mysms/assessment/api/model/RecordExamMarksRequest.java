// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
package com.mysms.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.assessment.api.model.MarkEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RecordExamMarksRequest
 */

public class RecordExamMarksRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid MarkEntry> entries = new ArrayList<>();

  public RecordExamMarksRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RecordExamMarksRequest(List<@Valid MarkEntry> entries) {
    this.entries = entries;
  }

  public RecordExamMarksRequest entries(List<@Valid MarkEntry> entries) {
    this.entries = entries;
    return this;
  }

  public RecordExamMarksRequest addEntriesItem(MarkEntry entriesItem) {
    if (this.entries == null) {
      this.entries = new ArrayList<>();
    }
    this.entries.add(entriesItem);
    return this;
  }

  /**
   * Marks per student (upsert).
   * @return entries
   */
  @NotNull 
  @JsonProperty("entries")
  public List<@Valid MarkEntry> getEntries() {
    return entries;
  }

  @JsonProperty("entries")
  public void setEntries(List<@Valid MarkEntry> entries) {
    this.entries = entries;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordExamMarksRequest recordExamMarksRequest = (RecordExamMarksRequest) o;
    return Objects.equals(this.entries, recordExamMarksRequest.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordExamMarksRequest {\n");
    sb.append("    entries: ").append(toIndentedString(entries)).append("\n");
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

