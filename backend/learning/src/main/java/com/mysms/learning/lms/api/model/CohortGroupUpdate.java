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
 * Fields of CohortGroup that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortGroupUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isDisabled;

  public CohortGroupUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Group name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public CohortGroupUpdate description(@Nullable Optional<String> description) {
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

  public CohortGroupUpdate isDisabled(@Nullable Optional<Boolean> isDisabled) {
    this.isDisabled = isDisabled;
    return this;
  }

  /**
   * Disabled.
   * @return isDisabled
   */
  
  @JsonProperty("isDisabled")
  public @Nullable Optional<Boolean> getIsDisabled() {
    return isDisabled;
  }

  @JsonProperty("isDisabled")
  public void setIsDisabled(@Nullable Optional<Boolean> isDisabled) {
    this.isDisabled = isDisabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortGroupUpdate cohortGroupUpdate = (CohortGroupUpdate) o;
    return Objects.equals(this.name, cohortGroupUpdate.name) &&
        Objects.equals(this.description, cohortGroupUpdate.description) &&
        Objects.equals(this.isDisabled, cohortGroupUpdate.isDisabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, isDisabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortGroupUpdate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isDisabled: ").append(toIndentedString(isDisabled)).append("\n");
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

