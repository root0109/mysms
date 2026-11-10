// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.GradeEntryInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * BatchGradeEntriesRequest
 */

public class BatchGradeEntriesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid GradeEntryInput> entries = new ArrayList<>();

  public BatchGradeEntriesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchGradeEntriesRequest(List<@Valid GradeEntryInput> entries) {
    this.entries = entries;
  }

  public BatchGradeEntriesRequest entries(List<@Valid GradeEntryInput> entries) {
    this.entries = entries;
    return this;
  }

  public BatchGradeEntriesRequest addEntriesItem(GradeEntryInput entriesItem) {
    if (this.entries == null) {
      this.entries = new ArrayList<>();
    }
    this.entries.add(entriesItem);
    return this;
  }

  /**
   * Up to 200 grades (e.g. a whole column graded on a tablet).
   * @return entries
   */
  @NotNull 
  @JsonProperty("entries")
  public List<@Valid GradeEntryInput> getEntries() {
    return entries;
  }

  @JsonProperty("entries")
  public void setEntries(List<@Valid GradeEntryInput> entries) {
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
    BatchGradeEntriesRequest batchGradeEntriesRequest = (BatchGradeEntriesRequest) o;
    return Objects.equals(this.entries, batchGradeEntriesRequest.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchGradeEntriesRequest {\n");
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

