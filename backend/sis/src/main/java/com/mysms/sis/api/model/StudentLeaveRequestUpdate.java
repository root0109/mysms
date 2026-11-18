// Owned API contract of sis.
package com.mysms.sis.api.model;

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
 * Fields of StudentLeaveRequest that can be updated (partial update; see updateMask).
 */

public class StudentLeaveRequestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate fromDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate toDate;

  /**
   * Which part of the day.
   */
  public enum DayPartEnum {
    FULL_DAY("full_day"),
    
    FIRST_HALF("first_half"),
    
    SECOND_HALF("second_half");

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
  private @Nullable String reason;

  public StudentLeaveRequestUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student the leave is for.
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

  public StudentLeaveRequestUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year the leave falls in.
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

  public StudentLeaveRequestUpdate fromDate(@Nullable LocalDate fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  /**
   * First day of leave.
   * @return fromDate
   */
  @Valid 
  @JsonProperty("fromDate")
  public @Nullable LocalDate getFromDate() {
    return fromDate;
  }

  @JsonProperty("fromDate")
  public void setFromDate(@Nullable LocalDate fromDate) {
    this.fromDate = fromDate;
  }

  public StudentLeaveRequestUpdate toDate(@Nullable LocalDate toDate) {
    this.toDate = toDate;
    return this;
  }

  /**
   * Last day of leave.
   * @return toDate
   */
  @Valid 
  @JsonProperty("toDate")
  public @Nullable LocalDate getToDate() {
    return toDate;
  }

  @JsonProperty("toDate")
  public void setToDate(@Nullable LocalDate toDate) {
    this.toDate = toDate;
  }

  public StudentLeaveRequestUpdate dayPart(@Nullable DayPartEnum dayPart) {
    this.dayPart = dayPart;
    return this;
  }

  /**
   * Which part of the day.
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

  public StudentLeaveRequestUpdate reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Stated reason.
   * @return reason
   */
  @Size(max = 500) 
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentLeaveRequestUpdate studentLeaveRequestUpdate = (StudentLeaveRequestUpdate) o;
    return Objects.equals(this.studentId, studentLeaveRequestUpdate.studentId) &&
        Objects.equals(this.academicYearId, studentLeaveRequestUpdate.academicYearId) &&
        Objects.equals(this.fromDate, studentLeaveRequestUpdate.fromDate) &&
        Objects.equals(this.toDate, studentLeaveRequestUpdate.toDate) &&
        Objects.equals(this.dayPart, studentLeaveRequestUpdate.dayPart) &&
        Objects.equals(this.reason, studentLeaveRequestUpdate.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, academicYearId, fromDate, toDate, dayPart, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentLeaveRequestUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    fromDate: ").append(toIndentedString(fromDate)).append("\n");
    sb.append("    toDate: ").append(toIndentedString(toDate)).append("\n");
    sb.append("    dayPart: ").append(toIndentedString(dayPart)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

