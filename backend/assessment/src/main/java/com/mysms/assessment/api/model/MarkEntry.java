// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
package com.mysms.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * MarkEntry
 */

public class MarkEntry {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  /**
   * Attendance.
   */
  public enum AttendanceStatusEnum {
    PRESENT("present"),
    
    ABSENT("absent");

    private final String value;

    AttendanceStatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static AttendanceStatusEnum fromValue(String value) {
      for (AttendanceStatusEnum b : AttendanceStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private AttendanceStatusEnum attendanceStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String marksObtained;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  public MarkEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MarkEntry(UUID studentId, AttendanceStatusEnum attendanceStatus) {
    this.studentId = studentId;
    this.attendanceStatus = attendanceStatus;
  }

  public MarkEntry studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public MarkEntry attendanceStatus(AttendanceStatusEnum attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
    return this;
  }

  /**
   * Attendance.
   * @return attendanceStatus
   */
  @NotNull 
  @JsonProperty("attendanceStatus")
  public AttendanceStatusEnum getAttendanceStatus() {
    return attendanceStatus;
  }

  @JsonProperty("attendanceStatus")
  public void setAttendanceStatus(AttendanceStatusEnum attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
  }

  public MarkEntry marksObtained(@Nullable String marksObtained) {
    this.marksObtained = marksObtained;
    return this;
  }

  /**
   * Marks.
   * @return marksObtained
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marksObtained")
  public @Nullable String getMarksObtained() {
    return marksObtained;
  }

  @JsonProperty("marksObtained")
  public void setMarksObtained(@Nullable String marksObtained) {
    this.marksObtained = marksObtained;
  }

  public MarkEntry note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * Note.
   * @return note
   */
  
  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarkEntry markEntry = (MarkEntry) o;
    return Objects.equals(this.studentId, markEntry.studentId) &&
        Objects.equals(this.attendanceStatus, markEntry.attendanceStatus) &&
        Objects.equals(this.marksObtained, markEntry.marksObtained) &&
        Objects.equals(this.note, markEntry.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, attendanceStatus, marksObtained, note);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarkEntry {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    attendanceStatus: ").append(toIndentedString(attendanceStatus)).append("\n");
    sb.append("    marksObtained: ").append(toIndentedString(marksObtained)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

