// Owned API contract of admission.
package com.mysms.students.admission.api.model;

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
 * Fields of AdmissionCycle that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AdmissionCycleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

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
  private @Nullable Optional<@NotNull AdmissionBasisEnum> admissionBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> programId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> minApplications;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxApplications;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> minimumAgeYears;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> applicationFeeHeadId;

  public AdmissionCycleUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public AdmissionCycleUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Cycle name.
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

  public AdmissionCycleUpdate admissionBasis(@Nullable Optional<AdmissionBasisEnum> admissionBasis) {
    this.admissionBasis = admissionBasis;
    return this;
  }

  /**
   * What applicants apply for.
   * @return admissionBasis
   */
  
  @JsonProperty("admissionBasis")
  public @Nullable Optional<AdmissionBasisEnum> getAdmissionBasis() {
    return admissionBasis;
  }

  @JsonProperty("admissionBasis")
  public void setAdmissionBasis(@Nullable Optional<AdmissionBasisEnum> admissionBasis) {
    this.admissionBasis = admissionBasis;
  }

  public AdmissionCycleUpdate programId(@Nullable Optional<UUID> programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Program (basis = program).
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

  public AdmissionCycleUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (basis = course).
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

  public AdmissionCycleUpdate gradeLevelId(@Nullable Optional<UUID> gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * Grade (basis = grade_level).
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

  public AdmissionCycleUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
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

  public AdmissionCycleUpdate academicTermId(@Nullable Optional<UUID> academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Term.
   * @return academicTermId
   */
  
  @JsonProperty("academicTermId")
  public @Nullable Optional<UUID> getAcademicTermId() {
    return academicTermId;
  }

  @JsonProperty("academicTermId")
  public void setAcademicTermId(@Nullable Optional<UUID> academicTermId) {
    this.academicTermId = academicTermId;
  }

  public AdmissionCycleUpdate startDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Opens.
   * @return startDate
   */
  
  @JsonProperty("startDate")
  public @Nullable Optional<LocalDate> getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
  }

  public AdmissionCycleUpdate endDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Closes.
   * @return endDate
   */
  
  @JsonProperty("endDate")
  public @Nullable Optional<LocalDate> getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
  }

  public AdmissionCycleUpdate minApplications(@Nullable Optional<Integer> minApplications) {
    this.minApplications = minApplications;
    return this;
  }

  /**
   * Minimum applications to run the cycle.
   * @return minApplications
   */
  
  @JsonProperty("minApplications")
  public @Nullable Optional<Integer> getMinApplications() {
    return minApplications;
  }

  @JsonProperty("minApplications")
  public void setMinApplications(@Nullable Optional<Integer> minApplications) {
    this.minApplications = minApplications;
  }

  public AdmissionCycleUpdate maxApplications(@Nullable Optional<Integer> maxApplications) {
    this.maxApplications = maxApplications;
    return this;
  }

  /**
   * Seat / application cap.
   * @return maxApplications
   */
  
  @JsonProperty("maxApplications")
  public @Nullable Optional<Integer> getMaxApplications() {
    return maxApplications;
  }

  @JsonProperty("maxApplications")
  public void setMaxApplications(@Nullable Optional<Integer> maxApplications) {
    this.maxApplications = maxApplications;
  }

  public AdmissionCycleUpdate minimumAgeYears(@Nullable Optional<Integer> minimumAgeYears) {
    this.minimumAgeYears = minimumAgeYears;
    return this;
  }

  /**
   * Minimum age at admission.
   * @return minimumAgeYears
   */
  
  @JsonProperty("minimumAgeYears")
  public @Nullable Optional<Integer> getMinimumAgeYears() {
    return minimumAgeYears;
  }

  @JsonProperty("minimumAgeYears")
  public void setMinimumAgeYears(@Nullable Optional<Integer> minimumAgeYears) {
    this.minimumAgeYears = minimumAgeYears;
  }

  public AdmissionCycleUpdate applicationFeeHeadId(@Nullable Optional<UUID> applicationFeeHeadId) {
    this.applicationFeeHeadId = applicationFeeHeadId;
    return this;
  }

  /**
   * Fee head charged on application.
   * @return applicationFeeHeadId
   */
  
  @JsonProperty("applicationFeeHeadId")
  public @Nullable Optional<UUID> getApplicationFeeHeadId() {
    return applicationFeeHeadId;
  }

  @JsonProperty("applicationFeeHeadId")
  public void setApplicationFeeHeadId(@Nullable Optional<UUID> applicationFeeHeadId) {
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

