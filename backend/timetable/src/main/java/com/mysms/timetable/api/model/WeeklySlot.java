// Owned API contract of timetable.
package com.mysms.timetable.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * WeeklySlot
 */

public class WeeklySlot {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer weekday;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID periodId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roomId;

  public WeeklySlot() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WeeklySlot(Integer weekday, UUID periodId, UUID subjectId, UUID staffId) {
    this.weekday = weekday;
    this.periodId = periodId;
    this.subjectId = subjectId;
    this.staffId = staffId;
  }

  public WeeklySlot weekday(Integer weekday) {
    this.weekday = weekday;
    return this;
  }

  /**
   * ISO weekday 1-7.
   * @return weekday
   */
  @NotNull 
  @JsonProperty("weekday")
  public Integer getWeekday() {
    return weekday;
  }

  @JsonProperty("weekday")
  public void setWeekday(Integer weekday) {
    this.weekday = weekday;
  }

  public WeeklySlot periodId(UUID periodId) {
    this.periodId = periodId;
    return this;
  }

  /**
   * Period.
   * @return periodId
   */
  @NotNull @Valid 
  @JsonProperty("periodId")
  public UUID getPeriodId() {
    return periodId;
  }

  @JsonProperty("periodId")
  public void setPeriodId(UUID periodId) {
    this.periodId = periodId;
  }

  public WeeklySlot subjectId(UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  @NotNull @Valid 
  @JsonProperty("subjectId")
  public UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(UUID subjectId) {
    this.subjectId = subjectId;
  }

  public WeeklySlot staffId(UUID staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Teacher.
   * @return staffId
   */
  @NotNull @Valid 
  @JsonProperty("staffId")
  public UUID getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(UUID staffId) {
    this.staffId = staffId;
  }

  public WeeklySlot roomId(@Nullable UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Room.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WeeklySlot weeklySlot = (WeeklySlot) o;
    return Objects.equals(this.weekday, weeklySlot.weekday) &&
        Objects.equals(this.periodId, weeklySlot.periodId) &&
        Objects.equals(this.subjectId, weeklySlot.subjectId) &&
        Objects.equals(this.staffId, weeklySlot.staffId) &&
        Objects.equals(this.roomId, weeklySlot.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(weekday, periodId, subjectId, staffId, roomId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WeeklySlot {\n");
    sb.append("    weekday: ").append(toIndentedString(weekday)).append("\n");
    sb.append("    periodId: ").append(toIndentedString(periodId)).append("\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
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

