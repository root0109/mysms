// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ChangeStudentStatusRequest
 */

public class ChangeStudentStatusRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  /**
   * New status.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    INACTIVE("inactive"),
    
    ALUMNI("alumni"),
    
    WITHDRAWN("withdrawn"),
    
    TRANSFERRED("transferred"),
    
    GRADUATED("graduated");

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate exitDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String exitReason;

  public ChangeStudentStatusRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ChangeStudentStatusRequest(StatusEnum status) {
    this.status = status;
  }

  public ChangeStudentStatusRequest etag(@Nullable String etag) {
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

  public ChangeStudentStatusRequest status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * New status.
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

  public ChangeStudentStatusRequest exitDate(@Nullable LocalDate exitDate) {
    this.exitDate = exitDate;
    return this;
  }

  /**
   * Exit date.
   * @return exitDate
   */
  @Valid 
  @JsonProperty("exitDate")
  public @Nullable LocalDate getExitDate() {
    return exitDate;
  }

  @JsonProperty("exitDate")
  public void setExitDate(@Nullable LocalDate exitDate) {
    this.exitDate = exitDate;
  }

  public ChangeStudentStatusRequest exitReason(@Nullable String exitReason) {
    this.exitReason = exitReason;
    return this;
  }

  /**
   * Reason.
   * @return exitReason
   */
  
  @JsonProperty("exitReason")
  public @Nullable String getExitReason() {
    return exitReason;
  }

  @JsonProperty("exitReason")
  public void setExitReason(@Nullable String exitReason) {
    this.exitReason = exitReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangeStudentStatusRequest changeStudentStatusRequest = (ChangeStudentStatusRequest) o;
    return Objects.equals(this.etag, changeStudentStatusRequest.etag) &&
        Objects.equals(this.status, changeStudentStatusRequest.status) &&
        Objects.equals(this.exitDate, changeStudentStatusRequest.exitDate) &&
        Objects.equals(this.exitReason, changeStudentStatusRequest.exitReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, status, exitDate, exitReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChangeStudentStatusRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    exitDate: ").append(toIndentedString(exitDate)).append("\n");
    sb.append("    exitReason: ").append(toIndentedString(exitReason)).append("\n");
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

