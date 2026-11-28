// Owned API contract of time_attendance.
package com.mysms.hrms.timeattendance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of StaffAttendance that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StaffAttendanceUpdate {

  /**
   * Day status.
   */
  public enum StatusEnum {
    PRESENT("present"),
    
    ABSENT("absent"),
    
    HALF_DAY("half_day"),
    
    LATE("late"),
    
    ON_LEAVE("on_leave"),
    
    HOLIDAY("holiday");

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
  private @Nullable Optional<@NotNull StatusEnum> status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> checkInAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> checkOutAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 300) String> remark;

  public StaffAttendanceUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * Day status.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
    this.status = status;
  }

  public StaffAttendanceUpdate checkInAt(@Nullable Optional<OffsetDateTime> checkInAt) {
    this.checkInAt = checkInAt;
    return this;
  }

  /**
   * Clock-in time when captured.
   * @return checkInAt
   */
  
  @JsonProperty("checkInAt")
  public @Nullable Optional<OffsetDateTime> getCheckInAt() {
    return checkInAt;
  }

  @JsonProperty("checkInAt")
  public void setCheckInAt(@Nullable Optional<OffsetDateTime> checkInAt) {
    this.checkInAt = checkInAt;
  }

  public StaffAttendanceUpdate checkOutAt(@Nullable Optional<OffsetDateTime> checkOutAt) {
    this.checkOutAt = checkOutAt;
    return this;
  }

  /**
   * Clock-out time when captured.
   * @return checkOutAt
   */
  
  @JsonProperty("checkOutAt")
  public @Nullable Optional<OffsetDateTime> getCheckOutAt() {
    return checkOutAt;
  }

  @JsonProperty("checkOutAt")
  public void setCheckOutAt(@Nullable Optional<OffsetDateTime> checkOutAt) {
    this.checkOutAt = checkOutAt;
  }

  public StaffAttendanceUpdate remark(@Nullable Optional<String> remark) {
    this.remark = remark;
    return this;
  }

  /**
   * Free-text remark.
   * @return remark
   */
  
  @JsonProperty("remark")
  public @Nullable Optional<String> getRemark() {
    return remark;
  }

  @JsonProperty("remark")
  public void setRemark(@Nullable Optional<String> remark) {
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
    StaffAttendanceUpdate staffAttendanceUpdate = (StaffAttendanceUpdate) o;
    return Objects.equals(this.status, staffAttendanceUpdate.status) &&
        Objects.equals(this.checkInAt, staffAttendanceUpdate.checkInAt) &&
        Objects.equals(this.checkOutAt, staffAttendanceUpdate.checkOutAt) &&
        Objects.equals(this.remark, staffAttendanceUpdate.remark);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, checkInAt, checkOutAt, remark);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffAttendanceUpdate {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    checkInAt: ").append(toIndentedString(checkInAt)).append("\n");
    sb.append("    checkOutAt: ").append(toIndentedString(checkOutAt)).append("\n");
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

