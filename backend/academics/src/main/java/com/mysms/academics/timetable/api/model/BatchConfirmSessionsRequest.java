// Owned API contract of timetable.
package com.mysms.academics.timetable.api.model;

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
 * BatchConfirmSessionsRequest
 */

public class BatchConfirmSessionsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> sessionIds = new ArrayList<>();

  public BatchConfirmSessionsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchConfirmSessionsRequest(List<UUID> sessionIds) {
    this.sessionIds = sessionIds;
  }

  public BatchConfirmSessionsRequest sessionIds(List<UUID> sessionIds) {
    this.sessionIds = sessionIds;
    return this;
  }

  public BatchConfirmSessionsRequest addSessionIdsItem(UUID sessionIdsItem) {
    if (this.sessionIds == null) {
      this.sessionIds = new ArrayList<>();
    }
    this.sessionIds.add(sessionIdsItem);
    return this;
  }

  /**
   * Sessions to confirm.
   * @return sessionIds
   */
  @NotNull 
  @JsonProperty("sessionIds")
  public List<UUID> getSessionIds() {
    return sessionIds;
  }

  @JsonProperty("sessionIds")
  public void setSessionIds(List<UUID> sessionIds) {
    this.sessionIds = sessionIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchConfirmSessionsRequest batchConfirmSessionsRequest = (BatchConfirmSessionsRequest) o;
    return Objects.equals(this.sessionIds, batchConfirmSessionsRequest.sessionIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchConfirmSessionsRequest {\n");
    sb.append("    sessionIds: ").append(toIndentedString(sessionIds)).append("\n");
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

