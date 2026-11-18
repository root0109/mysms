// Owned API contract of transport.
package com.mysms.transport.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Route that can be updated (partial update; see updateMask).
 */

public class RouteUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String vehicleNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public RouteUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public RouteUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year the route runs.
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

  public RouteUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Route name.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public RouteUpdate vehicleNumber(@Nullable String vehicleNumber) {
    this.vehicleNumber = vehicleNumber;
    return this;
  }

  /**
   * Vehicle registration number.
   * @return vehicleNumber
   */
  @Size(max = 30) 
  @JsonProperty("vehicleNumber")
  public @Nullable String getVehicleNumber() {
    return vehicleNumber;
  }

  @JsonProperty("vehicleNumber")
  public void setVehicleNumber(@Nullable String vehicleNumber) {
    this.vehicleNumber = vehicleNumber;
  }

  public RouteUpdate startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Departure time.
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

  public RouteUpdate endTime(@Nullable String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Arrival time.
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

  public RouteUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether running.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RouteUpdate routeUpdate = (RouteUpdate) o;
    return Objects.equals(this.institutionId, routeUpdate.institutionId) &&
        Objects.equals(this.academicYearId, routeUpdate.academicYearId) &&
        Objects.equals(this.name, routeUpdate.name) &&
        Objects.equals(this.vehicleNumber, routeUpdate.vehicleNumber) &&
        Objects.equals(this.startTime, routeUpdate.startTime) &&
        Objects.equals(this.endTime, routeUpdate.endTime) &&
        Objects.equals(this.isActive, routeUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, name, vehicleNumber, startTime, endTime, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RouteUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    vehicleNumber: ").append(toIndentedString(vehicleNumber)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

