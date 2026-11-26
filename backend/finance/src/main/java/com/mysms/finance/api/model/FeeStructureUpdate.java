// Owned API contract of finance.
package com.mysms.finance.api.model;

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
 * Fields of FeeStructure that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class FeeStructureUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> feePlanId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  public FeeStructureUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide).
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

  public FeeStructureUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year the structure prices.
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

  public FeeStructureUpdate gradeLevelId(@Nullable Optional<UUID> gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * K-12 grade level (class) the structure applies to.
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

  public FeeStructureUpdate programId(@Nullable Optional<UUID> programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Higher-ed program the structure applies to.
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

  public FeeStructureUpdate feePlanId(@Nullable Optional<UUID> feePlanId) {
    this.feePlanId = feePlanId;
    return this;
  }

  /**
   * Fee plan (heads + installments) charged.
   * @return feePlanId
   */
  
  @JsonProperty("feePlanId")
  public @Nullable Optional<UUID> getFeePlanId() {
    return feePlanId;
  }

  @JsonProperty("feePlanId")
  public void setFeePlanId(@Nullable Optional<UUID> feePlanId) {
    this.feePlanId = feePlanId;
  }

  public FeeStructureUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Admin-facing name.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeeStructureUpdate feeStructureUpdate = (FeeStructureUpdate) o;
    return Objects.equals(this.institutionId, feeStructureUpdate.institutionId) &&
        Objects.equals(this.academicYearId, feeStructureUpdate.academicYearId) &&
        Objects.equals(this.gradeLevelId, feeStructureUpdate.gradeLevelId) &&
        Objects.equals(this.programId, feeStructureUpdate.programId) &&
        Objects.equals(this.feePlanId, feeStructureUpdate.feePlanId) &&
        Objects.equals(this.name, feeStructureUpdate.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, gradeLevelId, programId, feePlanId, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeeStructureUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    gradeLevelId: ").append(toIndentedString(gradeLevelId)).append("\n");
    sb.append("    programId: ").append(toIndentedString(programId)).append("\n");
    sb.append("    feePlanId: ").append(toIndentedString(feePlanId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

