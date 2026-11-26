// Owned API contract of transport.
package com.mysms.campus.transport.api.model;

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
 * Fields of StudentTransport that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StudentTransportUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> academicYearId;

  /**
   * Mode.
   */
  public enum TransportModeEnum {
    AUTO("auto"),
    
    CAR("car"),
    
    CITY_BUS("city_bus"),
    
    CYCLE("cycle"),
    
    RICKSHAW("rickshaw"),
    
    SCHOOL_BUS("school_bus"),
    
    TAXI("taxi"),
    
    WALKING("walking");

    private final String value;

    TransportModeEnum(String value) {
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
    public static TransportModeEnum fromValue(String value) {
      for (TransportModeEnum b : TransportModeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull TransportModeEnum> transportMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> routeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> routeStopId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> busPassIssued;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> details;

  public StudentTransportUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public StudentTransportUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public StudentTransportUpdate transportMode(@Nullable Optional<TransportModeEnum> transportMode) {
    this.transportMode = transportMode;
    return this;
  }

  /**
   * Mode.
   * @return transportMode
   */
  
  @JsonProperty("transportMode")
  public @Nullable Optional<TransportModeEnum> getTransportMode() {
    return transportMode;
  }

  @JsonProperty("transportMode")
  public void setTransportMode(@Nullable Optional<TransportModeEnum> transportMode) {
    this.transportMode = transportMode;
  }

  public StudentTransportUpdate routeId(@Nullable Optional<UUID> routeId) {
    this.routeId = routeId;
    return this;
  }

  /**
   * School route (school_bus).
   * @return routeId
   */
  
  @JsonProperty("routeId")
  public @Nullable Optional<UUID> getRouteId() {
    return routeId;
  }

  @JsonProperty("routeId")
  public void setRouteId(@Nullable Optional<UUID> routeId) {
    this.routeId = routeId;
  }

  public StudentTransportUpdate routeStopId(@Nullable Optional<UUID> routeStopId) {
    this.routeStopId = routeStopId;
    return this;
  }

  /**
   * Boarding stop.
   * @return routeStopId
   */
  
  @JsonProperty("routeStopId")
  public @Nullable Optional<UUID> getRouteStopId() {
    return routeStopId;
  }

  @JsonProperty("routeStopId")
  public void setRouteStopId(@Nullable Optional<UUID> routeStopId) {
    this.routeStopId = routeStopId;
  }

  public StudentTransportUpdate busPassIssued(@Nullable Optional<Boolean> busPassIssued) {
    this.busPassIssued = busPassIssued;
    return this;
  }

  /**
   * Bus pass issued.
   * @return busPassIssued
   */
  
  @JsonProperty("busPassIssued")
  public @Nullable Optional<Boolean> getBusPassIssued() {
    return busPassIssued;
  }

  @JsonProperty("busPassIssued")
  public void setBusPassIssued(@Nullable Optional<Boolean> busPassIssued) {
    this.busPassIssued = busPassIssued;
  }

  public StudentTransportUpdate details(@Nullable Optional<String> details) {
    this.details = details;
    return this;
  }

  /**
   * Notes (driver contact, private van...).
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable Optional<String> getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable Optional<String> details) {
    this.details = details;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentTransportUpdate studentTransportUpdate = (StudentTransportUpdate) o;
    return Objects.equals(this.studentId, studentTransportUpdate.studentId) &&
        Objects.equals(this.academicYearId, studentTransportUpdate.academicYearId) &&
        Objects.equals(this.transportMode, studentTransportUpdate.transportMode) &&
        Objects.equals(this.routeId, studentTransportUpdate.routeId) &&
        Objects.equals(this.routeStopId, studentTransportUpdate.routeStopId) &&
        Objects.equals(this.busPassIssued, studentTransportUpdate.busPassIssued) &&
        Objects.equals(this.details, studentTransportUpdate.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, academicYearId, transportMode, routeId, routeStopId, busPassIssued, details);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentTransportUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    transportMode: ").append(toIndentedString(transportMode)).append("\n");
    sb.append("    routeId: ").append(toIndentedString(routeId)).append("\n");
    sb.append("    routeStopId: ").append(toIndentedString(routeStopId)).append("\n");
    sb.append("    busPassIssued: ").append(toIndentedString(busPassIssued)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
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

