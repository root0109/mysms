// Owned API contract of library.
package com.mysms.campus.library.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ReturnLoanRequest
 */

public class ReturnLoanRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate returnedOn;

  public ReturnLoanRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReturnLoanRequest(LocalDate returnedOn) {
    this.returnedOn = returnedOn;
  }

  public ReturnLoanRequest etag(@Nullable String etag) {
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

  public ReturnLoanRequest returnedOn(LocalDate returnedOn) {
    this.returnedOn = returnedOn;
    return this;
  }

  /**
   * Return date.
   * @return returnedOn
   */
  @NotNull @Valid 
  @JsonProperty("returnedOn")
  public LocalDate getReturnedOn() {
    return returnedOn;
  }

  @JsonProperty("returnedOn")
  public void setReturnedOn(LocalDate returnedOn) {
    this.returnedOn = returnedOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReturnLoanRequest returnLoanRequest = (ReturnLoanRequest) o;
    return Objects.equals(this.etag, returnLoanRequest.etag) &&
        Objects.equals(this.returnedOn, returnLoanRequest.returnedOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, returnedOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReturnLoanRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    returnedOn: ").append(toIndentedString(returnedOn)).append("\n");
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

