// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Evaluator that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class EvaluatorUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> unavailableFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> unavailableTo;

  public EvaluatorUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Evaluator account.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public EvaluatorUpdate unavailableFrom(@Nullable Optional<LocalDate> unavailableFrom) {
    this.unavailableFrom = unavailableFrom;
    return this;
  }

  /**
   * Start of unavailability.
   * @return unavailableFrom
   */
  
  @JsonProperty("unavailableFrom")
  public @Nullable Optional<LocalDate> getUnavailableFrom() {
    return unavailableFrom;
  }

  @JsonProperty("unavailableFrom")
  public void setUnavailableFrom(@Nullable Optional<LocalDate> unavailableFrom) {
    this.unavailableFrom = unavailableFrom;
  }

  public EvaluatorUpdate unavailableTo(@Nullable Optional<LocalDate> unavailableTo) {
    this.unavailableTo = unavailableTo;
    return this;
  }

  /**
   * End of unavailability.
   * @return unavailableTo
   */
  
  @JsonProperty("unavailableTo")
  public @Nullable Optional<LocalDate> getUnavailableTo() {
    return unavailableTo;
  }

  @JsonProperty("unavailableTo")
  public void setUnavailableTo(@Nullable Optional<LocalDate> unavailableTo) {
    this.unavailableTo = unavailableTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EvaluatorUpdate evaluatorUpdate = (EvaluatorUpdate) o;
    return Objects.equals(this.userId, evaluatorUpdate.userId) &&
        Objects.equals(this.unavailableFrom, evaluatorUpdate.unavailableFrom) &&
        Objects.equals(this.unavailableTo, evaluatorUpdate.unavailableTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, unavailableFrom, unavailableTo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EvaluatorUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    unavailableFrom: ").append(toIndentedString(unavailableFrom)).append("\n");
    sb.append("    unavailableTo: ").append(toIndentedString(unavailableTo)).append("\n");
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

