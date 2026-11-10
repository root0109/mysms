// Owned API contract of attendance (JAVA-CODING-RULES §7); first materialized from api/openapi/attendance.yaml on 2026-11-10.
package com.mysms.attendance.api.model;

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
 * Fields of StaffAttendance that can be updated (partial update; see updateMask).
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
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime checkInAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime checkOutAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID leaveApplicationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remark;

  public StaffAttendanceUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Day status.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public StaffAttendanceUpdate checkInAt(@Nullable OffsetDateTime checkInAt) {
    this.checkInAt = checkInAt;
    return this;
  }

  /**
   * Clock-in time when captured.
   * @return checkInAt
   */
  @Valid 
  @JsonProperty("checkInAt")
  public @Nullable OffsetDateTime getCheckInAt() {
    return checkInAt;
  }

  @JsonProperty("checkInAt")
  public void setCheckInAt(@Nullable OffsetDateTime checkInAt) {
    this.checkInAt = checkInAt;
  }

  public StaffAttendanceUpdate checkOutAt(@Nullable OffsetDateTime checkOutAt) {
    this.checkOutAt = checkOutAt;
    return this;
  }

  /**
   * Clock-out time when captured.
   * @return checkOutAt
   */
  @Valid 
  @JsonProperty("checkOutAt")
  public @Nullable OffsetDateTime getCheckOutAt() {
    return checkOutAt;
  }

  @JsonProperty("checkOutAt")
  public void setCheckOutAt(@Nullable OffsetDateTime checkOutAt) {
    this.checkOutAt = checkOutAt;
  }

  public StaffAttendanceUpdate leaveApplicationId(@Nullable UUID leaveApplicationId) {
    this.leaveApplicationId = leaveApplicationId;
    return this;
  }

  /**
   * Approved leave application that explains an on_leave day.
   * @return leaveApplicationId
   */
  @Valid 
  @JsonProperty("leaveApplicationId")
  public @Nullable UUID getLeaveApplicationId() {
    return leaveApplicationId;
  }

  @JsonProperty("leaveApplicationId")
  public void setLeaveApplicationId(@Nullable UUID leaveApplicationId) {
    this.leaveApplicationId = leaveApplicationId;
  }

  public StaffAttendanceUpdate remark(@Nullable String remark) {
    this.remark = remark;
    return this;
  }

  /**
   * Free-text remark.
   * @return remark
   */
  @Size(max = 300) 
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
    StaffAttendanceUpdate staffAttendanceUpdate = (StaffAttendanceUpdate) o;
    return Objects.equals(this.status, staffAttendanceUpdate.status) &&
        Objects.equals(this.checkInAt, staffAttendanceUpdate.checkInAt) &&
        Objects.equals(this.checkOutAt, staffAttendanceUpdate.checkOutAt) &&
        Objects.equals(this.leaveApplicationId, staffAttendanceUpdate.leaveApplicationId) &&
        Objects.equals(this.remark, staffAttendanceUpdate.remark);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, checkInAt, checkOutAt, leaveApplicationId, remark);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffAttendanceUpdate {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    checkInAt: ").append(toIndentedString(checkInAt)).append("\n");
    sb.append("    checkOutAt: ").append(toIndentedString(checkOutAt)).append("\n");
    sb.append("    leaveApplicationId: ").append(toIndentedString(leaveApplicationId)).append("\n");
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

