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
 * Fields of ExamResult that can be updated (partial update; see updateMask).
 */

public class ExamResultUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roomId;

  /**
   * Attendance.
   */
  public enum AttendanceStatusEnum {
    EXPECTED("expected"),
    
    PRESENT("present"),
    
    ABSENT("absent"),
    
    EXEMPTED("exempted");

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
  private @Nullable AttendanceStatusEnum attendanceStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  public ExamResultUpdate roomId(@Nullable UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Exam room.
   * @return roomId
   */
  @Valid 
  @JsonProperty("roomId")
  public @Nullable UUID getRoomId() {
    return roomId;
  }

  @JsonProperty("roomId")
  public void setRoomId(@Nullable UUID roomId) {
    this.roomId = roomId;
  }

  public ExamResultUpdate attendanceStatus(@Nullable AttendanceStatusEnum attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
    return this;
  }

  /**
   * Attendance.
   * @return attendanceStatus
   */
  
  @JsonProperty("attendanceStatus")
  public @Nullable AttendanceStatusEnum getAttendanceStatus() {
    return attendanceStatus;
  }

  @JsonProperty("attendanceStatus")
  public void setAttendanceStatus(@Nullable AttendanceStatusEnum attendanceStatus) {
    this.attendanceStatus = attendanceStatus;
  }

  public ExamResultUpdate note(@Nullable String note) {
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
    ExamResultUpdate examResultUpdate = (ExamResultUpdate) o;
    return Objects.equals(this.roomId, examResultUpdate.roomId) &&
        Objects.equals(this.attendanceStatus, examResultUpdate.attendanceStatus) &&
        Objects.equals(this.note, examResultUpdate.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, attendanceStatus, note);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExamResultUpdate {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    attendanceStatus: ").append(toIndentedString(attendanceStatus)).append("\n");
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

