// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of CohortHoliday that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortHolidayUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> holidayId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isIncluded;

  public CohortHolidayUpdate holidayId(@Nullable Optional<UUID> holidayId) {
    this.holidayId = holidayId;
    return this;
  }

  /**
   * Institution holiday (NULL = cohort-only holiday).
   * @return holidayId
   */
  
  @JsonProperty("holidayId")
  public @Nullable Optional<UUID> getHolidayId() {
    return holidayId;
  }

  @JsonProperty("holidayId")
  public void setHolidayId(@Nullable Optional<UUID> holidayId) {
    this.holidayId = holidayId;
  }

  public CohortHolidayUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public CohortHolidayUpdate startDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * First day.
   * @return startDate
   */
  
  @JsonProperty("startDate")
  public @Nullable Optional<LocalDate> getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
  }

  public CohortHolidayUpdate endDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Last day.
   * @return endDate
   */
  
  @JsonProperty("endDate")
  public @Nullable Optional<LocalDate> getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
  }

  public CohortHolidayUpdate isIncluded(@Nullable Optional<Boolean> isIncluded) {
    this.isIncluded = isIncluded;
    return this;
  }

  /**
   * Applied to this cohort (false = the cohort meets despite the institution holiday).
   * @return isIncluded
   */
  
  @JsonProperty("isIncluded")
  public @Nullable Optional<Boolean> getIsIncluded() {
    return isIncluded;
  }

  @JsonProperty("isIncluded")
  public void setIsIncluded(@Nullable Optional<Boolean> isIncluded) {
    this.isIncluded = isIncluded;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortHolidayUpdate cohortHolidayUpdate = (CohortHolidayUpdate) o;
    return Objects.equals(this.holidayId, cohortHolidayUpdate.holidayId) &&
        Objects.equals(this.title, cohortHolidayUpdate.title) &&
        Objects.equals(this.startDate, cohortHolidayUpdate.startDate) &&
        Objects.equals(this.endDate, cohortHolidayUpdate.endDate) &&
        Objects.equals(this.isIncluded, cohortHolidayUpdate.isIncluded);
  }

  @Override
  public int hashCode() {
    return Objects.hash(holidayId, title, startDate, endDate, isIncluded);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortHolidayUpdate {\n");
    sb.append("    holidayId: ").append(toIndentedString(holidayId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    isIncluded: ").append(toIndentedString(isIncluded)).append("\n");
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

