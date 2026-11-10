// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CreateInvoiceFromDuesRequest
 */

public class CreateInvoiceFromDuesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> dueIds = new ArrayList<>();

  public CreateInvoiceFromDuesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateInvoiceFromDuesRequest(UUID studentId, List<UUID> dueIds) {
    this.studentId = studentId;
    this.dueIds = dueIds;
  }

  public CreateInvoiceFromDuesRequest studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public CreateInvoiceFromDuesRequest dueIds(List<UUID> dueIds) {
    this.dueIds = dueIds;
    return this;
  }

  public CreateInvoiceFromDuesRequest addDueIdsItem(UUID dueIdsItem) {
    if (this.dueIds == null) {
      this.dueIds = new ArrayList<>();
    }
    this.dueIds.add(dueIdsItem);
    return this;
  }

  /**
   * Dues to bill.
   * @return dueIds
   */
  @NotNull 
  @JsonProperty("dueIds")
  public List<UUID> getDueIds() {
    return dueIds;
  }

  @JsonProperty("dueIds")
  public void setDueIds(List<UUID> dueIds) {
    this.dueIds = dueIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateInvoiceFromDuesRequest createInvoiceFromDuesRequest = (CreateInvoiceFromDuesRequest) o;
    return Objects.equals(this.studentId, createInvoiceFromDuesRequest.studentId) &&
        Objects.equals(this.dueIds, createInvoiceFromDuesRequest.dueIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, dueIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateInvoiceFromDuesRequest {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    dueIds: ").append(toIndentedString(dueIds)).append("\n");
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

