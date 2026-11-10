// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
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
 * FollowClassPageRequest
 */

public class FollowClassPageRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean follow;

  public FollowClassPageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FollowClassPageRequest(Boolean follow) {
    this.follow = follow;
  }

  public FollowClassPageRequest follow(Boolean follow) {
    this.follow = follow;
    return this;
  }

  /**
   * Follow / unfollow.
   * @return follow
   */
  @NotNull 
  @JsonProperty("follow")
  public Boolean getFollow() {
    return follow;
  }

  @JsonProperty("follow")
  public void setFollow(Boolean follow) {
    this.follow = follow;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FollowClassPageRequest followClassPageRequest = (FollowClassPageRequest) o;
    return Objects.equals(this.follow, followClassPageRequest.follow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(follow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FollowClassPageRequest {\n");
    sb.append("    follow: ").append(toIndentedString(follow)).append("\n");
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

