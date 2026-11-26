// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CohortSession that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortSessionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> startsAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> endsAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> venue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isCancelled;

  public CohortSessionUpdate startsAt(@Nullable Optional<OffsetDateTime> startsAt) {
    this.startsAt = startsAt;
    return this;
  }

  /**
   * Start.
   * @return startsAt
   */
  
  @JsonProperty("startsAt")
  public @Nullable Optional<OffsetDateTime> getStartsAt() {
    return startsAt;
  }

  @JsonProperty("startsAt")
  public void setStartsAt(@Nullable Optional<OffsetDateTime> startsAt) {
    this.startsAt = startsAt;
  }

  public CohortSessionUpdate endsAt(@Nullable Optional<OffsetDateTime> endsAt) {
    this.endsAt = endsAt;
    return this;
  }

  /**
   * End.
   * @return endsAt
   */
  
  @JsonProperty("endsAt")
  public @Nullable Optional<OffsetDateTime> getEndsAt() {
    return endsAt;
  }

  @JsonProperty("endsAt")
  public void setEndsAt(@Nullable Optional<OffsetDateTime> endsAt) {
    this.endsAt = endsAt;
  }

  public CohortSessionUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Topic / notes.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public CohortSessionUpdate venue(@Nullable Optional<String> venue) {
    this.venue = venue;
    return this;
  }

  /**
   * Room / location / online link.
   * @return venue
   */
  
  @JsonProperty("venue")
  public @Nullable Optional<String> getVenue() {
    return venue;
  }

  @JsonProperty("venue")
  public void setVenue(@Nullable Optional<String> venue) {
    this.venue = venue;
  }

  public CohortSessionUpdate isCancelled(@Nullable Optional<Boolean> isCancelled) {
    this.isCancelled = isCancelled;
    return this;
  }

  /**
   * Cancelled.
   * @return isCancelled
   */
  
  @JsonProperty("isCancelled")
  public @Nullable Optional<Boolean> getIsCancelled() {
    return isCancelled;
  }

  @JsonProperty("isCancelled")
  public void setIsCancelled(@Nullable Optional<Boolean> isCancelled) {
    this.isCancelled = isCancelled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortSessionUpdate cohortSessionUpdate = (CohortSessionUpdate) o;
    return Objects.equals(this.startsAt, cohortSessionUpdate.startsAt) &&
        Objects.equals(this.endsAt, cohortSessionUpdate.endsAt) &&
        Objects.equals(this.description, cohortSessionUpdate.description) &&
        Objects.equals(this.venue, cohortSessionUpdate.venue) &&
        Objects.equals(this.isCancelled, cohortSessionUpdate.isCancelled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startsAt, endsAt, description, venue, isCancelled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortSessionUpdate {\n");
    sb.append("    startsAt: ").append(toIndentedString(startsAt)).append("\n");
    sb.append("    endsAt: ").append(toIndentedString(endsAt)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    venue: ").append(toIndentedString(venue)).append("\n");
    sb.append("    isCancelled: ").append(toIndentedString(isCancelled)).append("\n");
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

