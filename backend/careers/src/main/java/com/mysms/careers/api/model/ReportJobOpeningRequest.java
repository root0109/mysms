// Owned API contract of careers (JAVA-CODING-RULES §7); first materialized from api/openapi/careers.yaml on 2026-11-10.
package com.mysms.careers.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ReportJobOpeningRequest
 */

public class ReportJobOpeningRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String reason;

  public ReportJobOpeningRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReportJobOpeningRequest(String reason) {
    this.reason = reason;
  }

  public ReportJobOpeningRequest reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Why it is inappropriate.
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReportJobOpeningRequest reportJobOpeningRequest = (ReportJobOpeningRequest) o;
    return Objects.equals(this.reason, reportJobOpeningRequest.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReportJobOpeningRequest {\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

