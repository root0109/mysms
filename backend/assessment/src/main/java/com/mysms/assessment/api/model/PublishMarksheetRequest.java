// Owned API contract of assessment.
package com.mysms.assessment.api.model;

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
 * PublishMarksheetRequest
 */

public class PublishMarksheetRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean visibleToGuardians;

  public PublishMarksheetRequest visibleToGuardians(@Nullable Boolean visibleToGuardians) {
    this.visibleToGuardians = visibleToGuardians;
    return this;
  }

  /**
   * Whether guardians can see it.
   * @return visibleToGuardians
   */
  
  @JsonProperty("visibleToGuardians")
  public @Nullable Boolean getVisibleToGuardians() {
    return visibleToGuardians;
  }

  @JsonProperty("visibleToGuardians")
  public void setVisibleToGuardians(@Nullable Boolean visibleToGuardians) {
    this.visibleToGuardians = visibleToGuardians;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublishMarksheetRequest publishMarksheetRequest = (PublishMarksheetRequest) o;
    return Objects.equals(this.visibleToGuardians, publishMarksheetRequest.visibleToGuardians);
  }

  @Override
  public int hashCode() {
    return Objects.hash(visibleToGuardians);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublishMarksheetRequest {\n");
    sb.append("    visibleToGuardians: ").append(toIndentedString(visibleToGuardians)).append("\n");
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

