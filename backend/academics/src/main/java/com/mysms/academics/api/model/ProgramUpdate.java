// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
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
 * Fields of Program that can be updated (partial update; see updateMask).
 */

public class ProgramUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID departmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String minUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public ProgramUpdate institutionId(@Nullable UUID institutionId) {
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

  public ProgramUpdate departmentId(@Nullable UUID departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  /**
   * Owning department.
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

  public ProgramUpdate programLevelId(@Nullable UUID programLevelId) {
    this.programLevelId = programLevelId;
    return this;
  }

  /**
   * Level.
   * @return programLevelId
   */
  @Valid 
  @JsonProperty("programLevelId")
  public @Nullable UUID getProgramLevelId() {
    return programLevelId;
  }

  @JsonProperty("programLevelId")
  public void setProgramLevelId(@Nullable UUID programLevelId) {
    this.programLevelId = programLevelId;
  }

  public ProgramUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Program code.
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

  public ProgramUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Program name.
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

  public ProgramUpdate minUnitLoad(@Nullable String minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
    return this;
  }

  /**
   * Minimum credit load per term.
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

  public ProgramUpdate maxUnitLoad(@Nullable String maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
    return this;
  }

  /**
   * Maximum credit load per term.
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

  public ProgramUpdate imageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Program image.
   * @return imageFileId
   */
  @Valid 
  @JsonProperty("imageFileId")
  public @Nullable UUID getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
  }

  public ProgramUpdate isActive(@Nullable Boolean isActive) {
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
    ProgramUpdate programUpdate = (ProgramUpdate) o;
    return Objects.equals(this.institutionId, programUpdate.institutionId) &&
        Objects.equals(this.departmentId, programUpdate.departmentId) &&
        Objects.equals(this.programLevelId, programUpdate.programLevelId) &&
        Objects.equals(this.code, programUpdate.code) &&
        Objects.equals(this.name, programUpdate.name) &&
        Objects.equals(this.minUnitLoad, programUpdate.minUnitLoad) &&
        Objects.equals(this.maxUnitLoad, programUpdate.maxUnitLoad) &&
        Objects.equals(this.imageFileId, programUpdate.imageFileId) &&
        Objects.equals(this.isActive, programUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, departmentId, programLevelId, code, name, minUnitLoad, maxUnitLoad, imageFileId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    departmentId: ").append(toIndentedString(departmentId)).append("\n");
    sb.append("    programLevelId: ").append(toIndentedString(programLevelId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    minUnitLoad: ").append(toIndentedString(minUnitLoad)).append("\n");
    sb.append("    maxUnitLoad: ").append(toIndentedString(maxUnitLoad)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
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

