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
 * Fields of Program that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ProgramUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> departmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> programLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> minUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> maxUnitLoad;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public ProgramUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public ProgramUpdate departmentId(@Nullable Optional<UUID> departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  /**
   * Owning department.
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

  public ProgramUpdate programLevelId(@Nullable Optional<UUID> programLevelId) {
    this.programLevelId = programLevelId;
    return this;
  }

  /**
   * Level.
   * @return programLevelId
   */
  
  @JsonProperty("programLevelId")
  public @Nullable Optional<UUID> getProgramLevelId() {
    return programLevelId;
  }

  @JsonProperty("programLevelId")
  public void setProgramLevelId(@Nullable Optional<UUID> programLevelId) {
    this.programLevelId = programLevelId;
  }

  public ProgramUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Program code.
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

  public ProgramUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Program name.
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

  public ProgramUpdate minUnitLoad(@Nullable Optional<String> minUnitLoad) {
    this.minUnitLoad = minUnitLoad;
    return this;
  }

  /**
   * Minimum credit load per term.
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

  public ProgramUpdate maxUnitLoad(@Nullable Optional<String> maxUnitLoad) {
    this.maxUnitLoad = maxUnitLoad;
    return this;
  }

  /**
   * Maximum credit load per term.
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

  public ProgramUpdate imageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Program image.
   * @return imageFileId
   */
  
  @JsonProperty("imageFileId")
  public @Nullable Optional<UUID> getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
  }

  public ProgramUpdate isActive(@Nullable Optional<Boolean> isActive) {
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

