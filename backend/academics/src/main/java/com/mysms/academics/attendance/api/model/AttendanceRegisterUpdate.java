// Owned API contract of attendance.
package com.mysms.academics.attendance.api.model;

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
 * Fields of AttendanceRegister that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AttendanceRegisterUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

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
  private @Nullable Optional<@NotNull RegisterScopeEnum> registerScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public AttendanceRegisterUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public AttendanceRegisterUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Register code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public AttendanceRegisterUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Register name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public AttendanceRegisterUpdate registerScope(@Nullable Optional<RegisterScopeEnum> registerScope) {
    this.registerScope = registerScope;
    return this;
  }

  /**
   * What the register covers.
   * @return registerScope
   */
  
  @JsonProperty("registerScope")
  public @Nullable Optional<RegisterScopeEnum> getRegisterScope() {
    return registerScope;
  }

  @JsonProperty("registerScope")
  public void setRegisterScope(@Nullable Optional<RegisterScopeEnum> registerScope) {
    this.registerScope = registerScope;
  }

  public AttendanceRegisterUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section (scope class_section).
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public AttendanceRegisterUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (scope course_batch/subject).
   * @return courseId
   */
  
  @JsonProperty("courseId")
  public @Nullable Optional<UUID> getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
  }

  public AttendanceRegisterUpdate batchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (scope course_batch/subject).
   * @return batchId
   */
  
  @JsonProperty("batchId")
  public @Nullable Optional<UUID> getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable Optional<UUID> batchId) {
    this.batchId = batchId;
  }

  public AttendanceRegisterUpdate subjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject (scope subject).
   * @return subjectId
   */
  
  @JsonProperty("subjectId")
  public @Nullable Optional<UUID> getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
  }

  public AttendanceRegisterUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether in use.
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

