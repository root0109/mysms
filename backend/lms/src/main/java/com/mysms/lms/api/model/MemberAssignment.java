// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * MemberAssignment
 */

public class MemberAssignment {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String roleLabel;

  public MemberAssignment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MemberAssignment(UUID userId) {
    this.userId = userId;
  }

  public MemberAssignment userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public MemberAssignment roleId(@Nullable UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Cohort role (staff); omit for students.
   * @return roleId
   */
  @Valid 
  @JsonProperty("roleId")
  public @Nullable UUID getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable UUID roleId) {
    this.roleId = roleId;
  }

  public MemberAssignment roleLabel(@Nullable String roleLabel) {
    this.roleLabel = roleLabel;
    return this;
  }

  /**
   * Label shown for the role.
   * @return roleLabel
   */
  
  @JsonProperty("roleLabel")
  public @Nullable String getRoleLabel() {
    return roleLabel;
  }

  @JsonProperty("roleLabel")
  public void setRoleLabel(@Nullable String roleLabel) {
    this.roleLabel = roleLabel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MemberAssignment memberAssignment = (MemberAssignment) o;
    return Objects.equals(this.userId, memberAssignment.userId) &&
        Objects.equals(this.roleId, memberAssignment.roleId) &&
        Objects.equals(this.roleLabel, memberAssignment.roleLabel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleId, roleLabel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MemberAssignment {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    roleLabel: ").append(toIndentedString(roleLabel)).append("\n");
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

