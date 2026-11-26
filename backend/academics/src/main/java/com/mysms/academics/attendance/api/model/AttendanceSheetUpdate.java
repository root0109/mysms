// Owned API contract of attendance.
package com.mysms.academics.attendance.api.model;

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
 * Fields of AttendanceSheet that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AttendanceSheetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> registerId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> attendanceDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> timetableSessionId;

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
  private @Nullable Optional<@NotNull DayPartEnum> dayPart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> reference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> takenByStaffId;

  public AttendanceSheetUpdate registerId(@Nullable Optional<UUID> registerId) {
    this.registerId = registerId;
    return this;
  }

  /**
   * Register.
   * @return registerId
   */
  
  @JsonProperty("registerId")
  public @Nullable Optional<UUID> getRegisterId() {
    return registerId;
  }

  @JsonProperty("registerId")
  public void setRegisterId(@Nullable Optional<UUID> registerId) {
    this.registerId = registerId;
  }

  public AttendanceSheetUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public AttendanceSheetUpdate attendanceDate(@Nullable Optional<LocalDate> attendanceDate) {
    this.attendanceDate = attendanceDate;
    return this;
  }

  /**
   * Date.
   * @return attendanceDate
   */
  
  @JsonProperty("attendanceDate")
  public @Nullable Optional<LocalDate> getAttendanceDate() {
    return attendanceDate;
  }

  @JsonProperty("attendanceDate")
  public void setAttendanceDate(@Nullable Optional<LocalDate> attendanceDate) {
    this.attendanceDate = attendanceDate;
  }

  public AttendanceSheetUpdate timetableSessionId(@Nullable Optional<UUID> timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
    return this;
  }

  /**
   * Timetable session the sheet is for.
   * @return timetableSessionId
   */
  
  @JsonProperty("timetableSessionId")
  public @Nullable Optional<UUID> getTimetableSessionId() {
    return timetableSessionId;
  }

  @JsonProperty("timetableSessionId")
  public void setTimetableSessionId(@Nullable Optional<UUID> timetableSessionId) {
    this.timetableSessionId = timetableSessionId;
  }

  public AttendanceSheetUpdate dayPart(@Nullable Optional<DayPartEnum> dayPart) {
    this.dayPart = dayPart;
    return this;
  }

  /**
   * Part of the day (daily attendance).
   * @return dayPart
   */
  
  @JsonProperty("dayPart")
  public @Nullable Optional<DayPartEnum> getDayPart() {
    return dayPart;
  }

  @JsonProperty("dayPart")
  public void setDayPart(@Nullable Optional<DayPartEnum> dayPart) {
    this.dayPart = dayPart;
  }

  public AttendanceSheetUpdate reference(@Nullable Optional<String> reference) {
    this.reference = reference;
    return this;
  }

  /**
   * Sheet reference number.
   * @return reference
   */
  
  @JsonProperty("reference")
  public @Nullable Optional<String> getReference() {
    return reference;
  }

  @JsonProperty("reference")
  public void setReference(@Nullable Optional<String> reference) {
    this.reference = reference;
  }

  public AttendanceSheetUpdate takenByStaffId(@Nullable Optional<UUID> takenByStaffId) {
    this.takenByStaffId = takenByStaffId;
    return this;
  }

  /**
   * Staff member taking attendance.
   * @return takenByStaffId
   */
  
  @JsonProperty("takenByStaffId")
  public @Nullable Optional<UUID> getTakenByStaffId() {
    return takenByStaffId;
  }

  @JsonProperty("takenByStaffId")
  public void setTakenByStaffId(@Nullable Optional<UUID> takenByStaffId) {
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

