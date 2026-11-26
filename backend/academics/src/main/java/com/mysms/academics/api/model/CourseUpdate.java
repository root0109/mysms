// Owned API contract of academics.
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
 * Fields of Course that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CourseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> departmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> parentCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

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
  private @Nullable Optional<@NotNull EvaluationTypeEnum> evaluationType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> minUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> maxUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public CourseUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public CourseUpdate programId(@Nullable Optional<UUID> programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program.
   * @return programId
   */
  
  @JsonProperty("programId")
  public @Nullable Optional<UUID> getProgramId() {
    return programId;
  }

  @JsonProperty("programId")
  public void setProgramId(@Nullable Optional<UUID> programId) {
    this.programId = programId;
  }

  public CourseUpdate departmentId(@Nullable Optional<UUID> departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  /**
   * Department.
   * @return departmentId
   */
  
  @JsonProperty("departmentId")
  public @Nullable Optional<UUID> getDepartmentId() {
    return departmentId;
  }

  @JsonProperty("departmentId")
  public void setDepartmentId(@Nullable Optional<UUID> departmentId) {
    this.departmentId = departmentId;
  }

  public CourseUpdate parentCourseId(@Nullable Optional<UUID> parentCourseId) {
    this.parentCourseId = parentCourseId;
    return this;
  }

  /**
   * Parent course.
   * @return parentCourseId
   */
  
  @JsonProperty("parentCourseId")
  public @Nullable Optional<UUID> getParentCourseId() {
    return parentCourseId;
  }

  @JsonProperty("parentCourseId")
  public void setParentCourseId(@Nullable Optional<UUID> parentCourseId) {
    this.parentCourseId = parentCourseId;
  }

  public CourseUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Course code.
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

  public CourseUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Course name.
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

  public CourseUpdate evaluationType(@Nullable Optional<EvaluationTypeEnum> evaluationType) {
    this.evaluationType = evaluationType;
    return this;
  }

  /**
   * Evaluation scheme.
   * @return evaluationType
   */
  
  @JsonProperty("evaluationType")
  public @Nullable Optional<EvaluationTypeEnum> getEvaluationType() {
    return evaluationType;
  }

  @JsonProperty("evaluationType")
  public void setEvaluationType(@Nullable Optional<EvaluationTypeEnum> evaluationType) {
    this.evaluationType = evaluationType;
  }

  public CourseUpdate minUnitLoad(@Nullable Optional<String> minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
    return this;
  }

  /**
   * Minimum credit load.
   * @return minUnitLoad
   */
  
  @JsonProperty("minUnitLoad")
  public @Nullable Optional<String> getMinUnitLoad() {
    return minUnitLoad;
  }

  @JsonProperty("minUnitLoad")
  public void setMinUnitLoad(@Nullable Optional<String> minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
  }

  public CourseUpdate maxUnitLoad(@Nullable Optional<String> maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
    return this;
  }

  /**
   * Maximum credit load.
   * @return maxUnitLoad
   */
  
  @JsonProperty("maxUnitLoad")
  public @Nullable Optional<String> getMaxUnitLoad() {
    return maxUnitLoad;
  }

  @JsonProperty("maxUnitLoad")
  public void setMaxUnitLoad(@Nullable Optional<String> maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
  }

  public CourseUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether offered.
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

