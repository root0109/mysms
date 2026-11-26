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
 * SetContentItemVisibilityRequest
 */

public class SetContentItemVisibilityRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean isActive;

  public SetContentItemVisibilityRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetContentItemVisibilityRequest(Boolean isActive) {
    this.isActive = isActive;
  }

  public SetContentItemVisibilityRequest isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Show (true) or hide (false) for students.
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetContentItemVisibilityRequest setContentItemVisibilityRequest = (SetContentItemVisibilityRequest) o;
    return Objects.equals(this.isActive, setContentItemVisibilityRequest.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetContentItemVisibilityRequest {\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

