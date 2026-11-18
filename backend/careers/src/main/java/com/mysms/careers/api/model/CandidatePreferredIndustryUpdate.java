// Owned API contract of careers.
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
 * Fields of CandidatePreferredIndustry that can be updated (partial update; see updateMask).
 */

public class CandidatePreferredIndustryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID industryId;

  public CandidatePreferredIndustryUpdate industryId(@Nullable UUID industryId) {
    this.industryId = industryId;
    return this;
  }

  /**
   * Industry.
   * @return industryId
   */
  @Valid 
  @JsonProperty("industryId")
  public @Nullable UUID getIndustryId() {
    return industryId;
  }

  @JsonProperty("industryId")
  public void setIndustryId(@Nullable UUID industryId) {
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

