// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SetStudentAccessRequest
 */

public class SetStudentAccessRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime studentAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime studentAccessUntil;

  public SetStudentAccessRequest studentAccessFrom(@Nullable OffsetDateTime studentAccessFrom) {
    this.studentAccessFrom = studentAccessFrom;
    return this;
  }

  /**
   * From.
   * @return studentAccessFrom
   */
  @Valid 
  @JsonProperty("studentAccessFrom")
  public @Nullable OffsetDateTime getStudentAccessFrom() {
    return studentAccessFrom;
  }

  @JsonProperty("studentAccessFrom")
  public void setStudentAccessFrom(@Nullable OffsetDateTime studentAccessFrom) {
    this.studentAccessFrom = studentAccessFrom;
  }

  public SetStudentAccessRequest studentAccessUntil(@Nullable OffsetDateTime studentAccessUntil) {
    this.studentAccessUntil = studentAccessUntil;
    return this;
  }

  /**
   * Until.
   * @return studentAccessUntil
   */
  @Valid 
  @JsonProperty("studentAccessUntil")
  public @Nullable OffsetDateTime getStudentAccessUntil() {
    return studentAccessUntil;
  }

  @JsonProperty("studentAccessUntil")
  public void setStudentAccessUntil(@Nullable OffsetDateTime studentAccessUntil) {
    this.studentAccessUntil = studentAccessUntil;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetStudentAccessRequest setStudentAccessRequest = (SetStudentAccessRequest) o;
    return Objects.equals(this.studentAccessFrom, setStudentAccessRequest.studentAccessFrom) &&
        Objects.equals(this.studentAccessUntil, setStudentAccessRequest.studentAccessUntil);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentAccessFrom, studentAccessUntil);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetStudentAccessRequest {\n");
    sb.append("    studentAccessFrom: ").append(toIndentedString(studentAccessFrom)).append("\n");
    sb.append("    studentAccessUntil: ").append(toIndentedString(studentAccessUntil)).append("\n");
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

