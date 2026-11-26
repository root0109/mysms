// Owned API contract of transport.
package com.mysms.campus.transport.api.model;

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
 * Fields of RouteStop that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RouteStopUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sequence;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> stopName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> pickupTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> dropTime;

  public RouteStopUpdate sequence(@Nullable Optional<Integer> sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Order on the route.
   * @return sequence
   */
  
  @JsonProperty("sequence")
  public @Nullable Optional<Integer> getSequence() {
    return sequence;
  }

  @JsonProperty("sequence")
  public void setSequence(@Nullable Optional<Integer> sequence) {
    this.sequence = sequence;
  }

  public RouteStopUpdate stopName(@Nullable Optional<String> stopName) {
    this.stopName = stopName;
    return this;
  }

  /**
   * Stop name.
   * @return stopName
   */
  
  @JsonProperty("stopName")
  public @Nullable Optional<String> getStopName() {
    return stopName;
  }

  @JsonProperty("stopName")
  public void setStopName(@Nullable Optional<String> stopName) {
    this.stopName = stopName;
  }

  public RouteStopUpdate pickupTime(@Nullable Optional<String> pickupTime) {
    this.pickupTime = pickupTime;
    return this;
  }

  /**
   * Morning pickup time.
   * @return pickupTime
   */
  
  @JsonProperty("pickupTime")
  public @Nullable Optional<String> getPickupTime() {
    return pickupTime;
  }

  @JsonProperty("pickupTime")
  public void setPickupTime(@Nullable Optional<String> pickupTime) {
    this.pickupTime = pickupTime;
  }

  public RouteStopUpdate dropTime(@Nullable Optional<String> dropTime) {
    this.dropTime = dropTime;
    return this;
  }

  /**
   * Evening drop time.
   * @return dropTime
   */
  
  @JsonProperty("dropTime")
  public @Nullable Optional<String> getDropTime() {
    return dropTime;
  }

  @JsonProperty("dropTime")
  public void setDropTime(@Nullable Optional<String> dropTime) {
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

