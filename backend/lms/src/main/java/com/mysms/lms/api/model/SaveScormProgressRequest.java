// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SaveScormProgressRequest
 */

public class SaveScormProgressRequest {

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Object runtimeState = null;

  /**
   * Status.
   */
  public enum StatusEnum {
    COMPLETE("complete"),
    
    PARTIALLY_COMPLETE("partially_complete"),
    
    INCOMPLETE("incomplete");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private StatusEnum status;

  public SaveScormProgressRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SaveScormProgressRequest(Object runtimeState, StatusEnum status) {
    this.runtimeState = runtimeState;
    this.status = status;
  }

  public SaveScormProgressRequest runtimeState(Object runtimeState) {
    this.runtimeState = runtimeState;
    return this;
  }

  /**
   * CMI data.
   * @return runtimeState
   */
  
  @JsonProperty("runtimeState")
  public Object getRuntimeState() {
    return runtimeState;
  }

  @JsonProperty("runtimeState")
  public void setRuntimeState(Object runtimeState) {
    this.runtimeState = runtimeState;
  }

  public SaveScormProgressRequest status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Status.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SaveScormProgressRequest saveScormProgressRequest = (SaveScormProgressRequest) o;
    return Objects.equals(this.runtimeState, saveScormProgressRequest.runtimeState) &&
        Objects.equals(this.status, saveScormProgressRequest.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtimeState, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SaveScormProgressRequest {\n");
    sb.append("    runtimeState: ").append(toIndentedString(runtimeState)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

