// Owned API contract of lms.
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
 * EvaluatorSlot
 */

public class EvaluatorSlot {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID evaluatorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String endTime;

  public EvaluatorSlot() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EvaluatorSlot(UUID evaluatorUserId, LocalDate date, String startTime, String endTime) {
    this.evaluatorUserId = evaluatorUserId;
    this.date = date;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public EvaluatorSlot evaluatorUserId(UUID evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
    return this;
  }

  /**
   * Evaluator.
   * @return evaluatorUserId
   */
  @NotNull @Valid 
  @JsonProperty("evaluatorUserId")
  public UUID getEvaluatorUserId() {
    return evaluatorUserId;
  }

  @JsonProperty("evaluatorUserId")
  public void setEvaluatorUserId(UUID evaluatorUserId) {
    this.evaluatorUserId = evaluatorUserId;
  }

  public EvaluatorSlot date(LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Date.
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

  public EvaluatorSlot startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start.
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public EvaluatorSlot endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * End.
   * @return endTime
   */
  @NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(String endTime) {
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
    EvaluatorSlot evaluatorSlot = (EvaluatorSlot) o;
    return Objects.equals(this.evaluatorUserId, evaluatorSlot.evaluatorUserId) &&
        Objects.equals(this.date, evaluatorSlot.date) &&
        Objects.equals(this.startTime, evaluatorSlot.startTime) &&
        Objects.equals(this.endTime, evaluatorSlot.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(evaluatorUserId, date, startTime, endTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EvaluatorSlot {\n");
    sb.append("    evaluatorUserId: ").append(toIndentedString(evaluatorUserId)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
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

