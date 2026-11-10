// Owned API contract of hr (JAVA-CODING-RULES §7); first materialized from api/openapi/hr.yaml on 2026-11-10.
package com.mysms.hr.api.model;

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
 * Fields of Staff that can be updated (partial update; see updateMask).
 */

public class StaffUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String employeeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID designationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subDesignation;

  /**
   * Employment type.
   */
  public enum EmploymentTypeEnum {
    FULL_TIME("full_time"),
    
    PART_TIME("part_time"),
    
    CONTRACT("contract"),
    
    VISITING("visiting");

    private final String value;

    EmploymentTypeEnum(String value) {
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
    public static EmploymentTypeEnum fromValue(String value) {
      for (EmploymentTypeEnum b : EmploymentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EmploymentTypeEnum employmentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate joiningDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reportingToStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID primaryDepartmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID highestQualificationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String qualificationDetail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String specialization;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String interests;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxPeriodsPerWeek;

  public StaffUpdate personId(@Nullable UUID personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person.
   * @return personId
   */
  @Valid 
  @JsonProperty("personId")
  public @Nullable UUID getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(@Nullable UUID personId) {
    this.personId = personId;
  }

  public StaffUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Staff login account.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public StaffUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Employing institution.
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

  public StaffUpdate employeeCode(@Nullable String employeeCode) {
    this.employeeCode = employeeCode;
    return this;
  }

  /**
   * Employee number.
   * @return employeeCode
   */
  @Size(max = 30) 
  @JsonProperty("employeeCode")
  public @Nullable String getEmployeeCode() {
    return employeeCode;
  }

  @JsonProperty("employeeCode")
  public void setEmployeeCode(@Nullable String employeeCode) {
    this.employeeCode = employeeCode;
  }

  public StaffUpdate designationId(@Nullable UUID designationId) {
    this.designationId = designationId;
    return this;
  }

  /**
   * Designation.
   * @return designationId
   */
  @Valid 
  @JsonProperty("designationId")
  public @Nullable UUID getDesignationId() {
    return designationId;
  }

  @JsonProperty("designationId")
  public void setDesignationId(@Nullable UUID designationId) {
    this.designationId = designationId;
  }

  public StaffUpdate subDesignation(@Nullable String subDesignation) {
    this.subDesignation = subDesignation;
    return this;
  }

  /**
   * Designation detail.
   * @return subDesignation
   */
  @Size(max = 120) 
  @JsonProperty("subDesignation")
  public @Nullable String getSubDesignation() {
    return subDesignation;
  }

  @JsonProperty("subDesignation")
  public void setSubDesignation(@Nullable String subDesignation) {
    this.subDesignation = subDesignation;
  }

  public StaffUpdate employmentType(@Nullable EmploymentTypeEnum employmentType) {
    this.employmentType = employmentType;
    return this;
  }

  /**
   * Employment type.
   * @return employmentType
   */
  
  @JsonProperty("employmentType")
  public @Nullable EmploymentTypeEnum getEmploymentType() {
    return employmentType;
  }

  @JsonProperty("employmentType")
  public void setEmploymentType(@Nullable EmploymentTypeEnum employmentType) {
    this.employmentType = employmentType;
  }

  public StaffUpdate joiningDate(@Nullable LocalDate joiningDate) {
    this.joiningDate = joiningDate;
    return this;
  }

  /**
   * Date of joining.
   * @return joiningDate
   */
  @Valid 
  @JsonProperty("joiningDate")
  public @Nullable LocalDate getJoiningDate() {
    return joiningDate;
  }

  @JsonProperty("joiningDate")
  public void setJoiningDate(@Nullable LocalDate joiningDate) {
    this.joiningDate = joiningDate;
  }

  public StaffUpdate reportingToStaffId(@Nullable UUID reportingToStaffId) {
    this.reportingToStaffId = reportingToStaffId;
    return this;
  }

  /**
   * Line manager (any user account, staff or administrator).
   * @return reportingToStaffId
   */
  @Valid 
  @JsonProperty("reportingToStaffId")
  public @Nullable UUID getReportingToStaffId() {
    return reportingToStaffId;
  }

  @JsonProperty("reportingToStaffId")
  public void setReportingToStaffId(@Nullable UUID reportingToStaffId) {
    this.reportingToStaffId = reportingToStaffId;
  }

  public StaffUpdate primaryDepartmentId(@Nullable UUID primaryDepartmentId) {
    this.primaryDepartmentId = primaryDepartmentId;
    return this;
  }

  /**
   * Main department.
   * @return primaryDepartmentId
   */
  @Valid 
  @JsonProperty("primaryDepartmentId")
  public @Nullable UUID getPrimaryDepartmentId() {
    return primaryDepartmentId;
  }

  @JsonProperty("primaryDepartmentId")
  public void setPrimaryDepartmentId(@Nullable UUID primaryDepartmentId) {
    this.primaryDepartmentId = primaryDepartmentId;
  }

  public StaffUpdate highestQualificationId(@Nullable UUID highestQualificationId) {
    this.highestQualificationId = highestQualificationId;
    return this;
  }

  /**
   * Highest qualification.
   * @return highestQualificationId
   */
  @Valid 
  @JsonProperty("highestQualificationId")
  public @Nullable UUID getHighestQualificationId() {
    return highestQualificationId;
  }

  @JsonProperty("highestQualificationId")
  public void setHighestQualificationId(@Nullable UUID highestQualificationId) {
    this.highestQualificationId = highestQualificationId;
  }

  public StaffUpdate qualificationDetail(@Nullable String qualificationDetail) {
    this.qualificationDetail = qualificationDetail;
    return this;
  }

  /**
   * Qualification detail.
   * @return qualificationDetail
   */
  @Size(max = 150) 
  @JsonProperty("qualificationDetail")
  public @Nullable String getQualificationDetail() {
    return qualificationDetail;
  }

  @JsonProperty("qualificationDetail")
  public void setQualificationDetail(@Nullable String qualificationDetail) {
    this.qualificationDetail = qualificationDetail;
  }

  public StaffUpdate specialization(@Nullable String specialization) {
    this.specialization = specialization;
    return this;
  }

  /**
   * Specialisation.
   * @return specialization
   */
  @Size(max = 150) 
  @JsonProperty("specialization")
  public @Nullable String getSpecialization() {
    return specialization;
  }

  @JsonProperty("specialization")
  public void setSpecialization(@Nullable String specialization) {
    this.specialization = specialization;
  }

  public StaffUpdate interests(@Nullable String interests) {
    this.interests = interests;
    return this;
  }

  /**
   * Interests / extra responsibilities.
   * @return interests
   */
  
  @JsonProperty("interests")
  public @Nullable String getInterests() {
    return interests;
  }

  @JsonProperty("interests")
  public void setInterests(@Nullable String interests) {
    this.interests = interests;
  }

  public StaffUpdate maxPeriodsPerWeek(@Nullable Integer maxPeriodsPerWeek) {
    this.maxPeriodsPerWeek = maxPeriodsPerWeek;
    return this;
  }

  /**
   * Teaching-load ceiling in periods.
   * @return maxPeriodsPerWeek
   */
  
  @JsonProperty("maxPeriodsPerWeek")
  public @Nullable Integer getMaxPeriodsPerWeek() {
    return maxPeriodsPerWeek;
  }

  @JsonProperty("maxPeriodsPerWeek")
  public void setMaxPeriodsPerWeek(@Nullable Integer maxPeriodsPerWeek) {
    this.maxPeriodsPerWeek = maxPeriodsPerWeek;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffUpdate staffUpdate = (StaffUpdate) o;
    return Objects.equals(this.personId, staffUpdate.personId) &&
        Objects.equals(this.userId, staffUpdate.userId) &&
        Objects.equals(this.institutionId, staffUpdate.institutionId) &&
        Objects.equals(this.employeeCode, staffUpdate.employeeCode) &&
        Objects.equals(this.designationId, staffUpdate.designationId) &&
        Objects.equals(this.subDesignation, staffUpdate.subDesignation) &&
        Objects.equals(this.employmentType, staffUpdate.employmentType) &&
        Objects.equals(this.joiningDate, staffUpdate.joiningDate) &&
        Objects.equals(this.reportingToStaffId, staffUpdate.reportingToStaffId) &&
        Objects.equals(this.primaryDepartmentId, staffUpdate.primaryDepartmentId) &&
        Objects.equals(this.highestQualificationId, staffUpdate.highestQualificationId) &&
        Objects.equals(this.qualificationDetail, staffUpdate.qualificationDetail) &&
        Objects.equals(this.specialization, staffUpdate.specialization) &&
        Objects.equals(this.interests, staffUpdate.interests) &&
        Objects.equals(this.maxPeriodsPerWeek, staffUpdate.maxPeriodsPerWeek);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personId, userId, institutionId, employeeCode, designationId, subDesignation, employmentType, joiningDate, reportingToStaffId, primaryDepartmentId, highestQualificationId, qualificationDetail, specialization, interests, maxPeriodsPerWeek);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffUpdate {\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    employeeCode: ").append(toIndentedString(employeeCode)).append("\n");
    sb.append("    designationId: ").append(toIndentedString(designationId)).append("\n");
    sb.append("    subDesignation: ").append(toIndentedString(subDesignation)).append("\n");
    sb.append("    employmentType: ").append(toIndentedString(employmentType)).append("\n");
    sb.append("    joiningDate: ").append(toIndentedString(joiningDate)).append("\n");
    sb.append("    reportingToStaffId: ").append(toIndentedString(reportingToStaffId)).append("\n");
    sb.append("    primaryDepartmentId: ").append(toIndentedString(primaryDepartmentId)).append("\n");
    sb.append("    highestQualificationId: ").append(toIndentedString(highestQualificationId)).append("\n");
    sb.append("    qualificationDetail: ").append(toIndentedString(qualificationDetail)).append("\n");
    sb.append("    specialization: ").append(toIndentedString(specialization)).append("\n");
    sb.append("    interests: ").append(toIndentedString(interests)).append("\n");
    sb.append("    maxPeriodsPerWeek: ").append(toIndentedString(maxPeriodsPerWeek)).append("\n");
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

