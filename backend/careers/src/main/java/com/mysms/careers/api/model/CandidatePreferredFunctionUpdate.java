// Owned API contract of careers (JAVA-CODING-RULES §7); first materialized from api/openapi/careers.yaml on 2026-11-10.
package com.mysms.careers.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CandidatePreferredFunction that can be updated (partial update; see updateMask).
 */

public class CandidatePreferredFunctionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID functionAreaId;

  public CandidatePreferredFunctionUpdate functionAreaId(@Nullable UUID functionAreaId) {
    this.functionAreaId = functionAreaId;
    return this;
  }

  /**
   * Function.
   * @return functionAreaId
   */
  @Valid 
  @JsonProperty("functionAreaId")
  public @Nullable UUID getFunctionAreaId() {
    return functionAreaId;
  }

  @JsonProperty("functionAreaId")
  public void setFunctionAreaId(@Nullable UUID functionAreaId) {
    this.functionAreaId = functionAreaId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CandidatePreferredFunctionUpdate candidatePreferredFunctionUpdate = (CandidatePreferredFunctionUpdate) o;
    return Objects.equals(this.functionAreaId, candidatePreferredFunctionUpdate.functionAreaId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionAreaId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CandidatePreferredFunctionUpdate {\n");
    sb.append("    functionAreaId: ").append(toIndentedString(functionAreaId)).append("\n");
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

