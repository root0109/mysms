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
 * Fields of ContentItemRating that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ContentItemRatingUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Boolean> isLike;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> rating;

  public ContentItemRatingUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public ContentItemRatingUpdate isLike(@Nullable Optional<Boolean> isLike) {
    this.isLike = isLike;
    return this;
  }

  /**
   * Liked.
   * @return isLike
   */
  
  @JsonProperty("isLike")
  public @Nullable Optional<Boolean> getIsLike() {
    return isLike;
  }

  @JsonProperty("isLike")
  public void setIsLike(@Nullable Optional<Boolean> isLike) {
    this.isLike = isLike;
  }

  public ContentItemRatingUpdate rating(@Nullable Optional<Integer> rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Rating 1-5.
   * @return rating
   */
  
  @JsonProperty("rating")
  public @Nullable Optional<Integer> getRating() {
    return rating;
  }

  @JsonProperty("rating")
  public void setRating(@Nullable Optional<Integer> rating) {
    this.rating = rating;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentItemRatingUpdate contentItemRatingUpdate = (ContentItemRatingUpdate) o;
    return Objects.equals(this.cohortId, contentItemRatingUpdate.cohortId) &&
        Objects.equals(this.isLike, contentItemRatingUpdate.isLike) &&
        Objects.equals(this.rating, contentItemRatingUpdate.rating);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, isLike, rating);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentItemRatingUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    isLike: ").append(toIndentedString(isLike)).append("\n");
    sb.append("    rating: ").append(toIndentedString(rating)).append("\n");
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

