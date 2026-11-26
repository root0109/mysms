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
 * FlagForumPostRequest
 */

public class FlagForumPostRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean flagged;

  public FlagForumPostRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FlagForumPostRequest(Boolean flagged) {
    this.flagged = flagged;
  }

  public FlagForumPostRequest flagged(Boolean flagged) {
    this.flagged = flagged;
    return this;
  }

  /**
   * Flag / unflag for follow-up (per user).
   * @return flagged
   */
  @NotNull 
  @JsonProperty("flagged")
  public Boolean getFlagged() {
    return flagged;
  }

  @JsonProperty("flagged")
  public void setFlagged(Boolean flagged) {
    this.flagged = flagged;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FlagForumPostRequest flagForumPostRequest = (FlagForumPostRequest) o;
    return Objects.equals(this.flagged, flagForumPostRequest.flagged);
  }

  @Override
  public int hashCode() {
    return Objects.hash(flagged);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FlagForumPostRequest {\n");
    sb.append("    flagged: ").append(toIndentedString(flagged)).append("\n");
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

