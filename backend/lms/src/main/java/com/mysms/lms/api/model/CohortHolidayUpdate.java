// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of CohortHoliday that can be updated (partial update; see updateMask).
 */

public class CohortHolidayUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID holidayId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isIncluded;

  public CohortHolidayUpdate holidayId(@Nullable UUID holidayId) {
    this.holidayId = holidayId;
    return this;
  }

  /**
   * Institution holiday (NULL = cohort-only holiday).
   * @return holidayId
   */
  @Valid 
  @JsonProperty("holidayId")
  public @Nullable UUID getHolidayId() {
    return holidayId;
  }

  @JsonProperty("holidayId")
  public void setHolidayId(@Nullable UUID holidayId) {
    this.holidayId = holidayId;
  }

  public CohortHolidayUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public CohortHolidayUpdate startDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * First day.
   * @return startDate
   */
  @Valid 
  @JsonProperty("startDate")
  public @Nullable LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
  }

  public CohortHolidayUpdate endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Last day.
   * @return endDate
   */
  @Valid 
  @JsonProperty("endDate")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public CohortHolidayUpdate isIncluded(@Nullable Boolean isIncluded) {
    this.isIncluded = isIncluded;
    return this;
  }

  /**
   * Applied to this cohort (false = the cohort meets despite the institution holiday).
   * @return isIncluded
   */
  
  @JsonProperty("isIncluded")
  public @Nullable Boolean getIsIncluded() {
    return isIncluded;
  }

  @JsonProperty("isIncluded")
  public void setIsIncluded(@Nullable Boolean isIncluded) {
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

