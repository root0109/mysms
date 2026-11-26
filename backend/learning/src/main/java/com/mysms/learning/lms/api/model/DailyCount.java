// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * DailyCount
 */

public class DailyCount {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer count;

  public DailyCount() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DailyCount(LocalDate date, Integer count) {
    this.date = date;
    this.count = count;
  }

  public DailyCount date(LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Day.
   * @return date
   */
  @NotNull @Valid 
  @JsonProperty("date")
  public LocalDate getDate() {
    return date;
  }

  @JsonProperty("date")
  public void setDate(LocalDate date) {
    this.date = date;
  }

  public DailyCount count(Integer count) {
    this.count = count;
    return this;
  }

  /**
   * Count.
   * @return count
   */
  @NotNull 
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }

  @JsonProperty("count")
  public void setCount(Integer count) {
    this.count = count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DailyCount dailyCount = (DailyCount) o;
    return Objects.equals(this.date, dailyCount.date) &&
        Objects.equals(this.count, dailyCount.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DailyCount {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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

