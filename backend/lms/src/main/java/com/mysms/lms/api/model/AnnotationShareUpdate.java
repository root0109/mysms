// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AnnotationShare that can be updated (partial update; see updateMask).
 */

public class AnnotationShareUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID sharedWithUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime sharedAt;

  public AnnotationShareUpdate sharedWithUserId(@Nullable UUID sharedWithUserId) {
    this.sharedWithUserId = sharedWithUserId;
    return this;
  }

  /**
   * Recipient.
   * @return sharedWithUserId
   */
  @Valid 
  @JsonProperty("sharedWithUserId")
  public @Nullable UUID getSharedWithUserId() {
    return sharedWithUserId;
  }

  @JsonProperty("sharedWithUserId")
  public void setSharedWithUserId(@Nullable UUID sharedWithUserId) {
    this.sharedWithUserId = sharedWithUserId;
  }

  public AnnotationShareUpdate sharedAt(@Nullable OffsetDateTime sharedAt) {
    this.sharedAt = sharedAt;
    return this;
  }

  /**
   * When shared.
   * @return sharedAt
   */
  @Valid 
  @JsonProperty("sharedAt")
  public @Nullable OffsetDateTime getSharedAt() {
    return sharedAt;
  }

  @JsonProperty("sharedAt")
  public void setSharedAt(@Nullable OffsetDateTime sharedAt) {
    this.sharedAt = sharedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnnotationShareUpdate annotationShareUpdate = (AnnotationShareUpdate) o;
    return Objects.equals(this.sharedWithUserId, annotationShareUpdate.sharedWithUserId) &&
        Objects.equals(this.sharedAt, annotationShareUpdate.sharedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sharedWithUserId, sharedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnnotationShareUpdate {\n");
    sb.append("    sharedWithUserId: ").append(toIndentedString(sharedWithUserId)).append("\n");
    sb.append("    sharedAt: ").append(toIndentedString(sharedAt)).append("\n");
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

