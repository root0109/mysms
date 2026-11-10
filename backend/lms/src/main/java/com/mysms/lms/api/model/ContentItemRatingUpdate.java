// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of ContentItemRating that can be updated (partial update; see updateMask).
 */

public class ContentItemRatingUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isLike;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer rating;

  public ContentItemRatingUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public ContentItemRatingUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public ContentItemRatingUpdate isLike(@Nullable Boolean isLike) {
    this.isLike = isLike;
    return this;
  }

  /**
   * Liked.
   * @return isLike
   */
  
  @JsonProperty("isLike")
  public @Nullable Boolean getIsLike() {
    return isLike;
  }

  @JsonProperty("isLike")
  public void setIsLike(@Nullable Boolean isLike) {
    this.isLike = isLike;
  }

  public ContentItemRatingUpdate rating(@Nullable Integer rating) {
    this.rating = rating;
    return this;
  }

  /**
   * Rating 1-5.
   * @return rating
   */
  
  @JsonProperty("rating")
  public @Nullable Integer getRating() {
    return rating;
  }

  @JsonProperty("rating")
  public void setRating(@Nullable Integer rating) {
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
        Objects.equals(this.userId, contentItemRatingUpdate.userId) &&
        Objects.equals(this.isLike, contentItemRatingUpdate.isLike) &&
        Objects.equals(this.rating, contentItemRatingUpdate.rating);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, userId, isLike, rating);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentItemRatingUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

