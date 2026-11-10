// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AdmissionCycle that can be updated (partial update; see updateMask).
 */

public class AdmissionCycleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  /**
   * What applicants apply for.
   */
  public enum AdmissionBasisEnum {
    PROGRAM("program"),
    
    COURSE("course"),
    
    GRADE_LEVEL("grade_level");

    private final String value;

    AdmissionBasisEnum(String value) {
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
    public static AdmissionBasisEnum fromValue(String value) {
      for (AdmissionBasisEnum b : AdmissionBasisEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AdmissionBasisEnum admissionBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer minApplications;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxApplications;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer minimumAgeYears;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID applicationFeeHeadId;

  public AdmissionCycleUpdate institutionId(@Nullable UUID institutionId) {
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

  public AdmissionCycleUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Cycle name.
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

  public AdmissionCycleUpdate admissionBasis(@Nullable AdmissionBasisEnum admissionBasis) {
    this.admissionBasis = admissionBasis;
    return this;
  }

  /**
   * What applicants apply for.
   * @return admissionBasis
   */
  
  @JsonProperty("admissionBasis")
  public @Nullable AdmissionBasisEnum getAdmissionBasis() {
    return admissionBasis;
  }

  @JsonProperty("admissionBasis")
  public void setAdmissionBasis(@Nullable AdmissionBasisEnum admissionBasis) {
    this.admissionBasis = admissionBasis;
  }

  public AdmissionCycleUpdate programId(@Nullable UUID programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program (basis = program).
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

  public AdmissionCycleUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (basis = course).
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

  public AdmissionCycleUpdate gradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * Grade (basis = grade_level).
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

  public AdmissionCycleUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public AdmissionCycleUpdate academicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Term.
   * @return academicTermId
   */
  @Valid 
  @JsonProperty("academicTermId")
  public @Nullable UUID getAcademicTermId() {
    return academicTermId;
  }

  @JsonProperty("academicTermId")
  public void setAcademicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
  }

  public AdmissionCycleUpdate startDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Opens.
   * @return startDate
   */
  @Valid 
  @JsonProperty("startDate")
  public @Nullable LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
  }

  public AdmissionCycleUpdate endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Closes.
   * @return endDate
   */
  @Valid 
  @JsonProperty("endDate")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public AdmissionCycleUpdate minApplications(@Nullable Integer minApplications) {
    this.minApplications = minApplications;
    return this;
  }

  /**
   * Minimum applications to run the cycle.
   * @return minApplications
   */
  
  @JsonProperty("minApplications")
  public @Nullable Integer getMinApplications() {
    return minApplications;
  }

  @JsonProperty("minApplications")
  public void setMinApplications(@Nullable Integer minApplications) {
    this.minApplications = minApplications;
  }

  public AdmissionCycleUpdate maxApplications(@Nullable Integer maxApplications) {
    this.maxApplications = maxApplications;
    return this;
  }

  /**
   * Seat / application cap.
   * @return maxApplications
   */
  
  @JsonProperty("maxApplications")
  public @Nullable Integer getMaxApplications() {
    return maxApplications;
  }

  @JsonProperty("maxApplications")
  public void setMaxApplications(@Nullable Integer maxApplications) {
    this.maxApplications = maxApplications;
  }

  public AdmissionCycleUpdate minimumAgeYears(@Nullable Integer minimumAgeYears) {
    this.minimumAgeYears = minimumAgeYears;
    return this;
  }

  /**
   * Minimum age at admission.
   * @return minimumAgeYears
   */
  
  @JsonProperty("minimumAgeYears")
  public @Nullable Integer getMinimumAgeYears() {
    return minimumAgeYears;
  }

  @JsonProperty("minimumAgeYears")
  public void setMinimumAgeYears(@Nullable Integer minimumAgeYears) {
    this.minimumAgeYears = minimumAgeYears;
  }

  public AdmissionCycleUpdate applicationFeeHeadId(@Nullable UUID applicationFeeHeadId) {
    this.applicationFeeHeadId = applicationFeeHeadId;
    return this;
  }

  /**
   * Fee head charged on application.
   * @return applicationFeeHeadId
   */
  @Valid 
  @JsonProperty("applicationFeeHeadId")
  public @Nullable UUID getApplicationFeeHeadId() {
    return applicationFeeHeadId;
  }

  @JsonProperty("applicationFeeHeadId")
  public void setApplicationFeeHeadId(@Nullable UUID applicationFeeHeadId) {
    this.applicationFeeHeadId = applicationFeeHeadId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdmissionCycleUpdate admissionCycleUpdate = (AdmissionCycleUpdate) o;
    return Objects.equals(this.institutionId, admissionCycleUpdate.institutionId) &&
        Objects.equals(this.name, admissionCycleUpdate.name) &&
        Objects.equals(this.admissionBasis, admissionCycleUpdate.admissionBasis) &&
        Objects.equals(this.programId, admissionCycleUpdate.programId) &&
        Objects.equals(this.courseId, admissionCycleUpdate.courseId) &&
        Objects.equals(this.gradeLevelId, admissionCycleUpdate.gradeLevelId) &&
        Objects.equals(this.academicYearId, admissionCycleUpdate.academicYearId) &&
        Objects.equals(this.academicTermId, admissionCycleUpdate.academicTermId) &&
        Objects.equals(this.startDate, admissionCycleUpdate.startDate) &&
        Objects.equals(this.endDate, admissionCycleUpdate.endDate) &&
        Objects.equals(this.minApplications, admissionCycleUpdate.minApplications) &&
        Objects.equals(this.maxApplications, admissionCycleUpdate.maxApplications) &&
        Objects.equals(this.minimumAgeYears, admissionCycleUpdate.minimumAgeYears) &&
        Objects.equals(this.applicationFeeHeadId, admissionCycleUpdate.applicationFeeHeadId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, name, admissionBasis, programId, courseId, gradeLevelId, academicYearId, academicTermId, startDate, endDate, minApplications, maxApplications, minimumAgeYears, applicationFeeHeadId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdmissionCycleUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    admissionBasis: ").append(toIndentedString(admissionBasis)).append("\n");
    sb.append("    programId: ").append(toIndentedString(programId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    gradeLevelId: ").append(toIndentedString(gradeLevelId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    academicTermId: ").append(toIndentedString(academicTermId)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    minApplications: ").append(toIndentedString(minApplications)).append("\n");
    sb.append("    maxApplications: ").append(toIndentedString(maxApplications)).append("\n");
    sb.append("    minimumAgeYears: ").append(toIndentedString(minimumAgeYears)).append("\n");
    sb.append("    applicationFeeHeadId: ").append(toIndentedString(applicationFeeHeadId)).append("\n");
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

