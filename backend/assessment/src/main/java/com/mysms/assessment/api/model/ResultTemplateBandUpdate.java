// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
package com.mysms.assessment.api.model;

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
 * Fields of ResultTemplateBand that can be updated (partial update; see updateMask).
 */

public class ResultTemplateBandUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradingBandId;

  public ResultTemplateBandUpdate gradingBandId(@Nullable UUID gradingBandId) {
    this.gradingBandId = gradingBandId;
    return this;
  }

  /**
   * Band.
   * @return gradingBandId
   */
  @Valid 
  @JsonProperty("gradingBandId")
  public @Nullable UUID getGradingBandId() {
    return gradingBandId;
  }

  @JsonProperty("gradingBandId")
  public void setGradingBandId(@Nullable UUID gradingBandId) {
    this.gradingBandId = gradingBandId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultTemplateBandUpdate resultTemplateBandUpdate = (ResultTemplateBandUpdate) o;
    return Objects.equals(this.gradingBandId, resultTemplateBandUpdate.gradingBandId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gradingBandId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultTemplateBandUpdate {\n");
    sb.append("    gradingBandId: ").append(toIndentedString(gradingBandId)).append("\n");
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

