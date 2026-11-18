// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * SendTestMessageRequest
 */

public class SendTestMessageRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String to;

  public SendTestMessageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SendTestMessageRequest(String to) {
    this.to = to;
  }

  public SendTestMessageRequest to(String to) {
    this.to = to;
    return this;
  }

  /**
   * Address / number.
   * @return to
   */
  @NotNull 
  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  @JsonProperty("to")
  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SendTestMessageRequest sendTestMessageRequest = (SendTestMessageRequest) o;
    return Objects.equals(this.to, sendTestMessageRequest.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(to);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SendTestMessageRequest {\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
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

