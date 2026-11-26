// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * RosterEntry
 */

public class RosterEntry {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String roleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID enrollmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID teacherId;

  public RosterEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RosterEntry(UUID userId, String displayName, String roleCode, Boolean isActive) {
    this.userId = userId;
    this.displayName = displayName;
    this.roleCode = roleCode;
    this.isActive = isActive;
  }

  public RosterEntry userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Member.
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

  public RosterEntry displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Name.
   * @return displayName
   */
  @NotNull 
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public RosterEntry email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * E-mail; returned only to cohort staff and administrators (empty for students, guardians and observers).
   * @return email
   */
  
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public RosterEntry roleCode(String roleCode) {
    this.roleCode = roleCode;
    return this;
  }

  /**
   * Role in the cohort (student, teacher, assistant, observer...).
   * @return roleCode
   */
  @NotNull 
  @JsonProperty("roleCode")
  public String getRoleCode() {
    return roleCode;
  }

  @JsonProperty("roleCode")
  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public RosterEntry isActive(Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Active member.
   * @return isActive
   */
  @NotNull 
  @JsonProperty("isActive")
  public Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public RosterEntry enrollmentId(@Nullable UUID enrollmentId) {
    this.enrollmentId = enrollmentId;
    return this;
  }

  /**
   * Student enrolment.
   * @return enrollmentId
   */
  @Valid 
  @JsonProperty("enrollmentId")
  public @Nullable UUID getEnrollmentId() {
    return enrollmentId;
  }

  @JsonProperty("enrollmentId")
  public void setEnrollmentId(@Nullable UUID enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public RosterEntry teacherId(@Nullable UUID teacherId) {
    this.teacherId = teacherId;
    return this;
  }

  /**
   * Staff membership.
   * @return teacherId
   */
  @Valid 
  @JsonProperty("teacherId")
  public @Nullable UUID getTeacherId() {
    return teacherId;
  }

  @JsonProperty("teacherId")
  public void setTeacherId(@Nullable UUID teacherId) {
    this.teacherId = teacherId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RosterEntry rosterEntry = (RosterEntry) o;
    return Objects.equals(this.userId, rosterEntry.userId) &&
        Objects.equals(this.displayName, rosterEntry.displayName) &&
        Objects.equals(this.email, rosterEntry.email) &&
        Objects.equals(this.roleCode, rosterEntry.roleCode) &&
        Objects.equals(this.isActive, rosterEntry.isActive) &&
        Objects.equals(this.enrollmentId, rosterEntry.enrollmentId) &&
        Objects.equals(this.teacherId, rosterEntry.teacherId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, displayName, email, roleCode, isActive, enrollmentId, teacherId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RosterEntry {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    roleCode: ").append(toIndentedString(roleCode)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    enrollmentId: ").append(toIndentedString(enrollmentId)).append("\n");
    sb.append("    teacherId: ").append(toIndentedString(teacherId)).append("\n");
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

