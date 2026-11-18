// Owned API contract of attendance.
package com.mysms.attendance.api.model;

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
 * AttendanceMark
 */

public class AttendanceMark {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  /**
   * Status.
   */
  public enum StatusEnum {
    PRESENT("present"),
    
    ABSENT("absent"),
    
    LATE("late"),
    
    EXCUSED("excused");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attendanceTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID absenceReasonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remark;

  public AttendanceMark() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AttendanceMark(UUID studentId, StatusEnum status) {
    this.studentId = studentId;
    this.status = status;
  }

  public AttendanceMark studentId(UUID studentId) {
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

  public AttendanceMark status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Status.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public AttendanceMark attendanceTypeId(@Nullable UUID attendanceTypeId) {
    this.attendanceTypeId = attendanceTypeId;
    return this;
  }

  /**
   * Configured mark.
   * @return attendanceTypeId
   */
  @Valid 
  @JsonProperty("attendanceTypeId")
  public @Nullable UUID getAttendanceTypeId() {
    return attendanceTypeId;
  }

  @JsonProperty("attendanceTypeId")
  public void setAttendanceTypeId(@Nullable UUID attendanceTypeId) {
    this.attendanceTypeId = attendanceTypeId;
  }

  public AttendanceMark absenceReasonId(@Nullable UUID absenceReasonId) {
    this.absenceReasonId = absenceReasonId;
    return this;
  }

  /**
   * Reason.
   * @return absenceReasonId
   */
  @Valid 
  @JsonProperty("absenceReasonId")
  public @Nullable UUID getAbsenceReasonId() {
    return absenceReasonId;
  }

  @JsonProperty("absenceReasonId")
  public void setAbsenceReasonId(@Nullable UUID absenceReasonId) {
    this.absenceReasonId = absenceReasonId;
  }

  public AttendanceMark remark(@Nullable String remark) {
    this.remark = remark;
    return this;
  }

  /**
   * Remark.
   * @return remark
   */
  
  @JsonProperty("remark")
  public @Nullable String getRemark() {
    return remark;
  }

  @JsonProperty("remark")
  public void setRemark(@Nullable String remark) {
    this.remark = remark;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttendanceMark attendanceMark = (AttendanceMark) o;
    return Objects.equals(this.studentId, attendanceMark.studentId) &&
        Objects.equals(this.status, attendanceMark.status) &&
        Objects.equals(this.attendanceTypeId, attendanceMark.attendanceTypeId) &&
        Objects.equals(this.absenceReasonId, attendanceMark.absenceReasonId) &&
        Objects.equals(this.remark, attendanceMark.remark);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, status, attendanceTypeId, absenceReasonId, remark);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceMark {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    attendanceTypeId: ").append(toIndentedString(attendanceTypeId)).append("\n");
    sb.append("    absenceReasonId: ").append(toIndentedString(absenceReasonId)).append("\n");
    sb.append("    remark: ").append(toIndentedString(remark)).append("\n");
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

