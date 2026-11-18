// Owned API contract of attendance.
package com.mysms.attendance.api.model;

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
 * Fields of AttendanceRegister that can be updated (partial update; see updateMask).
 */

public class AttendanceRegisterUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  /**
   * What the register covers.
   */
  public enum RegisterScopeEnum {
    INSTITUTION("institution"),
    
    CLASS_SECTION("class_section"),
    
    COURSE_BATCH("course_batch"),
    
    SUBJECT("subject");

    private final String value;

    RegisterScopeEnum(String value) {
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
    public static RegisterScopeEnum fromValue(String value) {
      for (RegisterScopeEnum b : RegisterScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RegisterScopeEnum registerScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public AttendanceRegisterUpdate institutionId(@Nullable UUID institutionId) {
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

  public AttendanceRegisterUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Register code.
   * @return code
   */
  @Size(max = 30) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public AttendanceRegisterUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Register name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AttendanceRegisterUpdate registerScope(@Nullable RegisterScopeEnum registerScope) {
    this.registerScope = registerScope;
    return this;
  }

  /**
   * What the register covers.
   * @return registerScope
   */
  
  @JsonProperty("registerScope")
  public @Nullable RegisterScopeEnum getRegisterScope() {
    return registerScope;
  }

  @JsonProperty("registerScope")
  public void setRegisterScope(@Nullable RegisterScopeEnum registerScope) {
    this.registerScope = registerScope;
  }

  public AttendanceRegisterUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section (scope class_section).
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public AttendanceRegisterUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (scope course_batch/subject).
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public AttendanceRegisterUpdate batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (scope course_batch/subject).
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public AttendanceRegisterUpdate subjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject (scope subject).
   * @return subjectId
   */
  @Valid 
  @JsonProperty("subjectId")
  public @Nullable UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
  }

  public AttendanceRegisterUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether in use.
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
    AttendanceRegisterUpdate attendanceRegisterUpdate = (AttendanceRegisterUpdate) o;
    return Objects.equals(this.institutionId, attendanceRegisterUpdate.institutionId) &&
        Objects.equals(this.code, attendanceRegisterUpdate.code) &&
        Objects.equals(this.name, attendanceRegisterUpdate.name) &&
        Objects.equals(this.registerScope, attendanceRegisterUpdate.registerScope) &&
        Objects.equals(this.classSectionId, attendanceRegisterUpdate.classSectionId) &&
        Objects.equals(this.courseId, attendanceRegisterUpdate.courseId) &&
        Objects.equals(this.batchId, attendanceRegisterUpdate.batchId) &&
        Objects.equals(this.subjectId, attendanceRegisterUpdate.subjectId) &&
        Objects.equals(this.isActive, attendanceRegisterUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, code, name, registerScope, classSectionId, courseId, batchId, subjectId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttendanceRegisterUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    registerScope: ").append(toIndentedString(registerScope)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
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

