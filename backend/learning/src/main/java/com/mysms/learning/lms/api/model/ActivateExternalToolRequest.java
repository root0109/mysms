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
 * ActivateExternalToolRequest
 */

public class ActivateExternalToolRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String activationKey;

  public ActivateExternalToolRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActivateExternalToolRequest(String activationKey) {
    this.activationKey = activationKey;
  }

  public ActivateExternalToolRequest activationKey(String activationKey) {
    this.activationKey = activationKey;
    return this;
  }

  /**
   * Licence / activation key from the vendor.
   * @return activationKey
   */
  @NotNull 
  @JsonProperty("activationKey")
  public String getActivationKey() {
    return activationKey;
  }

  @JsonProperty("activationKey")
  public void setActivationKey(String activationKey) {
    this.activationKey = activationKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActivateExternalToolRequest activateExternalToolRequest = (ActivateExternalToolRequest) o;
    return Objects.equals(this.activationKey, activateExternalToolRequest.activationKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activationKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActivateExternalToolRequest {\n");
    sb.append("    activationKey: ").append(toIndentedString(activationKey)).append("\n");
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

