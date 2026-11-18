// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ViolationEvent
 */

public class ViolationEvent {

  /**
   * Event.
   */
  public enum EventTypeEnum {
    TAB_SWITCH("tab_switch"),
    
    NO_FACE("no_face"),
    
    MULTIPLE_FACES("multiple_faces"),
    
    FOCUS_LOSS("focus_loss"),
    
    CAMERA_DISCONNECT("camera_disconnect");

    private final String value;

    EventTypeEnum(String value) {
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
    public static EventTypeEnum fromValue(String value) {
      for (EventTypeEnum b : EventTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private EventTypeEnum eventType;

  /**
   * Severity.
   */
  public enum SeverityEnum {
    VIOLATION("violation"),
    
    WARNING("warning");

    private final String value;

    SeverityEnum(String value) {
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
    public static SeverityEnum fromValue(String value) {
      for (SeverityEnum b : SeverityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private SeverityEnum severity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime occurredAt;

  public ViolationEvent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ViolationEvent(EventTypeEnum eventType, SeverityEnum severity, OffsetDateTime occurredAt) {
    this.eventType = eventType;
    this.severity = severity;
    this.occurredAt = occurredAt;
  }

  public ViolationEvent eventType(EventTypeEnum eventType) {
    this.eventType = eventType;
    return this;
  }

  /**
   * Event.
   * @return eventType
   */
  @NotNull 
  @JsonProperty("eventType")
  public EventTypeEnum getEventType() {
    return eventType;
  }

  @JsonProperty("eventType")
  public void setEventType(EventTypeEnum eventType) {
    this.eventType = eventType;
  }

  public ViolationEvent severity(SeverityEnum severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Severity.
   * @return severity
   */
  @NotNull 
  @JsonProperty("severity")
  public SeverityEnum getSeverity() {
    return severity;
  }

  @JsonProperty("severity")
  public void setSeverity(SeverityEnum severity) {
    this.severity = severity;
  }

  public ViolationEvent occurredAt(OffsetDateTime occurredAt) {
    this.occurredAt = occurredAt;
    return this;
  }

  /**
   * When.
   * @return occurredAt
   */
  @NotNull @Valid 
  @JsonProperty("occurredAt")
  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  @JsonProperty("occurredAt")
  public void setOccurredAt(OffsetDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ViolationEvent violationEvent = (ViolationEvent) o;
    return Objects.equals(this.eventType, violationEvent.eventType) &&
        Objects.equals(this.severity, violationEvent.severity) &&
        Objects.equals(this.occurredAt, violationEvent.occurredAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventType, severity, occurredAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ViolationEvent {\n");
    sb.append("    eventType: ").append(toIndentedString(eventType)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    occurredAt: ").append(toIndentedString(occurredAt)).append("\n");
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

