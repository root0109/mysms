// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * AddGroupMembersRequest
 */

public class AddGroupMembersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> userIds = new ArrayList<>();

  /**
   * Type.
   */
  public enum MemberTypeEnum {
    STUDENT("student"),
    
    TEACHER("teacher"),
    
    NON_TEACHING("non_teaching");

    private final String value;

    MemberTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static MemberTypeEnum fromValue(String value) {
      for (MemberTypeEnum b : MemberTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private MemberTypeEnum memberType;

  public AddGroupMembersRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AddGroupMembersRequest(List<UUID> userIds, MemberTypeEnum memberType) {
    this.userIds = userIds;
    this.memberType = memberType;
  }

  public AddGroupMembersRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public AddGroupMembersRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Users.
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

  public AddGroupMembersRequest memberType(MemberTypeEnum memberType) {
    this.memberType = memberType;
    return this;
  }

  /**
   * Type.
   * @return memberType
   */
  @NotNull 
  @JsonProperty("memberType")
  public MemberTypeEnum getMemberType() {
    return memberType;
  }

  @JsonProperty("memberType")
  public void setMemberType(MemberTypeEnum memberType) {
    this.memberType = memberType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddGroupMembersRequest addGroupMembersRequest = (AddGroupMembersRequest) o;
    return Objects.equals(this.userIds, addGroupMembersRequest.userIds) &&
        Objects.equals(this.memberType, addGroupMembersRequest.memberType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds, memberType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddGroupMembersRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    memberType: ").append(toIndentedString(memberType)).append("\n");
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

