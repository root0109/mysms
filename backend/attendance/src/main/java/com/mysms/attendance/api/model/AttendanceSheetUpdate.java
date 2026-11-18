// Owned API contract of attendance.
package com.mysms.attendance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AttendanceSheet that can be updated (partial update; see updateMask).
 */

public class AttendanceSheetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID registerId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate attendanceDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID timetableSessionId;

  /**
   * Part of the day (daily attendance).
   */
  public enum DayPartEnum {
    FULL_DAY("full_day"),
    
    FORENOON("forenoon"),
    
    AFTERNOON("afternoon");

    private final String value;

    DayPartEnum(String value) {
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
    public static DayPartEnum fromValue(String value) {
      for (DayPartEnum b : DayPartEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DayPartEnum dayPart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID takenByStaffId;

  public AttendanceSheetUpdate registerId(@Nullable UUID registerId) {
    this.registerId = registerId;
    return this;
  }

  /**
   * Register.
   * @return registerId
   */
  @Valid 
  @JsonProperty("registerId")
  public @Nullable UUID getRegisterId() {
    return registerId;
  }

  @JsonProperty("registerId")
  public void setRegisterId(@Nullable UUID registerId) {
    this.registerId = registerId;
  }

  public AttendanceSheetUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public AttendanceSheetUpdate attendanceDate(@Nullable LocalDate attendanceDate) {
    this.attendanceDate = attendanceDate;
    return this;
  }

  /**
   * Date.
   * @return attendanceDate
   */
  @Valid 
  @JsonProperty("attendanceDate")
  public @Nullable LocalDate getAttendanceDate() {
    return attendanceDate;
  }

  @JsonProperty("attendanceDate")
  public void setAttendanceDate(@Nullable LocalDate attendanceDate) {
    this.attendanceDate = attendanceDate;
  }

  public AttendanceSheetUpdate timetableSessionId(@Nullable UUID timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
    return this;
  }

  /**
   * Timetable session the sheet is for.
   * @return timetableSessionId
   */
  @Valid 
  @JsonProperty("timetableSessionId")
  public @Nullable UUID getTimetableSessionId() {
    return timetableSessionId;
  }

  @JsonProperty("timetableSessionId")
  public void setTimetableSessionId(@Nullable UUID timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
  }

  public AttendanceSheetUpdate dayPart(@Nullable DayPartEnum dayPart) {
    this.dayPart = dayPart;
    return this;
  }

  /**
   * Part of the day (daily attendance).
   * @return dayPart
   */
  
  @JsonProperty("dayPart")
  public @Nullable DayPartEnum getDayPart() {
    return dayPart;
  }

  @JsonProperty("dayPart")
  public void setDayPart(@Nullable DayPartEnum dayPart) {
    this.dayPart = dayPart;
  }

  public AttendanceSheetUpdate reference(@Nullable String reference) {
    this.reference = reference;
    return this;
  }

  /**
   * Sheet reference number.
   * @return reference
   */
  @Size(max = 60) 
  @JsonProperty("reference")
  public @Nullable String getReference() {
    return reference;
  }

  @JsonProperty("reference")
  public void setReference(@Nullable String reference) {
    this.reference = reference;
  }

  public AttendanceSheetUpdate takenByStaffId(@Nullable UUID takenByStaffId) {
    this.takenByStaffId = takenByStaffId;
    return this;
  }

  /**
   * Staff member taking attendance.
   * @return takenByStaffId
   */
  @Valid 
  @JsonProperty("takenByStaffId")
  public @Nullable UUID getTakenByStaffId() {
    return takenByStaffId;
  }

  @JsonProperty("takenByStaffId")
  public void setTakenByStaffId(@Nullable UUID takenByStaffId) {
    this.takenByStaffId = takenByStaffId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendanceSheetUpdate attendanceSheetUpdate = (AttendanceSheetUpdate) o;
    return Objects.equals(this.registerId, attendanceSheetUpdate.registerId) &&
        Objects.equals(this.academicYearId, attendanceSheetUpdate.academicYearId) &&
        Objects.equals(this.attendanceDate, attendanceSheetUpdate.attendanceDate) &&
        Objects.equals(this.timetableSessionId, attendanceSheetUpdate.timetableSessionId) &&
        Objects.equals(this.dayPart, attendanceSheetUpdate.dayPart) &&
        Objects.equals(this.reference, attendanceSheetUpdate.reference) &&
        Objects.equals(this.takenByStaffId, attendanceSheetUpdate.takenByStaffId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(registerId, academicYearId, attendanceDate, timetableSessionId, dayPart, reference, takenByStaffId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceSheetUpdate {\n");
    sb.append("    registerId: ").append(toIndentedString(registerId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    attendanceDate: ").append(toIndentedString(attendanceDate)).append("\n");
    sb.append("    timetableSessionId: ").append(toIndentedString(timetableSessionId)).append("\n");
    sb.append("    dayPart: ").append(toIndentedString(dayPart)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
    sb.append("    takenByStaffId: ").append(toIndentedString(takenByStaffId)).append("\n");
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

