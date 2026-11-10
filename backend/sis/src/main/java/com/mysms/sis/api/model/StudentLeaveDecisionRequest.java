// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * StudentLeaveDecisionRequest
 */

public class StudentLeaveDecisionRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String decisionNote;

  public StudentLeaveDecisionRequest decisionNote(@Nullable String decisionNote) {
    this.decisionNote = decisionNote;
    return this;
  }

  /**
   * Note recorded with the decision.
   * @return decisionNote
   */
  @Size(max = 300) 
  @JsonProperty("decisionNote")
  public @Nullable String getDecisionNote() {
    return decisionNote;
  }

  @JsonProperty("decisionNote")
  public void setDecisionNote(@Nullable String decisionNote) {
    this.decisionNote = decisionNote;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentLeaveDecisionRequest studentLeaveDecisionRequest = (StudentLeaveDecisionRequest) o;
    return Objects.equals(this.decisionNote, studentLeaveDecisionRequest.decisionNote);
  }

  @Override
  public int hashCode() {
    return Objects.hash(decisionNote);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentLeaveDecisionRequest {\n");
    sb.append("    decisionNote: ").append(toIndentedString(decisionNote)).append("\n");
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

