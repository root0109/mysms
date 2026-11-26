// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * PeerPoints
 */

public class PeerPoints {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String points;

  public PeerPoints() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PeerPoints(UUID userId, String points) {
    this.userId = userId;
    this.points = points;
  }

  public PeerPoints userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Group member reviewed.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public PeerPoints points(String points) {
    this.points = points;
    return this;
  }

  /**
   * Points awarded.
   * @return points
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("points")
  public String getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(String points) {
    this.points = points;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PeerPoints peerPoints = (PeerPoints) o;
    return Objects.equals(this.userId, peerPoints.userId) &&
        Objects.equals(this.points, peerPoints.points);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, points);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PeerPoints {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
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

