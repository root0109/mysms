// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

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
 * Fields of ExamInvigilator that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ExamInvigilatorUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> staffId;

  public ExamInvigilatorUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Invigilator / examiner.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExamInvigilatorUpdate examInvigilatorUpdate = (ExamInvigilatorUpdate) o;
    return Objects.equals(this.staffId, examInvigilatorUpdate.staffId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(staffId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExamInvigilatorUpdate {\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
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

