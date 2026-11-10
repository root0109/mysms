// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
package com.mysms.academics.api.model;

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
 * Fields of Course that can be updated (partial update; see updateMask).
 */

public class CourseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID departmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  /**
   * Evaluation scheme.
   */
  public enum EvaluationTypeEnum {
    NORMAL("normal"),
    
    GPA("gpa"),
    
    CWA("cwa"),
    
    CCE("cce");

    private final String value;

    EvaluationTypeEnum(String value) {
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
    public static EvaluationTypeEnum fromValue(String value) {
      for (EvaluationTypeEnum b : EvaluationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EvaluationTypeEnum evaluationType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String minUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public CourseUpdate institutionId(@Nullable UUID institutionId) {
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

  public CourseUpdate programId(@Nullable UUID programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program.
   * @return programId
   */
  @Valid 
  @JsonProperty("programId")
  public @Nullable UUID getProgramId() {
    return programId;
  }

  @JsonProperty("programId")
  public void setProgramId(@Nullable UUID programId) {
    this.programId = programId;
  }

  public CourseUpdate departmentId(@Nullable UUID departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  /**
   * Department.
   * @return departmentId
   */
  @Valid 
  @JsonProperty("departmentId")
  public @Nullable UUID getDepartmentId() {
    return departmentId;
  }

  @JsonProperty("departmentId")
  public void setDepartmentId(@Nullable UUID departmentId) {
    this.departmentId = departmentId;
  }

  public CourseUpdate parentCourseId(@Nullable UUID parentCourseId) {
    this.parentCourseId = parentCourseId;
    return this;
  }

  /**
   * Parent course.
   * @return parentCourseId
   */
  @Valid 
  @JsonProperty("parentCourseId")
  public @Nullable UUID getParentCourseId() {
    return parentCourseId;
  }

  @JsonProperty("parentCourseId")
  public void setParentCourseId(@Nullable UUID parentCourseId) {
    this.parentCourseId = parentCourseId;
  }

  public CourseUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Course code.
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

  public CourseUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Course name.
   * @return name
   */
  @Size(max = 200) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public CourseUpdate evaluationType(@Nullable EvaluationTypeEnum evaluationType) {
    this.evaluationType = evaluationType;
    return this;
  }

  /**
   * Evaluation scheme.
   * @return evaluationType
   */
  
  @JsonProperty("evaluationType")
  public @Nullable EvaluationTypeEnum getEvaluationType() {
    return evaluationType;
  }

  @JsonProperty("evaluationType")
  public void setEvaluationType(@Nullable EvaluationTypeEnum evaluationType) {
    this.evaluationType = evaluationType;
  }

  public CourseUpdate minUnitLoad(@Nullable String minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
    return this;
  }

  /**
   * Minimum credit load.
   * @return minUnitLoad
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("minUnitLoad")
  public @Nullable String getMinUnitLoad() {
    return minUnitLoad;
  }

  @JsonProperty("minUnitLoad")
  public void setMinUnitLoad(@Nullable String minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
  }

  public CourseUpdate maxUnitLoad(@Nullable String maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
    return this;
  }

  /**
   * Maximum credit load.
   * @return maxUnitLoad
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("maxUnitLoad")
  public @Nullable String getMaxUnitLoad() {
    return maxUnitLoad;
  }

  @JsonProperty("maxUnitLoad")
  public void setMaxUnitLoad(@Nullable String maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
  }

  public CourseUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether offered.
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
    CourseUpdate courseUpdate = (CourseUpdate) o;
    return Objects.equals(this.institutionId, courseUpdate.institutionId) &&
        Objects.equals(this.programId, courseUpdate.programId) &&
        Objects.equals(this.departmentId, courseUpdate.departmentId) &&
        Objects.equals(this.parentCourseId, courseUpdate.parentCourseId) &&
        Objects.equals(this.code, courseUpdate.code) &&
        Objects.equals(this.name, courseUpdate.name) &&
        Objects.equals(this.evaluationType, courseUpdate.evaluationType) &&
        Objects.equals(this.minUnitLoad, courseUpdate.minUnitLoad) &&
        Objects.equals(this.maxUnitLoad, courseUpdate.maxUnitLoad) &&
        Objects.equals(this.isActive, courseUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, programId, departmentId, parentCourseId, code, name, evaluationType, minUnitLoad, maxUnitLoad, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    programId: ").append(toIndentedString(programId)).append("\n");
    sb.append("    departmentId: ").append(toIndentedString(departmentId)).append("\n");
    sb.append("    parentCourseId: ").append(toIndentedString(parentCourseId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    evaluationType: ").append(toIndentedString(evaluationType)).append("\n");
    sb.append("    minUnitLoad: ").append(toIndentedString(minUnitLoad)).append("\n");
    sb.append("    maxUnitLoad: ").append(toIndentedString(maxUnitLoad)).append("\n");
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

