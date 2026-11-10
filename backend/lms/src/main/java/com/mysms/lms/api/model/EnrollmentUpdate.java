// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Enrollment that can be updated (partial update; see updateMask).
 */

public class EnrollmentUpdate {

  /**
   * Role in the course.
   */
  public enum MemberRoleEnum {
    MEMBER("member"),
    
    ADMIN("admin");

    private final String value;

    MemberRoleEnum(String value) {
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
    public static MemberRoleEnum fromValue(String value) {
      for (MemberRoleEnum b : MemberRoleEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MemberRoleEnum memberRole;

  public EnrollmentUpdate memberRole(@Nullable MemberRoleEnum memberRole) {
    this.memberRole = memberRole;
    return this;
  }

  /**
   * Role in the course.
   * @return memberRole
   */
  
  @JsonProperty("memberRole")
  public @Nullable MemberRoleEnum getMemberRole() {
    return memberRole;
  }

  @JsonProperty("memberRole")
  public void setMemberRole(@Nullable MemberRoleEnum memberRole) {
    this.memberRole = memberRole;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrollmentUpdate enrollmentUpdate = (EnrollmentUpdate) o;
    return Objects.equals(this.memberRole, enrollmentUpdate.memberRole);
  }

  @Override
  public int hashCode() {
    return Objects.hash(memberRole);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrollmentUpdate {\n");
    sb.append("    memberRole: ").append(toIndentedString(memberRole)).append("\n");
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

