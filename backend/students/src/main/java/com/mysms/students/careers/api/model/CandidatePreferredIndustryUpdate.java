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
 * Fields of CandidatePreferredIndustry that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CandidatePreferredIndustryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> industryId;

  public CandidatePreferredIndustryUpdate industryId(@Nullable Optional<UUID> industryId) {
    this.industryId = industryId;
    return this;
  }

  /**
   * Industry.
   * @return industryId
   */
  
  @JsonProperty("industryId")
  public @Nullable Optional<UUID> getIndustryId() {
    return industryId;
  }

  @JsonProperty("industryId")
  public void setIndustryId(@Nullable Optional<UUID> industryId) {
    this.industryId = industryId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CandidatePreferredIndustryUpdate candidatePreferredIndustryUpdate = (CandidatePreferredIndustryUpdate) o;
    return Objects.equals(this.industryId, candidatePreferredIndustryUpdate.industryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(industryId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CandidatePreferredIndustryUpdate {\n");
    sb.append("    industryId: ").append(toIndentedString(industryId)).append("\n");
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

