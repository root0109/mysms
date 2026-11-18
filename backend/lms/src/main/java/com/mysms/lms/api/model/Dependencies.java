// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Dependencies
 */

public class Dependencies {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean canDelete;

  private @Nullable Object blocking = null;

  public Dependencies() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Dependencies(Boolean canDelete) {
    this.canDelete = canDelete;
  }

  public Dependencies canDelete(Boolean canDelete) {
    this.canDelete = canDelete;
    return this;
  }

  /**
   * No blocking dependants.
   * @return canDelete
   */
  @NotNull 
  @JsonProperty("canDelete")
  public Boolean getCanDelete() {
    return canDelete;
  }

  @JsonProperty("canDelete")
  public void setCanDelete(Boolean canDelete) {
    this.canDelete = canDelete;
  }

  public Dependencies blocking(@Nullable Object blocking) {
    this.blocking = blocking;
    return this;
  }

  /**
   * Counts of dependent records per type (cohorts, versions, catalogue entries...).
   * @return blocking
   */
  
  @JsonProperty("blocking")
  public @Nullable Object getBlocking() {
    return blocking;
  }

  @JsonProperty("blocking")
  public void setBlocking(@Nullable Object blocking) {
    this.blocking = blocking;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Dependencies dependencies = (Dependencies) o;
    return Objects.equals(this.canDelete, dependencies.canDelete) &&
        Objects.equals(this.blocking, dependencies.blocking);
  }

  @Override
  public int hashCode() {
    return Objects.hash(canDelete, blocking);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Dependencies {\n");
    sb.append("    canDelete: ").append(toIndentedString(canDelete)).append("\n");
    sb.append("    blocking: ").append(toIndentedString(blocking)).append("\n");
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

