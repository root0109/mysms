// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GrantItemAccessRequest
 */

public class GrantItemAccessRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> userIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean grant;

  public GrantItemAccessRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GrantItemAccessRequest(List<UUID> userIds, Boolean grant) {
    this.userIds = userIds;
    this.grant = grant;
  }

  public GrantItemAccessRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public GrantItemAccessRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Students.
   * @return userIds
   */
  @NotNull 
  @JsonProperty("userIds")
  public List<UUID> getUserIds() {
    return userIds;
  }

  @JsonProperty("userIds")
  public void setUserIds(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public GrantItemAccessRequest grant(Boolean grant) {
    this.grant = grant;
    return this;
  }

  /**
   * Grant (true) or withdraw (false) manual access.
   * @return grant
   */
  @NotNull 
  @JsonProperty("grant")
  public Boolean getGrant() {
    return grant;
  }

  @JsonProperty("grant")
  public void setGrant(Boolean grant) {
    this.grant = grant;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GrantItemAccessRequest grantItemAccessRequest = (GrantItemAccessRequest) o;
    return Objects.equals(this.userIds, grantItemAccessRequest.userIds) &&
        Objects.equals(this.grant, grantItemAccessRequest.grant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds, grant);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GrantItemAccessRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    grant: ").append(toIndentedString(grant)).append("\n");
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

