// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Availability
 */

public class Availability {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean available;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String suggestion;

  public Availability() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Availability(Boolean available) {
    this.available = available;
  }

  public Availability available(Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * The value is free.
   * @return available
   */
  @NotNull 
  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(Boolean available) {
    this.available = available;
  }

  public Availability suggestion(@Nullable String suggestion) {
    this.suggestion = suggestion;
    return this;
  }

  /**
   * A free alternative when not available.
   * @return suggestion
   */
  
  @JsonProperty("suggestion")
  public @Nullable String getSuggestion() {
    return suggestion;
  }

  @JsonProperty("suggestion")
  public void setSuggestion(@Nullable String suggestion) {
    this.suggestion = suggestion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Availability availability = (Availability) o;
    return Objects.equals(this.available, availability.available) &&
        Objects.equals(this.suggestion, availability.suggestion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(available, suggestion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Availability {\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
    sb.append("    suggestion: ").append(toIndentedString(suggestion)).append("\n");
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

