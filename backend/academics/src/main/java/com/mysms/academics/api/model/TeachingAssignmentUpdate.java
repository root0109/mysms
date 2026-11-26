// Owned API contract of academics.
package com.mysms.academics.api.model;

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
 * Fields of TeachingAssignment that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class TeachingAssignmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> classSubjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> periodsPerWeek;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> periodsRemaining;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> teacherRole;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public TeachingAssignmentUpdate classSubjectId(@Nullable Optional<UUID> classSubjectId) {
    this.classSubjectId = classSubjectId;
    return this;
  }

  /**
   * Class subject taught.
   * @return classSubjectId
   */
  
  @JsonProperty("classSubjectId")
  public @Nullable Optional<UUID> getClassSubjectId() {
    return classSubjectId;
  }

  @JsonProperty("classSubjectId")
  public void setClassSubjectId(@Nullable Optional<UUID> classSubjectId) {
    this.classSubjectId = classSubjectId;
  }

  public TeachingAssignmentUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Teacher.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public TeachingAssignmentUpdate periodsPerWeek(@Nullable Optional<Integer> periodsPerWeek) {
    this.periodsPerWeek = periodsPerWeek;
    return this;
  }

  /**
   * Periods allotted per week.
   * @return periodsPerWeek
   */
  
  @JsonProperty("periodsPerWeek")
  public @Nullable Optional<Integer> getPeriodsPerWeek() {
    return periodsPerWeek;
  }

  @JsonProperty("periodsPerWeek")
  public void setPeriodsPerWeek(@Nullable Optional<Integer> periodsPerWeek) {
    this.periodsPerWeek = periodsPerWeek;
  }

  public TeachingAssignmentUpdate periodsRemaining(@Nullable Optional<Integer> periodsRemaining) {
    this.periodsRemaining = periodsRemaining;
    return this;
  }

  /**
   * Periods not yet scheduled.
   * @return periodsRemaining
   */
  
  @JsonProperty("periodsRemaining")
  public @Nullable Optional<Integer> getPeriodsRemaining() {
    return periodsRemaining;
  }

  @JsonProperty("periodsRemaining")
  public void setPeriodsRemaining(@Nullable Optional<Integer> periodsRemaining) {
    this.periodsRemaining = periodsRemaining;
  }

  public TeachingAssignmentUpdate teacherRole(@Nullable Optional<String> teacherRole) {
    this.teacherRole = teacherRole;
    return this;
  }

  /**
   * Role on the subject (main, assistant, lab).
   * @return teacherRole
   */
  
  @JsonProperty("teacherRole")
  public @Nullable Optional<String> getTeacherRole() {
    return teacherRole;
  }

  @JsonProperty("teacherRole")
  public void setTeacherRole(@Nullable Optional<String> teacherRole) {
    this.teacherRole = teacherRole;
  }

  public TeachingAssignmentUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether current.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TeachingAssignmentUpdate teachingAssignmentUpdate = (TeachingAssignmentUpdate) o;
    return Objects.equals(this.classSubjectId, teachingAssignmentUpdate.classSubjectId) &&
        Objects.equals(this.staffId, teachingAssignmentUpdate.staffId) &&
        Objects.equals(this.periodsPerWeek, teachingAssignmentUpdate.periodsPerWeek) &&
        Objects.equals(this.periodsRemaining, teachingAssignmentUpdate.periodsRemaining) &&
        Objects.equals(this.teacherRole, teachingAssignmentUpdate.teacherRole) &&
        Objects.equals(this.isActive, teachingAssignmentUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classSubjectId, staffId, periodsPerWeek, periodsRemaining, teacherRole, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TeachingAssignmentUpdate {\n");
    sb.append("    classSubjectId: ").append(toIndentedString(classSubjectId)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    periodsPerWeek: ").append(toIndentedString(periodsPerWeek)).append("\n");
    sb.append("    periodsRemaining: ").append(toIndentedString(periodsRemaining)).append("\n");
    sb.append("    teacherRole: ").append(toIndentedString(teacherRole)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

