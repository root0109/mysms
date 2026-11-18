// Owned API contract of sis.
package com.mysms.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * AttendanceSummary
 */

public class AttendanceSummary {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate date;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer present;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer absent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer late;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer excused;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String attendancePercent;

  public AttendanceSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceSummary(Integer present, Integer absent, Integer late, Integer excused, String attendancePercent) {
    this.present = present;
    this.absent = absent;
    this.late = late;
    this.excused = excused;
    this.attendancePercent = attendancePercent;
  }

  public AttendanceSummary studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student (when grouped by student).
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

  public AttendanceSummary classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (when grouped by class).
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public AttendanceSummary date(@Nullable LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Date (when grouped by date).
   * @return date
   */
  @Valid 
  @JsonProperty("date")
  public @Nullable LocalDate getDate() {
    return date;
  }

  @JsonProperty("date")
  public void setDate(@Nullable LocalDate date) {
    this.date = date;
  }

  public AttendanceSummary present(Integer present) {
    this.present = present;
    return this;
  }

  /**
   * Present count.
   * @return present
   */
  @NotNull 
  @JsonProperty("present")
  public Integer getPresent() {
    return present;
  }

  @JsonProperty("present")
  public void setPresent(Integer present) {
    this.present = present;
  }

  public AttendanceSummary absent(Integer absent) {
    this.absent = absent;
    return this;
  }

  /**
   * Absent count.
   * @return absent
   */
  @NotNull 
  @JsonProperty("absent")
  public Integer getAbsent() {
    return absent;
  }

  @JsonProperty("absent")
  public void setAbsent(Integer absent) {
    this.absent = absent;
  }

  public AttendanceSummary late(Integer late) {
    this.late = late;
    return this;
  }

  /**
   * Late count.
   * @return late
   */
  @NotNull 
  @JsonProperty("late")
  public Integer getLate() {
    return late;
  }

  @JsonProperty("late")
  public void setLate(Integer late) {
    this.late = late;
  }

  public AttendanceSummary excused(Integer excused) {
    this.excused = excused;
    return this;
  }

  /**
   * Excused count.
   * @return excused
   */
  @NotNull 
  @JsonProperty("excused")
  public Integer getExcused() {
    return excused;
  }

  @JsonProperty("excused")
  public void setExcused(Integer excused) {
    this.excused = excused;
  }

  public AttendanceSummary attendancePercent(String attendancePercent) {
    this.attendancePercent = attendancePercent;
    return this;
  }

  /**
   * Present+late / total marks.
   * @return attendancePercent
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("attendancePercent")
  public String getAttendancePercent() {
    return attendancePercent;
  }

  @JsonProperty("attendancePercent")
  public void setAttendancePercent(String attendancePercent) {
    this.attendancePercent = attendancePercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendanceSummary attendanceSummary = (AttendanceSummary) o;
    return Objects.equals(this.studentId, attendanceSummary.studentId) &&
        Objects.equals(this.classSectionId, attendanceSummary.classSectionId) &&
        Objects.equals(this.date, attendanceSummary.date) &&
        Objects.equals(this.present, attendanceSummary.present) &&
        Objects.equals(this.absent, attendanceSummary.absent) &&
        Objects.equals(this.late, attendanceSummary.late) &&
        Objects.equals(this.excused, attendanceSummary.excused) &&
        Objects.equals(this.attendancePercent, attendanceSummary.attendancePercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, classSectionId, date, present, absent, late, excused, attendancePercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceSummary {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    present: ").append(toIndentedString(present)).append("\n");
    sb.append("    absent: ").append(toIndentedString(absent)).append("\n");
    sb.append("    late: ").append(toIndentedString(late)).append("\n");
    sb.append("    excused: ").append(toIndentedString(excused)).append("\n");
    sb.append("    attendancePercent: ").append(toIndentedString(attendancePercent)).append("\n");
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

