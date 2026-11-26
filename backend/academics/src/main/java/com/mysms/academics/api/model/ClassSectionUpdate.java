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
 * Fields of ClassSection that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ClassSectionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> sectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classTeacherStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> capacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> stream;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public ClassSectionUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public ClassSectionUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public ClassSectionUpdate gradeLevelId(@Nullable Optional<UUID> gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * Grade.
   * @return gradeLevelId
   */
  
  @JsonProperty("gradeLevelId")
  public @Nullable Optional<UUID> getGradeLevelId() {
    return gradeLevelId;
  }

  @JsonProperty("gradeLevelId")
  public void setGradeLevelId(@Nullable Optional<UUID> gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
  }

  public ClassSectionUpdate sectionId(@Nullable Optional<UUID> sectionId) {
    this.sectionId = sectionId;
    return this;
  }

  /**
   * Section.
   * @return sectionId
   */
  
  @JsonProperty("sectionId")
  public @Nullable Optional<UUID> getSectionId() {
    return sectionId;
  }

  @JsonProperty("sectionId")
  public void setSectionId(@Nullable Optional<UUID> sectionId) {
    this.sectionId = sectionId;
  }

  public ClassSectionUpdate classTeacherStaffId(@Nullable Optional<UUID> classTeacherStaffId) {
    this.classTeacherStaffId = classTeacherStaffId;
    return this;
  }

  /**
   * Class (homeroom) teacher.
   * @return classTeacherStaffId
   */
  
  @JsonProperty("classTeacherStaffId")
  public @Nullable Optional<UUID> getClassTeacherStaffId() {
    return classTeacherStaffId;
  }

  @JsonProperty("classTeacherStaffId")
  public void setClassTeacherStaffId(@Nullable Optional<UUID> classTeacherStaffId) {
    this.classTeacherStaffId = classTeacherStaffId;
  }

  public ClassSectionUpdate capacity(@Nullable Optional<Integer> capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Maximum students.
   * @return capacity
   */
  
  @JsonProperty("capacity")
  public @Nullable Optional<Integer> getCapacity() {
    return capacity;
  }

  @JsonProperty("capacity")
  public void setCapacity(@Nullable Optional<Integer> capacity) {
    this.capacity = capacity;
  }

  public ClassSectionUpdate stream(@Nullable Optional<String> stream) {
    this.stream = stream;
    return this;
  }

  /**
   * Stream for senior grades (Science, Commerce...).
   * @return stream
   */
  
  @JsonProperty("stream")
  public @Nullable Optional<String> getStream() {
    return stream;
  }

  @JsonProperty("stream")
  public void setStream(@Nullable Optional<String> stream) {
    this.stream = stream;
  }

  public ClassSectionUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether running.
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
    ClassSectionUpdate classSectionUpdate = (ClassSectionUpdate) o;
    return Objects.equals(this.institutionId, classSectionUpdate.institutionId) &&
        Objects.equals(this.academicYearId, classSectionUpdate.academicYearId) &&
        Objects.equals(this.gradeLevelId, classSectionUpdate.gradeLevelId) &&
        Objects.equals(this.sectionId, classSectionUpdate.sectionId) &&
        Objects.equals(this.classTeacherStaffId, classSectionUpdate.classTeacherStaffId) &&
        Objects.equals(this.capacity, classSectionUpdate.capacity) &&
        Objects.equals(this.stream, classSectionUpdate.stream) &&
        Objects.equals(this.isActive, classSectionUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, gradeLevelId, sectionId, classTeacherStaffId, capacity, stream, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassSectionUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    gradeLevelId: ").append(toIndentedString(gradeLevelId)).append("\n");
    sb.append("    sectionId: ").append(toIndentedString(sectionId)).append("\n");
    sb.append("    classTeacherStaffId: ").append(toIndentedString(classTeacherStaffId)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    stream: ").append(toIndentedString(stream)).append("\n");
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

