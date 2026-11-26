// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of EnrollmentRequest that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class EnrollmentRequestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> requestedAt;

  public EnrollmentRequestUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort the user was placed in.
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

  public EnrollmentRequestUpdate requestedAt(@Nullable Optional<OffsetDateTime> requestedAt) {
    this.requestedAt = requestedAt;
    return this;
  }

  /**
   * Request time.
   * @return requestedAt
   */
  
  @JsonProperty("requestedAt")
  public @Nullable Optional<OffsetDateTime> getRequestedAt() {
    return requestedAt;
  }

  @JsonProperty("requestedAt")
  public void setRequestedAt(@Nullable Optional<OffsetDateTime> requestedAt) {
    this.requestedAt = requestedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrollmentRequestUpdate enrollmentRequestUpdate = (EnrollmentRequestUpdate) o;
    return Objects.equals(this.cohortId, enrollmentRequestUpdate.cohortId) &&
        Objects.equals(this.requestedAt, enrollmentRequestUpdate.requestedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, requestedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrollmentRequestUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    requestedAt: ").append(toIndentedString(requestedAt)).append("\n");
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

