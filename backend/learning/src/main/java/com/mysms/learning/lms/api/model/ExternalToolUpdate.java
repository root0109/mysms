// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ExternalTool that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ExternalToolUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isIncluded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPaid;

  public ExternalToolUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Tool name.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public ExternalToolUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

  public ExternalToolUpdate isIncluded(@Nullable Optional<Boolean> isIncluded) {
    this.isIncluded = isIncluded;
    return this;
  }

  /**
   * Included in the subscription (no extra licence).
   * @return isIncluded
   */
  
  @JsonProperty("isIncluded")
  public @Nullable Optional<Boolean> getIsIncluded() {
    return isIncluded;
  }

  @JsonProperty("isIncluded")
  public void setIsIncluded(@Nullable Optional<Boolean> isIncluded) {
    this.isIncluded = isIncluded;
  }

  public ExternalToolUpdate isPaid(@Nullable Optional<Boolean> isPaid) {
    this.isPaid = isPaid;
    return this;
  }

  /**
   * Requires a paid licence.
   * @return isPaid
   */
  
  @JsonProperty("isPaid")
  public @Nullable Optional<Boolean> getIsPaid() {
    return isPaid;
  }

  @JsonProperty("isPaid")
  public void setIsPaid(@Nullable Optional<Boolean> isPaid) {
    this.isPaid = isPaid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalToolUpdate externalToolUpdate = (ExternalToolUpdate) o;
    return Objects.equals(this.title, externalToolUpdate.title) &&
        Objects.equals(this.description, externalToolUpdate.description) &&
        Objects.equals(this.isIncluded, externalToolUpdate.isIncluded) &&
        Objects.equals(this.isPaid, externalToolUpdate.isPaid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description, isIncluded, isPaid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalToolUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isIncluded: ").append(toIndentedString(isIncluded)).append("\n");
    sb.append("    isPaid: ").append(toIndentedString(isPaid)).append("\n");
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

