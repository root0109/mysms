// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * ApplyTimetableTemplateRequest
 */

public class ApplyTimetableTemplateRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID timetableTemplateId;

  public ApplyTimetableTemplateRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApplyTimetableTemplateRequest(UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public ApplyTimetableTemplateRequest timetableTemplateId(UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template.
   * @return timetableTemplateId
   */
  @Valid
  @JsonProperty("timetableTemplateId")
  public @Nullable UUID getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplyTimetableTemplateRequest applyTimetableTemplateRequest = (ApplyTimetableTemplateRequest) o;
    return Objects.equals(this.timetableTemplateId, applyTimetableTemplateRequest.timetableTemplateId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timetableTemplateId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplyTimetableTemplateRequest {\n");
    sb.append("    timetableTemplateId: ").append(toIndentedString(timetableTemplateId)).append("\n");
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

