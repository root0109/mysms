// Owned API contract of transport (JAVA-CODING-RULES §7); first materialized from api/openapi/transport.yaml on 2026-11-10.
package com.mysms.transport.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of RouteStop that can be updated (partial update; see updateMask).
 */

public class RouteStopUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sequence;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String stopName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String pickupTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String dropTime;

  public RouteStopUpdate sequence(@Nullable Integer sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Order on the route.
   * @return sequence
   */
  
  @JsonProperty("sequence")
  public @Nullable Integer getSequence() {
    return sequence;
  }

  @JsonProperty("sequence")
  public void setSequence(@Nullable Integer sequence) {
    this.sequence = sequence;
  }

  public RouteStopUpdate stopName(@Nullable String stopName) {
    this.stopName = stopName;
    return this;
  }

  /**
   * Stop name.
   * @return stopName
   */
  @Size(max = 150) 
  @JsonProperty("stopName")
  public @Nullable String getStopName() {
    return stopName;
  }

  @JsonProperty("stopName")
  public void setStopName(@Nullable String stopName) {
    this.stopName = stopName;
  }

  public RouteStopUpdate pickupTime(@Nullable String pickupTime) {
    this.pickupTime = pickupTime;
    return this;
  }

  /**
   * Morning pickup time.
   * @return pickupTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("pickupTime")
  public @Nullable String getPickupTime() {
    return pickupTime;
  }

  @JsonProperty("pickupTime")
  public void setPickupTime(@Nullable String pickupTime) {
    this.pickupTime = pickupTime;
  }

  public RouteStopUpdate dropTime(@Nullable String dropTime) {
    this.dropTime = dropTime;
    return this;
  }

  /**
   * Evening drop time.
   * @return dropTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("dropTime")
  public @Nullable String getDropTime() {
    return dropTime;
  }

  @JsonProperty("dropTime")
  public void setDropTime(@Nullable String dropTime) {
    this.dropTime = dropTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RouteStopUpdate routeStopUpdate = (RouteStopUpdate) o;
    return Objects.equals(this.sequence, routeStopUpdate.sequence) &&
        Objects.equals(this.stopName, routeStopUpdate.stopName) &&
        Objects.equals(this.pickupTime, routeStopUpdate.pickupTime) &&
        Objects.equals(this.dropTime, routeStopUpdate.dropTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sequence, stopName, pickupTime, dropTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RouteStopUpdate {\n");
    sb.append("    sequence: ").append(toIndentedString(sequence)).append("\n");
    sb.append("    stopName: ").append(toIndentedString(stopName)).append("\n");
    sb.append("    pickupTime: ").append(toIndentedString(pickupTime)).append("\n");
    sb.append("    dropTime: ").append(toIndentedString(dropTime)).append("\n");
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

