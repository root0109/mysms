// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

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
 * Fields of GradingScheme that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class GradingSchemeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  /**
   * Scholastic or co-scholastic.
   */
  public enum SchemeTypeEnum {
    SCHOLASTIC("scholastic"),
    
    CO_SCHOLASTIC("co_scholastic");

    private final String value;

    SchemeTypeEnum(String value) {
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
    public static SchemeTypeEnum fromValue(String value) {
      for (SchemeTypeEnum b : SchemeTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull SchemeTypeEnum> schemeType;

  /**
   * Method.
   */
  public enum GradingMethodEnum {
    PERCENTAGE_BAND("percentage_band"),
    
    CBSE("cbse"),
    
    PASS_FAIL("pass_fail"),
    
    DESCRIPTIVE("descriptive");

    private final String value;

    GradingMethodEnum(String value) {
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
    public static GradingMethodEnum fromValue(String value) {
      for (GradingMethodEnum b : GradingMethodEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull GradingMethodEnum> gradingMethod;

  private @Nullable Optional<Object> criteria;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isTemplate;

  public GradingSchemeUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public GradingSchemeUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year the scheme applies to (NULL = all years).
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

  public GradingSchemeUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Scheme name.
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

  public GradingSchemeUpdate schemeType(@Nullable Optional<SchemeTypeEnum> schemeType) {
    this.schemeType = schemeType;
    return this;
  }

  /**
   * Scholastic or co-scholastic.
   * @return schemeType
   */
  
  @JsonProperty("schemeType")
  public @Nullable Optional<SchemeTypeEnum> getSchemeType() {
    return schemeType;
  }

  @JsonProperty("schemeType")
  public void setSchemeType(@Nullable Optional<SchemeTypeEnum> schemeType) {
    this.schemeType = schemeType;
  }

  public GradingSchemeUpdate gradingMethod(@Nullable Optional<GradingMethodEnum> gradingMethod) {
    this.gradingMethod = gradingMethod;
    return this;
  }

  /**
   * Method.
   * @return gradingMethod
   */
  
  @JsonProperty("gradingMethod")
  public @Nullable Optional<GradingMethodEnum> getGradingMethod() {
    return gradingMethod;
  }

  @JsonProperty("gradingMethod")
  public void setGradingMethod(@Nullable Optional<GradingMethodEnum> gradingMethod) {
    this.gradingMethod = gradingMethod;
  }

  public GradingSchemeUpdate criteria(@Nullable Optional<Object> criteria) {
    this.criteria = criteria;
    return this;
  }

  /**
   * Co-scholastic indicators / areas assessed.
   * @return criteria
   */
  
  @JsonProperty("criteria")
  public @Nullable Optional<Object> getCriteria() {
    return criteria;
  }

  @JsonProperty("criteria")
  public void setCriteria(@Nullable Optional<Object> criteria) {
    this.criteria = criteria;
  }

  public GradingSchemeUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
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

  public GradingSchemeUpdate isTemplate(@Nullable Optional<Boolean> isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  /**
   * Reusable template copied into courses and cohorts.
   * @return isTemplate
   */
  
  @JsonProperty("isTemplate")
  public @Nullable Optional<Boolean> getIsTemplate() {
    return isTemplate;
  }

  @JsonProperty("isTemplate")
  public void setIsTemplate(@Nullable Optional<Boolean> isTemplate) {
    this.isTemplate = isTemplate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradingSchemeUpdate gradingSchemeUpdate = (GradingSchemeUpdate) o;
    return Objects.equals(this.institutionId, gradingSchemeUpdate.institutionId) &&
        Objects.equals(this.academicYearId, gradingSchemeUpdate.academicYearId) &&
        Objects.equals(this.name, gradingSchemeUpdate.name) &&
        Objects.equals(this.schemeType, gradingSchemeUpdate.schemeType) &&
        Objects.equals(this.gradingMethod, gradingSchemeUpdate.gradingMethod) &&
        Objects.equals(this.criteria, gradingSchemeUpdate.criteria) &&
        Objects.equals(this.isActive, gradingSchemeUpdate.isActive) &&
        Objects.equals(this.isTemplate, gradingSchemeUpdate.isTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, name, schemeType, gradingMethod, criteria, isActive, isTemplate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradingSchemeUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    schemeType: ").append(toIndentedString(schemeType)).append("\n");
    sb.append("    gradingMethod: ").append(toIndentedString(gradingMethod)).append("\n");
    sb.append("    criteria: ").append(toIndentedString(criteria)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    isTemplate: ").append(toIndentedString(isTemplate)).append("\n");
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

