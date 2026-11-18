// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SnoozeTaskRequest
 */

public class SnoozeTaskRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime until;

  public SnoozeTaskRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SnoozeTaskRequest(OffsetDateTime until) {
    this.until = until;
  }

  public SnoozeTaskRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  public SnoozeTaskRequest until(OffsetDateTime until) {
    this.until = until;
    return this;
  }

  /**
   * New reminder time.
   * @return until
   */
  @NotNull @Valid 
  @JsonProperty("until")
  public OffsetDateTime getUntil() {
    return until;
  }

  @JsonProperty("until")
  public void setUntil(OffsetDateTime until) {
    this.until = until;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SnoozeTaskRequest snoozeTaskRequest = (SnoozeTaskRequest) o;
    return Objects.equals(this.etag, snoozeTaskRequest.etag) &&
        Objects.equals(this.until, snoozeTaskRequest.until);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, until);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SnoozeTaskRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    until: ").append(toIndentedString(until)).append("\n");
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

