// Owned API contract of timetable.
package com.mysms.timetable.api.model;

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
 * Fields of SessionStudent that can be updated (partial update; see updateMask).
 */

public class SessionStudentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  public SessionStudentUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SessionStudentUpdate sessionStudentUpdate = (SessionStudentUpdate) o;
    return Objects.equals(this.studentId, sessionStudentUpdate.studentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SessionStudentUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
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

