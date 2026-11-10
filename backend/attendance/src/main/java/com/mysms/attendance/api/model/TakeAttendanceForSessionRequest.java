// Owned API contract of attendance (JAVA-CODING-RULES §7); first materialized from api/openapi/attendance.yaml on 2026-11-10.
package com.mysms.attendance.api.model;

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
 * TakeAttendanceForSessionRequest
 */

public class TakeAttendanceForSessionRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID timetableSessionId;

  public TakeAttendanceForSessionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TakeAttendanceForSessionRequest(UUID timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
  }

  public TakeAttendanceForSessionRequest timetableSessionId(UUID timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
    return this;
  }

  /**
   * Session.
   * @return timetableSessionId
   */
  @NotNull @Valid 
  @JsonProperty("timetableSessionId")
  public UUID getTimetableSessionId() {
    return timetableSessionId;
  }

  @JsonProperty("timetableSessionId")
  public void setTimetableSessionId(UUID timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TakeAttendanceForSessionRequest takeAttendanceForSessionRequest = (TakeAttendanceForSessionRequest) o;
    return Objects.equals(this.timetableSessionId, takeAttendanceForSessionRequest.timetableSessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timetableSessionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TakeAttendanceForSessionRequest {\n");
    sb.append("    timetableSessionId: ").append(toIndentedString(timetableSessionId)).append("\n");
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

