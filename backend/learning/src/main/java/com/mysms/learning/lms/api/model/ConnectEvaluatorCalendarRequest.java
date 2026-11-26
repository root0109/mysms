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
 * ConnectEvaluatorCalendarRequest
 */

public class ConnectEvaluatorCalendarRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String redirectUri;

  public ConnectEvaluatorCalendarRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConnectEvaluatorCalendarRequest(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public ConnectEvaluatorCalendarRequest redirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
    return this;
  }

  /**
   * Where to return.
   * @return redirectUri
   */
  @NotNull 
  @JsonProperty("redirectUri")
  public String getRedirectUri() {
    return redirectUri;
  }

  @JsonProperty("redirectUri")
  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectEvaluatorCalendarRequest connectEvaluatorCalendarRequest = (ConnectEvaluatorCalendarRequest) o;
    return Objects.equals(this.redirectUri, connectEvaluatorCalendarRequest.redirectUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(redirectUri);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectEvaluatorCalendarRequest {\n");
    sb.append("    redirectUri: ").append(toIndentedString(redirectUri)).append("\n");
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

