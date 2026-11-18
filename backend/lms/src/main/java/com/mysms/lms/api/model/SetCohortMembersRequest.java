// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.MemberAssignment;
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
 * SetCohortMembersRequest
 */

public class SetCohortMembersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid MemberAssignment> members = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> removeUserIds = new ArrayList<>();

  public SetCohortMembersRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetCohortMembersRequest(List<@Valid MemberAssignment> members) {
    this.members = members;
  }

  public SetCohortMembersRequest members(List<@Valid MemberAssignment> members) {
    this.members = members;
    return this;
  }

  public SetCohortMembersRequest addMembersItem(MemberAssignment membersItem) {
    if (this.members == null) {
      this.members = new ArrayList<>();
    }
    this.members.add(membersItem);
    return this;
  }

  /**
   * Members to add or update.
   * @return members
   */
  @JsonProperty("members")
  public @Nullable List<@Valid MemberAssignment> getMembers() {
    return members;
  }

  @JsonProperty("members")
  public void setMembers(List<@Valid MemberAssignment> members) {
    this.members = members;
  }

  public SetCohortMembersRequest removeUserIds(List<UUID> removeUserIds) {
    this.removeUserIds = removeUserIds;
    return this;
  }

  public SetCohortMembersRequest addRemoveUserIdsItem(UUID removeUserIdsItem) {
    if (this.removeUserIds == null) {
      this.removeUserIds = new ArrayList<>();
    }
    this.removeUserIds.add(removeUserIdsItem);
    return this;
  }

  /**
   * Members to remove.
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
    SetCohortMembersRequest setCohortMembersRequest = (SetCohortMembersRequest) o;
    return Objects.equals(this.members, setCohortMembersRequest.members) &&
        Objects.equals(this.removeUserIds, setCohortMembersRequest.removeUserIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(members, removeUserIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetCohortMembersRequest {\n");
    sb.append("    members: ").append(toIndentedString(members)).append("\n");
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

