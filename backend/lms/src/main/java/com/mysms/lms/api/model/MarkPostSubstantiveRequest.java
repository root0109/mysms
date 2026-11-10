// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * MarkPostSubstantiveRequest
 */

public class MarkPostSubstantiveRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean substantive;

  public MarkPostSubstantiveRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarkPostSubstantiveRequest(Boolean substantive) {
    this.substantive = substantive;
  }

  public MarkPostSubstantiveRequest substantive(Boolean substantive) {
    this.substantive = substantive;
    return this;
  }

  /**
   * Substantive or not.
   * @return substantive
   */
  @NotNull 
  @JsonProperty("substantive")
  public Boolean getSubstantive() {
    return substantive;
  }

  @JsonProperty("substantive")
  public void setSubstantive(Boolean substantive) {
    this.substantive = substantive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarkPostSubstantiveRequest markPostSubstantiveRequest = (MarkPostSubstantiveRequest) o;
    return Objects.equals(this.substantive, markPostSubstantiveRequest.substantive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(substantive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarkPostSubstantiveRequest {\n");
    sb.append("    substantive: ").append(toIndentedString(substantive)).append("\n");
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

