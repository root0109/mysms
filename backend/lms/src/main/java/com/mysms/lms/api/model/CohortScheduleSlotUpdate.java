// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of CohortScheduleSlot that can be updated (partial update; see updateMask).
 */

public class CohortScheduleSlotUpdate {

  /**
   * Kind of slot.
   */
  public enum SlotTypeEnum {
    CLASS_MEETING("class_meeting"),
    
    OFFICE_HOURS("office_hours");

    private final String value;

    SlotTypeEnum(String value) {
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
    public static SlotTypeEnum fromValue(String value) {
      for (SlotTypeEnum b : SlotTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable SlotTypeEnum slotType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortTeacherId;

  /**
   * Day of week.
   */
  public enum WeekdayEnum {
    MONDAY("monday"),
    
    TUESDAY("tuesday"),
    
    WEDNESDAY("wednesday"),
    
    THURSDAY("thursday"),
    
    FRIDAY("friday"),
    
    SATURDAY("saturday"),
    
    SUNDAY("sunday");

    private final String value;

    WeekdayEnum(String value) {
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
    public static WeekdayEnum fromValue(String value) {
      for (WeekdayEnum b : WeekdayEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable WeekdayEnum weekday;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String venue;

  public CohortScheduleSlotUpdate slotType(@Nullable SlotTypeEnum slotType) {
    this.slotType = slotType;
    return this;
  }

  /**
   * Kind of slot.
   * @return slotType
   */
  
  @JsonProperty("slotType")
  public @Nullable SlotTypeEnum getSlotType() {
    return slotType;
  }

  @JsonProperty("slotType")
  public void setSlotType(@Nullable SlotTypeEnum slotType) {
    this.slotType = slotType;
  }

  public CohortScheduleSlotUpdate cohortTeacherId(@Nullable UUID cohortTeacherId) {
    this.cohortTeacherId = cohortTeacherId;
    return this;
  }

  /**
   * Teacher (office hours).
   * @return cohortTeacherId
   */
  @Valid 
  @JsonProperty("cohortTeacherId")
  public @Nullable UUID getCohortTeacherId() {
    return cohortTeacherId;
  }

  @JsonProperty("cohortTeacherId")
  public void setCohortTeacherId(@Nullable UUID cohortTeacherId) {
    this.cohortTeacherId = cohortTeacherId;
  }

  public CohortScheduleSlotUpdate weekday(@Nullable WeekdayEnum weekday) {
    this.weekday = weekday;
    return this;
  }

  /**
   * Day of week.
   * @return weekday
   */
  
  @JsonProperty("weekday")
  public @Nullable WeekdayEnum getWeekday() {
    return weekday;
  }

  @JsonProperty("weekday")
  public void setWeekday(@Nullable WeekdayEnum weekday) {
    this.weekday = weekday;
  }

  public CohortScheduleSlotUpdate startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start.
   * @return startTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public @Nullable String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable String startTime) {
    this.startTime = startTime;
  }

  public CohortScheduleSlotUpdate endTime(@Nullable String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * End.
   * @return endTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("endTime")
  public @Nullable String getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable String endTime) {
    this.endTime = endTime;
  }

  public CohortScheduleSlotUpdate venue(@Nullable String venue) {
    this.venue = venue;
    return this;
  }

  /**
   * Room / location / online link.
   * @return venue
   */
  @Size(max = 200) 
  @JsonProperty("venue")
  public @Nullable String getVenue() {
    return venue;
  }

  @JsonProperty("venue")
  public void setVenue(@Nullable String venue) {
    this.venue = venue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortScheduleSlotUpdate cohortScheduleSlotUpdate = (CohortScheduleSlotUpdate) o;
    return Objects.equals(this.slotType, cohortScheduleSlotUpdate.slotType) &&
        Objects.equals(this.cohortTeacherId, cohortScheduleSlotUpdate.cohortTeacherId) &&
        Objects.equals(this.weekday, cohortScheduleSlotUpdate.weekday) &&
        Objects.equals(this.startTime, cohortScheduleSlotUpdate.startTime) &&
        Objects.equals(this.endTime, cohortScheduleSlotUpdate.endTime) &&
        Objects.equals(this.venue, cohortScheduleSlotUpdate.venue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slotType, cohortTeacherId, weekday, startTime, endTime, venue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortScheduleSlotUpdate {\n");
    sb.append("    slotType: ").append(toIndentedString(slotType)).append("\n");
    sb.append("    cohortTeacherId: ").append(toIndentedString(cohortTeacherId)).append("\n");
    sb.append("    weekday: ").append(toIndentedString(weekday)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    venue: ").append(toIndentedString(venue)).append("\n");
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

