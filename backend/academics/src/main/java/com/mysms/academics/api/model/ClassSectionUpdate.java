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
 * Fields of ClassSection that can be updated (partial update; see updateMask).
 */

public class ClassSectionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID sectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classTeacherStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer capacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String stream;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public ClassSectionUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public ClassSectionUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public ClassSectionUpdate gradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * Grade.
   * @return gradeLevelId
   */
  @Valid 
  @JsonProperty("gradeLevelId")
  public @Nullable UUID getGradeLevelId() {
    return gradeLevelId;
  }

  @JsonProperty("gradeLevelId")
  public void setGradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
  }

  public ClassSectionUpdate sectionId(@Nullable UUID sectionId) {
    this.sectionId = sectionId;
    return this;
  }

  /**
   * Section.
   * @return sectionId
   */
  @Valid 
  @JsonProperty("sectionId")
  public @Nullable UUID getSectionId() {
    return sectionId;
  }

  @JsonProperty("sectionId")
  public void setSectionId(@Nullable UUID sectionId) {
    this.sectionId = sectionId;
  }

  public ClassSectionUpdate classTeacherStaffId(@Nullable UUID classTeacherStaffId) {
    this.classTeacherStaffId = classTeacherStaffId;
    return this;
  }

  /**
   * Class (homeroom) teacher.
   * @return classTeacherStaffId
   */
  @Valid 
  @JsonProperty("classTeacherStaffId")
  public @Nullable UUID getClassTeacherStaffId() {
    return classTeacherStaffId;
  }

  @JsonProperty("classTeacherStaffId")
  public void setClassTeacherStaffId(@Nullable UUID classTeacherStaffId) {
    this.classTeacherStaffId = classTeacherStaffId;
  }

  public ClassSectionUpdate capacity(@Nullable Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Maximum students.
   * @return capacity
   */
  
  @JsonProperty("capacity")
  public @Nullable Integer getCapacity() {
    return capacity;
  }

  @JsonProperty("capacity")
  public void setCapacity(@Nullable Integer capacity) {
    this.capacity = capacity;
  }

  public ClassSectionUpdate stream(@Nullable String stream) {
    this.stream = stream;
    return this;
  }

  /**
   * Stream for senior grades (Science, Commerce...).
   * @return stream
   */
  @Size(max = 60) 
  @JsonProperty("stream")
  public @Nullable String getStream() {
    return stream;
  }

  @JsonProperty("stream")
  public void setStream(@Nullable String stream) {
    this.stream = stream;
  }

  public ClassSectionUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether running.
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

