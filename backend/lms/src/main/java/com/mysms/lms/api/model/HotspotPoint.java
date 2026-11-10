// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * HotspotPoint
 */

public class HotspotPoint {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String x;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String y;

  public HotspotPoint() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public HotspotPoint(String x, String y) {
    this.x = x;
    this.y = y;
  }

  public HotspotPoint x(String x) {
    this.x = x;
    return this;
  }

  /**
   * X coordinate (image pixels).
   * @return x
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("x")
  public String getX() {
    return x;
  }

  @JsonProperty("x")
  public void setX(String x) {
    this.x = x;
  }

  public HotspotPoint y(String y) {
    this.y = y;
    return this;
  }

  /**
   * Y coordinate (image pixels).
   * @return y
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("y")
  public String getY() {
    return y;
  }

  @JsonProperty("y")
  public void setY(String y) {
    this.y = y;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HotspotPoint hotspotPoint = (HotspotPoint) o;
    return Objects.equals(this.x, hotspotPoint.x) &&
        Objects.equals(this.y, hotspotPoint.y);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HotspotPoint {\n");
    sb.append("    x: ").append(toIndentedString(x)).append("\n");
    sb.append("    y: ").append(toIndentedString(y)).append("\n");
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

