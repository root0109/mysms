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
 * SetReleaseRulesEnabledRequest
 */

public class SetReleaseRulesEnabledRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean enabled;

  public SetReleaseRulesEnabledRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetReleaseRulesEnabledRequest(Boolean enabled) {
    this.enabled = enabled;
  }

  public SetReleaseRulesEnabledRequest enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Enable or disable every rule of the item.
   * @return enabled
   */
  @NotNull 
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetReleaseRulesEnabledRequest setReleaseRulesEnabledRequest = (SetReleaseRulesEnabledRequest) o;
    return Objects.equals(this.enabled, setReleaseRulesEnabledRequest.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetReleaseRulesEnabledRequest {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

