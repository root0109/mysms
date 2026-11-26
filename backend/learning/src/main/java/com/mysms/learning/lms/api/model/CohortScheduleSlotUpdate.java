// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of CohortScheduleSlot that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
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
  private @Nullable Optional<@NotNull SlotTypeEnum> slotType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortTeacherId;

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
  private @Nullable Optional<@NotNull WeekdayEnum> weekday;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> venue;

  public CohortScheduleSlotUpdate slotType(@Nullable Optional<SlotTypeEnum> slotType) {
    this.slotType = slotType;
    return this;
  }

  /**
   * Kind of slot.
   * @return slotType
   */
  
  @JsonProperty("slotType")
  public @Nullable Optional<SlotTypeEnum> getSlotType() {
    return slotType;
  }

  @JsonProperty("slotType")
  public void setSlotType(@Nullable Optional<SlotTypeEnum> slotType) {
    this.slotType = slotType;
  }

  public CohortScheduleSlotUpdate cohortTeacherId(@Nullable Optional<UUID> cohortTeacherId) {
    this.cohortTeacherId = cohortTeacherId;
    return this;
  }

  /**
   * Teacher (office hours).
   * @return cohortTeacherId
   */
  
  @JsonProperty("cohortTeacherId")
  public @Nullable Optional<UUID> getCohortTeacherId() {
    return cohortTeacherId;
  }

  @JsonProperty("cohortTeacherId")
  public void setCohortTeacherId(@Nullable Optional<UUID> cohortTeacherId) {
    this.cohortTeacherId = cohortTeacherId;
  }

  public CohortScheduleSlotUpdate weekday(@Nullable Optional<WeekdayEnum> weekday) {
    this.weekday = weekday;
    return this;
  }

  /**
   * Day of week.
   * @return weekday
   */
  
  @JsonProperty("weekday")
  public @Nullable Optional<WeekdayEnum> getWeekday() {
    return weekday;
  }

  @JsonProperty("weekday")
  public void setWeekday(@Nullable Optional<WeekdayEnum> weekday) {
    this.weekday = weekday;
  }

  public CohortScheduleSlotUpdate startTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start.
   * @return startTime
   */
  
  @JsonProperty("startTime")
  public @Nullable Optional<String> getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
  }

  public CohortScheduleSlotUpdate endTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * End.
   * @return endTime
   */
  
  @JsonProperty("endTime")
  public @Nullable Optional<String> getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
  }

  public CohortScheduleSlotUpdate venue(@Nullable Optional<String> venue) {
    this.venue = venue;
    return this;
  }

  /**
   * Room / location / online link.
   * @return venue
   */
  
  @JsonProperty("venue")
  public @Nullable Optional<String> getVenue() {
    return venue;
  }

  @JsonProperty("venue")
  public void setVenue(@Nullable Optional<String> venue) {
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

