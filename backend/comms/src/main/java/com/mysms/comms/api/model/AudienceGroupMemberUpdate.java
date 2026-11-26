// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AudienceGroupMember that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AudienceGroupMemberUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  /**
   * Member type.
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
  private @Nullable Optional<@NotNull MemberTypeEnum> memberType;

  public AudienceGroupMemberUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Member.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public AudienceGroupMemberUpdate memberType(@Nullable Optional<MemberTypeEnum> memberType) {
    this.memberType = memberType;
    return this;
  }

  /**
   * Member type.
   * @return memberType
   */
  
  @JsonProperty("memberType")
  public @Nullable Optional<MemberTypeEnum> getMemberType() {
    return memberType;
  }

  @JsonProperty("memberType")
  public void setMemberType(@Nullable Optional<MemberTypeEnum> memberType) {
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
    AudienceGroupMemberUpdate audienceGroupMemberUpdate = (AudienceGroupMemberUpdate) o;
    return Objects.equals(this.userId, audienceGroupMemberUpdate.userId) &&
        Objects.equals(this.memberType, audienceGroupMemberUpdate.memberType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, memberType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AudienceGroupMemberUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

