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
 * ShareDriveItemRequest
 */

public class ShareDriveItemRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> userIds = new ArrayList<>();

  public ShareDriveItemRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ShareDriveItemRequest(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public ShareDriveItemRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public ShareDriveItemRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Users to share with (replaces the current list; empty = unshare).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShareDriveItemRequest shareDriveItemRequest = (ShareDriveItemRequest) o;
    return Objects.equals(this.userIds, shareDriveItemRequest.userIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShareDriveItemRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
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

