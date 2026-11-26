// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * QuizAutosaveResult
 */

public class QuizAutosaveResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer savedCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer ignoredStale;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime serverTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime deadlineAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer remainingSeconds;

  public QuizAutosaveResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuizAutosaveResult(Integer savedCount, Integer ignoredStale, OffsetDateTime serverTime) {
    this.savedCount = savedCount;
    this.ignoredStale = ignoredStale;
    this.serverTime = serverTime;
  }

  public QuizAutosaveResult savedCount(Integer savedCount) {
    this.savedCount = savedCount;
    return this;
  }

  /**
   * Answers stored (stale ones ignored).
   * @return savedCount
   */
  @NotNull 
  @JsonProperty("savedCount")
  public Integer getSavedCount() {
    return savedCount;
  }

  @JsonProperty("savedCount")
  public void setSavedCount(Integer savedCount) {
    this.savedCount = savedCount;
  }

  public QuizAutosaveResult ignoredStale(Integer ignoredStale) {
    this.ignoredStale = ignoredStale;
    return this;
  }

  /**
   * Answers ignored because a newer one is stored.
   * @return ignoredStale
   */
  @NotNull 
  @JsonProperty("ignoredStale")
  public Integer getIgnoredStale() {
    return ignoredStale;
  }

  @JsonProperty("ignoredStale")
  public void setIgnoredStale(Integer ignoredStale) {
    this.ignoredStale = ignoredStale;
  }

  public QuizAutosaveResult serverTime(OffsetDateTime serverTime) {
    this.serverTime = serverTime;
    return this;
  }

  /**
   * Server clock, for timer correction.
   * @return serverTime
   */
  @NotNull @Valid 
  @JsonProperty("serverTime")
  public OffsetDateTime getServerTime() {
    return serverTime;
  }

  @JsonProperty("serverTime")
  public void setServerTime(OffsetDateTime serverTime) {
    this.serverTime = serverTime;
  }

  public QuizAutosaveResult deadlineAt(@Nullable OffsetDateTime deadlineAt) {
    this.deadlineAt = deadlineAt;
    return this;
  }

  /**
   * When the attempt closes (time limit / window).
   * @return deadlineAt
   */
  @Valid 
  @JsonProperty("deadlineAt")
  public @Nullable OffsetDateTime getDeadlineAt() {
    return deadlineAt;
  }

  @JsonProperty("deadlineAt")
  public void setDeadlineAt(@Nullable OffsetDateTime deadlineAt) {
    this.deadlineAt = deadlineAt;
  }

  public QuizAutosaveResult remainingSeconds(@Nullable Integer remainingSeconds) {
    this.remainingSeconds = remainingSeconds;
    return this;
  }

  /**
   * Time left on a timed attempt.
   * @return remainingSeconds
   */
  
  @JsonProperty("remainingSeconds")
  public @Nullable Integer getRemainingSeconds() {
    return remainingSeconds;
  }

  @JsonProperty("remainingSeconds")
  public void setRemainingSeconds(@Nullable Integer remainingSeconds) {
    this.remainingSeconds = remainingSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuizAutosaveResult quizAutosaveResult = (QuizAutosaveResult) o;
    return Objects.equals(this.savedCount, quizAutosaveResult.savedCount) &&
        Objects.equals(this.ignoredStale, quizAutosaveResult.ignoredStale) &&
        Objects.equals(this.serverTime, quizAutosaveResult.serverTime) &&
        Objects.equals(this.deadlineAt, quizAutosaveResult.deadlineAt) &&
        Objects.equals(this.remainingSeconds, quizAutosaveResult.remainingSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(savedCount, ignoredStale, serverTime, deadlineAt, remainingSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizAutosaveResult {\n");
    sb.append("    savedCount: ").append(toIndentedString(savedCount)).append("\n");
    sb.append("    ignoredStale: ").append(toIndentedString(ignoredStale)).append("\n");
    sb.append("    serverTime: ").append(toIndentedString(serverTime)).append("\n");
    sb.append("    deadlineAt: ").append(toIndentedString(deadlineAt)).append("\n");
    sb.append("    remainingSeconds: ").append(toIndentedString(remainingSeconds)).append("\n");
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

