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
 * Fields of CourseReview that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CourseReviewUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> rating;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> review;

  public CourseReviewUpdate rating(@Nullable Optional<String> rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Rating 0-5.
   * @return rating
   */
  
  @JsonProperty("rating")
  public @Nullable Optional<String> getRating() {
    return rating;
  }

  @JsonProperty("rating")
  public void setRating(@Nullable Optional<String> rating) {
    this.rating = rating;
  }

  public CourseReviewUpdate review(@Nullable Optional<String> review) {
    this.review = review;
    return this;
  }

  /**
   * Review text.
   * @return review
   */
  
  @JsonProperty("review")
  public @Nullable Optional<String> getReview() {
    return review;
  }

  @JsonProperty("review")
  public void setReview(@Nullable Optional<String> review) {
    this.review = review;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseReviewUpdate courseReviewUpdate = (CourseReviewUpdate) o;
    return Objects.equals(this.rating, courseReviewUpdate.rating) &&
        Objects.equals(this.review, courseReviewUpdate.review);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rating, review);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseReviewUpdate {\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
    sb.append("    review: ").append(toIndentedString(review)).append("\n");
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

