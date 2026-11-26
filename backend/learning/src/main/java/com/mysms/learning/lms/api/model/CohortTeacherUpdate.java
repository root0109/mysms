// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CohortTeacher that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CohortTeacherUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> roleLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> hasOfficeHours;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> removedAt;

  public CohortTeacherUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Teacher.
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

  public CohortTeacherUpdate roleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Cohort role (teacher, teaching assistant, grader, observer...).
   * @return roleId
   */
  
  @JsonProperty("roleId")
  public @Nullable Optional<UUID> getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
  }

  public CohortTeacherUpdate roleLabel(@Nullable Optional<String> roleLabel) {
    this.roleLabel = roleLabel;
    return this;
  }

  /**
   * Label shown for the role in this cohort.
   * @return roleLabel
   */
  
  @JsonProperty("roleLabel")
  public @Nullable Optional<String> getRoleLabel() {
    return roleLabel;
  }

  @JsonProperty("roleLabel")
  public void setRoleLabel(@Nullable Optional<String> roleLabel) {
    this.roleLabel = roleLabel;
  }

  public CohortTeacherUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Membership active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public CohortTeacherUpdate hasOfficeHours(@Nullable Optional<Boolean> hasOfficeHours) {
    this.hasOfficeHours = hasOfficeHours;
    return this;
  }

  /**
   * Office hours published.
   * @return hasOfficeHours
   */
  
  @JsonProperty("hasOfficeHours")
  public @Nullable Optional<Boolean> getHasOfficeHours() {
    return hasOfficeHours;
  }

  @JsonProperty("hasOfficeHours")
  public void setHasOfficeHours(@Nullable Optional<Boolean> hasOfficeHours) {
    this.hasOfficeHours = hasOfficeHours;
  }

  public CohortTeacherUpdate removedAt(@Nullable Optional<OffsetDateTime> removedAt) {
    this.removedAt = removedAt;
    return this;
  }

  /**
   * Removed from the cohort.
   * @return removedAt
   */
  
  @JsonProperty("removedAt")
  public @Nullable Optional<OffsetDateTime> getRemovedAt() {
    return removedAt;
  }

  @JsonProperty("removedAt")
  public void setRemovedAt(@Nullable Optional<OffsetDateTime> removedAt) {
    this.removedAt = removedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortTeacherUpdate cohortTeacherUpdate = (CohortTeacherUpdate) o;
    return Objects.equals(this.userId, cohortTeacherUpdate.userId) &&
        Objects.equals(this.roleId, cohortTeacherUpdate.roleId) &&
        Objects.equals(this.roleLabel, cohortTeacherUpdate.roleLabel) &&
        Objects.equals(this.isActive, cohortTeacherUpdate.isActive) &&
        Objects.equals(this.hasOfficeHours, cohortTeacherUpdate.hasOfficeHours) &&
        Objects.equals(this.removedAt, cohortTeacherUpdate.removedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleId, roleLabel, isActive, hasOfficeHours, removedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortTeacherUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    roleLabel: ").append(toIndentedString(roleLabel)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    hasOfficeHours: ").append(toIndentedString(hasOfficeHours)).append("\n");
    sb.append("    removedAt: ").append(toIndentedString(removedAt)).append("\n");
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

