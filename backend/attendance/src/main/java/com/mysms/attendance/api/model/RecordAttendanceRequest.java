// Owned API contract of attendance.
package com.mysms.attendance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.attendance.api.model.AttendanceMark;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RecordAttendanceRequest
 */

public class RecordAttendanceRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid AttendanceMark> marks = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean complete;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public RecordAttendanceRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RecordAttendanceRequest(List<@Valid AttendanceMark> marks) {
    this.marks = marks;
  }

  public RecordAttendanceRequest marks(List<@Valid AttendanceMark> marks) {
    this.marks = marks;
    return this;
  }

  public RecordAttendanceRequest addMarksItem(AttendanceMark marksItem) {
    if (this.marks == null) {
      this.marks = new ArrayList<>();
    }
    this.marks.add(marksItem);
    return this;
  }

  /**
   * One mark per student (upsert).
   * @return marks
   */
  @NotNull 
  @JsonProperty("marks")
  public List<@Valid AttendanceMark> getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(List<@Valid AttendanceMark> marks) {
    this.marks = marks;
  }

  public RecordAttendanceRequest complete(@Nullable Boolean complete) {
    this.complete = complete;
    return this;
  }

  /**
   * Also complete the sheet.
   * @return complete
   */
  
  @JsonProperty("complete")
  public @Nullable Boolean getComplete() {
    return complete;
  }

  @JsonProperty("complete")
  public void setComplete(@Nullable Boolean complete) {
    this.complete = complete;
  }

  public RecordAttendanceRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token of the sheet from the last read (optional but recommended).
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordAttendanceRequest recordAttendanceRequest = (RecordAttendanceRequest) o;
    return Objects.equals(this.marks, recordAttendanceRequest.marks) &&
        Objects.equals(this.complete, recordAttendanceRequest.complete) &&
        Objects.equals(this.etag, recordAttendanceRequest.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(marks, complete, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordAttendanceRequest {\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
    sb.append("    complete: ").append(toIndentedString(complete)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

