// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ScheduleLiveClassRequest
 */

public class ScheduleLiveClassRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate classDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID conferencingAccountId;

  /**
   * Recording.
   */
  public enum AutoRecordingEnum {
    NONE("none"),
    
    LOCAL("local"),
    
    CLOUD("cloud");

    private final String value;

    AutoRecordingEnum(String value) {
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
    public static AutoRecordingEnum fromValue(String value) {
      for (AutoRecordingEnum b : AutoRecordingEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AutoRecordingEnum autoRecording;

  public ScheduleLiveClassRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScheduleLiveClassRequest(UUID cohortId, String title, LocalDate classDate, String startTime, Integer durationMinutes, String timezone, UUID conferencingAccountId) {
    this.cohortId = cohortId;
    this.title = title;
    this.classDate = classDate;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.timezone = timezone;
    this.conferencingAccountId = conferencingAccountId;
  }

  public ScheduleLiveClassRequest cohortId(UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @NotNull @Valid 
  @JsonProperty("cohortId")
  public UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(UUID cohortId) {
    this.cohortId = cohortId;
  }

  public ScheduleLiveClassRequest title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public ScheduleLiveClassRequest description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ScheduleLiveClassRequest classDate(LocalDate classDate) {
    this.classDate = classDate;
    return this;
  }

  /**
   * Date.
   * @return classDate
   */
  @NotNull @Valid 
  @JsonProperty("classDate")
  public LocalDate getClassDate() {
    return classDate;
  }

  @JsonProperty("classDate")
  public void setClassDate(LocalDate classDate) {
    this.classDate = classDate;
  }

  public ScheduleLiveClassRequest startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Time.
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

  public ScheduleLiveClassRequest durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Minutes.
   * @return durationMinutes
   */
  @NotNull 
  @JsonProperty("durationMinutes")
  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  @JsonProperty("durationMinutes")
  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public ScheduleLiveClassRequest timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone.
   * @return timezone
   */
  @NotNull 
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public ScheduleLiveClassRequest conferencingAccountId(UUID conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
    return this;
  }

  /**
   * Account.
   * @return conferencingAccountId
   */
  @NotNull @Valid 
  @JsonProperty("conferencingAccountId")
  public UUID getConferencingAccountId() {
    return conferencingAccountId;
  }

  @JsonProperty("conferencingAccountId")
  public void setConferencingAccountId(UUID conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
  }

  public ScheduleLiveClassRequest autoRecording(@Nullable AutoRecordingEnum autoRecording) {
    this.autoRecording = autoRecording;
    return this;
  }

  /**
   * Recording.
   * @return autoRecording
   */
  
  @JsonProperty("autoRecording")
  public @Nullable AutoRecordingEnum getAutoRecording() {
    return autoRecording;
  }

  @JsonProperty("autoRecording")
  public void setAutoRecording(@Nullable AutoRecordingEnum autoRecording) {
    this.autoRecording = autoRecording;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScheduleLiveClassRequest scheduleLiveClassRequest = (ScheduleLiveClassRequest) o;
    return Objects.equals(this.cohortId, scheduleLiveClassRequest.cohortId) &&
        Objects.equals(this.title, scheduleLiveClassRequest.title) &&
        Objects.equals(this.description, scheduleLiveClassRequest.description) &&
        Objects.equals(this.classDate, scheduleLiveClassRequest.classDate) &&
        Objects.equals(this.startTime, scheduleLiveClassRequest.startTime) &&
        Objects.equals(this.durationMinutes, scheduleLiveClassRequest.durationMinutes) &&
        Objects.equals(this.timezone, scheduleLiveClassRequest.timezone) &&
        Objects.equals(this.conferencingAccountId, scheduleLiveClassRequest.conferencingAccountId) &&
        Objects.equals(this.autoRecording, scheduleLiveClassRequest.autoRecording);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, title, description, classDate, startTime, durationMinutes, timezone, conferencingAccountId, autoRecording);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScheduleLiveClassRequest {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    classDate: ").append(toIndentedString(classDate)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    conferencingAccountId: ").append(toIndentedString(conferencingAccountId)).append("\n");
    sb.append("    autoRecording: ").append(toIndentedString(autoRecording)).append("\n");
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

