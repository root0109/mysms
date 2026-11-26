// Owned API contract of frontoffice.
package com.mysms.campus.frontoffice.api.model;

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
 * CheckOutVisitorRequest
 */

public class CheckOutVisitorRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String exitTime;

  public CheckOutVisitorRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckOutVisitorRequest(String exitTime) {
    this.exitTime = exitTime;
  }

  public CheckOutVisitorRequest etag(@Nullable String etag) {
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

  public CheckOutVisitorRequest exitTime(String exitTime) {
    this.exitTime = exitTime;
    return this;
  }

  /**
   * Exit time.
   * @return exitTime
   */
  @NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("exitTime")
  public String getExitTime() {
    return exitTime;
  }

  @JsonProperty("exitTime")
  public void setExitTime(String exitTime) {
    this.exitTime = exitTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckOutVisitorRequest checkOutVisitorRequest = (CheckOutVisitorRequest) o;
    return Objects.equals(this.etag, checkOutVisitorRequest.etag) &&
        Objects.equals(this.exitTime, checkOutVisitorRequest.exitTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, exitTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckOutVisitorRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    exitTime: ").append(toIndentedString(exitTime)).append("\n");
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

