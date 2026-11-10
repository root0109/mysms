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
 * Fields of RelatedCourse that can be updated (partial update; see updateMask).
 */

public class RelatedCourseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID relatedCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID authorProfileId;

  public RelatedCourseUpdate relatedCourseId(@Nullable UUID relatedCourseId) {
    this.relatedCourseId = relatedCourseId;
    return this;
  }

  /**
   * Recommended course.
   * @return relatedCourseId
   */
  @Valid 
  @JsonProperty("relatedCourseId")
  public @Nullable UUID getRelatedCourseId() {
    return relatedCourseId;
  }

  @JsonProperty("relatedCourseId")
  public void setRelatedCourseId(@Nullable UUID relatedCourseId) {
    this.relatedCourseId = relatedCourseId;
  }

  public RelatedCourseUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public RelatedCourseUpdate authorProfileId(@Nullable UUID authorProfileId) {
    this.authorProfileId = authorProfileId;
    return this;
  }

  /**
   * Author highlighted on the related-course card.
   * @return authorProfileId
   */
  @Valid 
  @JsonProperty("authorProfileId")
  public @Nullable UUID getAuthorProfileId() {
    return authorProfileId;
  }

  @JsonProperty("authorProfileId")
  public void setAuthorProfileId(@Nullable UUID authorProfileId) {
    this.authorProfileId = authorProfileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelatedCourseUpdate relatedCourseUpdate = (RelatedCourseUpdate) o;
    return Objects.equals(this.relatedCourseId, relatedCourseUpdate.relatedCourseId) &&
        Objects.equals(this.sortOrder, relatedCourseUpdate.sortOrder) &&
        Objects.equals(this.authorProfileId, relatedCourseUpdate.authorProfileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relatedCourseId, sortOrder, authorProfileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RelatedCourseUpdate {\n");
    sb.append("    relatedCourseId: ").append(toIndentedString(relatedCourseId)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    authorProfileId: ").append(toIndentedString(authorProfileId)).append("\n");
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

