// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of EvaluatorAvailability that can be updated (partial update; see updateMask).
 */

public class EvaluatorAvailabilityUpdate {

  /**
   * Day.
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

  public EvaluatorAvailabilityUpdate weekday(@Nullable WeekdayEnum weekday) {
    this.weekday = weekday;
    return this;
  }

  /**
   * Day.
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

  public EvaluatorAvailabilityUpdate startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * From.
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

  public EvaluatorAvailabilityUpdate endTime(@Nullable String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * To.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EvaluatorAvailabilityUpdate evaluatorAvailabilityUpdate = (EvaluatorAvailabilityUpdate) o;
    return Objects.equals(this.weekday, evaluatorAvailabilityUpdate.weekday) &&
        Objects.equals(this.startTime, evaluatorAvailabilityUpdate.startTime) &&
        Objects.equals(this.endTime, evaluatorAvailabilityUpdate.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(weekday, startTime, endTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EvaluatorAvailabilityUpdate {\n");
    sb.append("    weekday: ").append(toIndentedString(weekday)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
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

