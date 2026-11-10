// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of CohortTeacher that can be updated (partial update; see updateMask).
 */

public class CohortTeacherUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String roleLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hasOfficeHours;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime removedAt;

  public CohortTeacherUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Teacher.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public CohortTeacherUpdate roleId(@Nullable UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Cohort role (teacher, teaching assistant, grader, observer...).
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

  public CohortTeacherUpdate roleLabel(@Nullable String roleLabel) {
    this.roleLabel = roleLabel;
    return this;
  }

  /**
   * Label shown for the role in this cohort.
   * @return roleLabel
   */
  @Size(max = 100) 
  @JsonProperty("roleLabel")
  public @Nullable String getRoleLabel() {
    return roleLabel;
  }

  @JsonProperty("roleLabel")
  public void setRoleLabel(@Nullable String roleLabel) {
    this.roleLabel = roleLabel;
  }

  public CohortTeacherUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Membership active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public CohortTeacherUpdate hasOfficeHours(@Nullable Boolean hasOfficeHours) {
    this.hasOfficeHours = hasOfficeHours;
    return this;
  }

  /**
   * Office hours published.
   * @return hasOfficeHours
   */
  
  @JsonProperty("hasOfficeHours")
  public @Nullable Boolean getHasOfficeHours() {
    return hasOfficeHours;
  }

  @JsonProperty("hasOfficeHours")
  public void setHasOfficeHours(@Nullable Boolean hasOfficeHours) {
    this.hasOfficeHours = hasOfficeHours;
  }

  public CohortTeacherUpdate removedAt(@Nullable OffsetDateTime removedAt) {
    this.removedAt = removedAt;
    return this;
  }

  /**
   * Removed from the cohort.
   * @return removedAt
   */
  @Valid 
  @JsonProperty("removedAt")
  public @Nullable OffsetDateTime getRemovedAt() {
    return removedAt;
  }

  @JsonProperty("removedAt")
  public void setRemovedAt(@Nullable OffsetDateTime removedAt) {
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

