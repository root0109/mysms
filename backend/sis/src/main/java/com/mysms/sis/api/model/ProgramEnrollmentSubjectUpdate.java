// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * Fields of ProgramEnrollmentSubject that can be updated (partial update; see updateMask).
 */

public class ProgramEnrollmentSubjectUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectId;

  public ProgramEnrollmentSubjectUpdate subjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  @Valid 
  @JsonProperty("subjectId")
  public @Nullable UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramEnrollmentSubjectUpdate programEnrollmentSubjectUpdate = (ProgramEnrollmentSubjectUpdate) o;
    return Objects.equals(this.subjectId, programEnrollmentSubjectUpdate.subjectId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramEnrollmentSubjectUpdate {\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
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

