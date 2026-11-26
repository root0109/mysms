// Owned API contract of careers.
package com.mysms.students.careers.api.model;

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
 * Fields of CandidatePreferredFunction that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CandidatePreferredFunctionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> functionAreaId;

  public CandidatePreferredFunctionUpdate functionAreaId(@Nullable Optional<UUID> functionAreaId) {
    this.functionAreaId = functionAreaId;
    return this;
  }

  /**
   * Function.
   * @return functionAreaId
   */
  
  @JsonProperty("functionAreaId")
  public @Nullable Optional<UUID> getFunctionAreaId() {
    return functionAreaId;
  }

  @JsonProperty("functionAreaId")
  public void setFunctionAreaId(@Nullable Optional<UUID> functionAreaId) {
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

