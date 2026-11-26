// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.PeerPoints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SubmitPeerReviewRequest
 */

public class SubmitPeerReviewRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid PeerPoints> reviews = new ArrayList<>();

  public SubmitPeerReviewRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SubmitPeerReviewRequest(List<@Valid PeerPoints> reviews) {
    this.reviews = reviews;
  }

  public SubmitPeerReviewRequest reviews(List<@Valid PeerPoints> reviews) {
    this.reviews = reviews;
    return this;
  }

  public SubmitPeerReviewRequest addReviewsItem(PeerPoints reviewsItem) {
    if (this.reviews == null) {
      this.reviews = new ArrayList<>();
    }
    this.reviews.add(reviewsItem);
    return this;
  }

  /**
   * Points the caller awards to each other member.
   * @return reviews
   */
  @NotNull 
  @JsonProperty("reviews")
  public List<@Valid PeerPoints> getReviews() {
    return reviews;
  }

  @JsonProperty("reviews")
  public void setReviews(List<@Valid PeerPoints> reviews) {
    this.reviews = reviews;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubmitPeerReviewRequest submitPeerReviewRequest = (SubmitPeerReviewRequest) o;
    return Objects.equals(this.reviews, submitPeerReviewRequest.reviews);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reviews);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubmitPeerReviewRequest {\n");
    sb.append("    reviews: ").append(toIndentedString(reviews)).append("\n");
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

