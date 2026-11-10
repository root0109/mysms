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
 * Fields of CourseTestimonial that can be updated (partial update; see updateMask).
 */

public class CourseTestimonialUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String quote;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String currentPosition;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public CourseTestimonialUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Person / headline.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public CourseTestimonialUpdate quote(@Nullable String quote) {
    this.quote = quote;
    return this;
  }

  /**
   * Testimonial text.
   * @return quote
   */
  
  @JsonProperty("quote")
  public @Nullable String getQuote() {
    return quote;
  }

  @JsonProperty("quote")
  public void setQuote(@Nullable String quote) {
    this.quote = quote;
  }

  public CourseTestimonialUpdate photoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Photo.
   * @return photoFileId
   */
  @Valid 
  @JsonProperty("photoFileId")
  public @Nullable UUID getPhotoFileId() {
    return photoFileId;
  }

  @JsonProperty("photoFileId")
  public void setPhotoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
  }

  public CourseTestimonialUpdate currentPosition(@Nullable String currentPosition) {
    this.currentPosition = currentPosition;
    return this;
  }

  /**
   * Person's current position.
   * @return currentPosition
   */
  @Size(max = 255) 
  @JsonProperty("currentPosition")
  public @Nullable String getCurrentPosition() {
    return currentPosition;
  }

  @JsonProperty("currentPosition")
  public void setCurrentPosition(@Nullable String currentPosition) {
    this.currentPosition = currentPosition;
  }

  public CourseTestimonialUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseTestimonialUpdate courseTestimonialUpdate = (CourseTestimonialUpdate) o;
    return Objects.equals(this.title, courseTestimonialUpdate.title) &&
        Objects.equals(this.quote, courseTestimonialUpdate.quote) &&
        Objects.equals(this.photoFileId, courseTestimonialUpdate.photoFileId) &&
        Objects.equals(this.currentPosition, courseTestimonialUpdate.currentPosition) &&
        Objects.equals(this.sortOrder, courseTestimonialUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, quote, photoFileId, currentPosition, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseTestimonialUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    quote: ").append(toIndentedString(quote)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
    sb.append("    currentPosition: ").append(toIndentedString(currentPosition)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

