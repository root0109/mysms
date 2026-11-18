// Owned API contract of lms.
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
 * Fields of CourseMentor that can be updated (partial update; see updateMask).
 */

public class CourseMentorUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  public CourseMentorUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Mentor.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseMentorUpdate courseMentorUpdate = (CourseMentorUpdate) o;
    return Objects.equals(this.userId, courseMentorUpdate.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseMentorUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

