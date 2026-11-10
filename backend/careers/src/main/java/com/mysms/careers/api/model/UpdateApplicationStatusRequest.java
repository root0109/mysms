// Owned API contract of careers (JAVA-CODING-RULES §7); first materialized from api/openapi/careers.yaml on 2026-11-10.
package com.mysms.careers.api.model;

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
 * UpdateApplicationStatusRequest
 */

public class UpdateApplicationStatusRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  /**
   * Status.
   */
  public enum StatusEnum {
    SUBMITTED("submitted"),
    
    SHORTLISTED("shortlisted"),
    
    REJECTED("rejected"),
    
    HIRED("hired"),
    
    WITHDRAWN("withdrawn");

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

  public UpdateApplicationStatusRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateApplicationStatusRequest(StatusEnum status) {
    this.status = status;
  }

  public UpdateApplicationStatusRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  public UpdateApplicationStatusRequest status(StatusEnum status) {
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
    UpdateApplicationStatusRequest updateApplicationStatusRequest = (UpdateApplicationStatusRequest) o;
    return Objects.equals(this.etag, updateApplicationStatusRequest.etag) &&
        Objects.equals(this.status, updateApplicationStatusRequest.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateApplicationStatusRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

