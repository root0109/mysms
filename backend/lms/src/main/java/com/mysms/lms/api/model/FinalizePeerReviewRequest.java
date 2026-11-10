// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.PeerPoints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * FinalizePeerReviewRequest
 */

public class FinalizePeerReviewRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid PeerPoints> points = new ArrayList<>();

  public FinalizePeerReviewRequest points(List<@Valid PeerPoints> points) {
    this.points = points;
    return this;
  }

  public FinalizePeerReviewRequest addPointsItem(PeerPoints pointsItem) {
    if (this.points == null) {
      this.points = new ArrayList<>();
    }
    this.points.add(pointsItem);
    return this;
  }

  /**
   * Teacher overrides of the peer points per member (default: average of the reviews).
   * @return points
   */
  
  @JsonProperty("points")
  public List<@Valid PeerPoints> getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(List<@Valid PeerPoints> points) {
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
    FinalizePeerReviewRequest finalizePeerReviewRequest = (FinalizePeerReviewRequest) o;
    return Objects.equals(this.points, finalizePeerReviewRequest.points);
  }

  @Override
  public int hashCode() {
    return Objects.hash(points);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FinalizePeerReviewRequest {\n");
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

