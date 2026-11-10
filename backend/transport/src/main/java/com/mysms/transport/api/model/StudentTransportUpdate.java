// Owned API contract of transport (JAVA-CODING-RULES §7); first materialized from api/openapi/transport.yaml on 2026-11-10.
package com.mysms.transport.api.model;

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
 * Fields of StudentTransport that can be updated (partial update; see updateMask).
 */

public class StudentTransportUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

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
  private @Nullable TransportModeEnum transportMode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID routeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID routeStopId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean busPassIssued;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String details;

  public StudentTransportUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public StudentTransportUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public StudentTransportUpdate transportMode(@Nullable TransportModeEnum transportMode) {
    this.transportMode = transportMode;
    return this;
  }

  /**
   * Mode.
   * @return transportMode
   */
  
  @JsonProperty("transportMode")
  public @Nullable TransportModeEnum getTransportMode() {
    return transportMode;
  }

  @JsonProperty("transportMode")
  public void setTransportMode(@Nullable TransportModeEnum transportMode) {
    this.transportMode = transportMode;
  }

  public StudentTransportUpdate routeId(@Nullable UUID routeId) {
    this.routeId = routeId;
    return this;
  }

  /**
   * School route (school_bus).
   * @return routeId
   */
  @Valid 
  @JsonProperty("routeId")
  public @Nullable UUID getRouteId() {
    return routeId;
  }

  @JsonProperty("routeId")
  public void setRouteId(@Nullable UUID routeId) {
    this.routeId = routeId;
  }

  public StudentTransportUpdate routeStopId(@Nullable UUID routeStopId) {
    this.routeStopId = routeStopId;
    return this;
  }

  /**
   * Boarding stop.
   * @return routeStopId
   */
  @Valid 
  @JsonProperty("routeStopId")
  public @Nullable UUID getRouteStopId() {
    return routeStopId;
  }

  @JsonProperty("routeStopId")
  public void setRouteStopId(@Nullable UUID routeStopId) {
    this.routeStopId = routeStopId;
  }

  public StudentTransportUpdate busPassIssued(@Nullable Boolean busPassIssued) {
    this.busPassIssued = busPassIssued;
    return this;
  }

  /**
   * Bus pass issued.
   * @return busPassIssued
   */
  
  @JsonProperty("busPassIssued")
  public @Nullable Boolean getBusPassIssued() {
    return busPassIssued;
  }

  @JsonProperty("busPassIssued")
  public void setBusPassIssued(@Nullable Boolean busPassIssued) {
    this.busPassIssued = busPassIssued;
  }

  public StudentTransportUpdate details(@Nullable String details) {
    this.details = details;
    return this;
  }

  /**
   * Notes (driver contact, private van...).
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable String getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable String details) {
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

