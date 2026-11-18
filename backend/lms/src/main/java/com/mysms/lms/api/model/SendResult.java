// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.Operation;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SendResult
 */

public class SendResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer queued;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer skipped;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Operation operation;

  public SendResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SendResult(Integer queued, Integer skipped) {
    this.queued = queued;
    this.skipped = skipped;
  }

  public SendResult queued(Integer queued) {
    this.queued = queued;
    return this;
  }

  /**
   * Messages queued.
   * @return queued
   */
  @NotNull 
  @JsonProperty("queued")
  public Integer getQueued() {
    return queued;
  }

  @JsonProperty("queued")
  public void setQueued(Integer queued) {
    this.queued = queued;
  }

  public SendResult skipped(Integer skipped) {
    this.skipped = skipped;
    return this;
  }

  /**
   * Recipients without a usable address.
   * @return skipped
   */
  @NotNull 
  @JsonProperty("skipped")
  public Integer getSkipped() {
    return skipped;
  }

  @JsonProperty("skipped")
  public void setSkipped(Integer skipped) {
    this.skipped = skipped;
  }

  public SendResult operation(@Nullable Operation operation) {
    this.operation = operation;
    return this;
  }

  /**
   * Get operation
   * @return operation
   */
  @Valid 
  @JsonProperty("operation")
  public @Nullable Operation getOperation() {
    return operation;
  }

  @JsonProperty("operation")
  public void setOperation(@Nullable Operation operation) {
    this.operation = operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SendResult sendResult = (SendResult) o;
    return Objects.equals(this.queued, sendResult.queued) &&
        Objects.equals(this.skipped, sendResult.skipped) &&
        Objects.equals(this.operation, sendResult.operation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(queued, skipped, operation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SendResult {\n");
    sb.append("    queued: ").append(toIndentedString(queued)).append("\n");
    sb.append("    skipped: ").append(toIndentedString(skipped)).append("\n");
    sb.append("    operation: ").append(toIndentedString(operation)).append("\n");
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

