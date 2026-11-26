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
 * SetGroupMembersRequest
 */

public class SetGroupMembersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> addUserIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> removeUserIds = new ArrayList<>();

  public SetGroupMembersRequest addUserIds(List<UUID> addUserIds) {
    this.addUserIds = addUserIds;
    return this;
  }

  public SetGroupMembersRequest addAddUserIdsItem(UUID addUserIdsItem) {
    if (this.addUserIds == null) {
      this.addUserIds = new ArrayList<>();
    }
    this.addUserIds.add(addUserIdsItem);
    return this;
  }

  /**
   * Add.
   * @return addUserIds
   */
  
  @JsonProperty("addUserIds")
  public List<UUID> getAddUserIds() {
    return addUserIds;
  }

  @JsonProperty("addUserIds")
  public void setAddUserIds(List<UUID> addUserIds) {
    this.addUserIds = addUserIds;
  }

  public SetGroupMembersRequest removeUserIds(List<UUID> removeUserIds) {
    this.removeUserIds = removeUserIds;
    return this;
  }

  public SetGroupMembersRequest addRemoveUserIdsItem(UUID removeUserIdsItem) {
    if (this.removeUserIds == null) {
      this.removeUserIds = new ArrayList<>();
    }
    this.removeUserIds.add(removeUserIdsItem);
    return this;
  }

  /**
   * Remove.
   * @return removeUserIds
   */
  
  @JsonProperty("removeUserIds")
  public List<UUID> getRemoveUserIds() {
    return removeUserIds;
  }

  @JsonProperty("removeUserIds")
  public void setRemoveUserIds(List<UUID> removeUserIds) {
    this.removeUserIds = removeUserIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetGroupMembersRequest setGroupMembersRequest = (SetGroupMembersRequest) o;
    return Objects.equals(this.addUserIds, setGroupMembersRequest.addUserIds) &&
        Objects.equals(this.removeUserIds, setGroupMembersRequest.removeUserIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addUserIds, removeUserIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetGroupMembersRequest {\n");
    sb.append("    addUserIds: ").append(toIndentedString(addUserIds)).append("\n");
    sb.append("    removeUserIds: ").append(toIndentedString(removeUserIds)).append("\n");
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

