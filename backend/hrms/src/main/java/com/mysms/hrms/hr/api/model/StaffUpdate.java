// Owned API contract of hr.
package com.mysms.hrms.hr.api.model;

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
 * Fields of Staff that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StaffUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> employeeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> designationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> subDesignation;

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
  private @Nullable Optional<@NotNull EmploymentTypeEnum> employmentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> joiningDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> reportingToStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> primaryDepartmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> highestQualificationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> qualificationDetail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> specialization;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> interests;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxPeriodsPerWeek;

  public StaffUpdate personId(@Nullable Optional<UUID> personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person.
   * @return personId
   */
  
  @JsonProperty("personId")
  public @Nullable Optional<UUID> getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(@Nullable Optional<UUID> personId) {
    this.personId = personId;
  }

  public StaffUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Staff login account.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public StaffUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Employing institution.
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

  public StaffUpdate employeeCode(@Nullable Optional<String> employeeCode) {
    this.employeeCode = employeeCode;
    return this;
  }

  /**
   * Employee number.
   * @return employeeCode
   */
  
  @JsonProperty("employeeCode")
  public @Nullable Optional<String> getEmployeeCode() {
    return employeeCode;
  }

  @JsonProperty("employeeCode")
  public void setEmployeeCode(@Nullable Optional<String> employeeCode) {
    this.employeeCode = employeeCode;
  }

  public StaffUpdate designationId(@Nullable Optional<UUID> designationId) {
    this.designationId = designationId;
    return this;
  }

  /**
   * Designation.
   * @return designationId
   */
  
  @JsonProperty("designationId")
  public @Nullable Optional<UUID> getDesignationId() {
    return designationId;
  }

  @JsonProperty("designationId")
  public void setDesignationId(@Nullable Optional<UUID> designationId) {
    this.designationId = designationId;
  }

  public StaffUpdate subDesignation(@Nullable Optional<String> subDesignation) {
    this.subDesignation = subDesignation;
    return this;
  }

  /**
   * Designation detail.
   * @return subDesignation
   */
  
  @JsonProperty("subDesignation")
  public @Nullable Optional<String> getSubDesignation() {
    return subDesignation;
  }

  @JsonProperty("subDesignation")
  public void setSubDesignation(@Nullable Optional<String> subDesignation) {
    this.subDesignation = subDesignation;
  }

  public StaffUpdate employmentType(@Nullable Optional<EmploymentTypeEnum> employmentType) {
    this.employmentType = employmentType;
    return this;
  }

  /**
   * Employment type.
   * @return employmentType
   */
  
  @JsonProperty("employmentType")
  public @Nullable Optional<EmploymentTypeEnum> getEmploymentType() {
    return employmentType;
  }

  @JsonProperty("employmentType")
  public void setEmploymentType(@Nullable Optional<EmploymentTypeEnum> employmentType) {
    this.employmentType = employmentType;
  }

  public StaffUpdate joiningDate(@Nullable Optional<LocalDate> joiningDate) {
    this.joiningDate = joiningDate;
    return this;
  }

  /**
   * Date of joining.
   * @return joiningDate
   */
  
  @JsonProperty("joiningDate")
  public @Nullable Optional<LocalDate> getJoiningDate() {
    return joiningDate;
  }

  @JsonProperty("joiningDate")
  public void setJoiningDate(@Nullable Optional<LocalDate> joiningDate) {
    this.joiningDate = joiningDate;
  }

  public StaffUpdate reportingToStaffId(@Nullable Optional<UUID> reportingToStaffId) {
    this.reportingToStaffId = reportingToStaffId;
    return this;
  }

  /**
   * Line manager (any user account, staff or administrator).
   * @return reportingToStaffId
   */
  
  @JsonProperty("reportingToStaffId")
  public @Nullable Optional<UUID> getReportingToStaffId() {
    return reportingToStaffId;
  }

  @JsonProperty("reportingToStaffId")
  public void setReportingToStaffId(@Nullable Optional<UUID> reportingToStaffId) {
    this.reportingToStaffId = reportingToStaffId;
  }

  public StaffUpdate primaryDepartmentId(@Nullable Optional<UUID> primaryDepartmentId) {
    this.primaryDepartmentId = primaryDepartmentId;
    return this;
  }

  /**
   * Main department.
   * @return primaryDepartmentId
   */
  
  @JsonProperty("primaryDepartmentId")
  public @Nullable Optional<UUID> getPrimaryDepartmentId() {
    return primaryDepartmentId;
  }

  @JsonProperty("primaryDepartmentId")
  public void setPrimaryDepartmentId(@Nullable Optional<UUID> primaryDepartmentId) {
    this.primaryDepartmentId = primaryDepartmentId;
  }

  public StaffUpdate highestQualificationId(@Nullable Optional<UUID> highestQualificationId) {
    this.highestQualificationId = highestQualificationId;
    return this;
  }

  /**
   * Highest qualification.
   * @return highestQualificationId
   */
  
  @JsonProperty("highestQualificationId")
  public @Nullable Optional<UUID> getHighestQualificationId() {
    return highestQualificationId;
  }

  @JsonProperty("highestQualificationId")
  public void setHighestQualificationId(@Nullable Optional<UUID> highestQualificationId) {
    this.highestQualificationId = highestQualificationId;
  }

  public StaffUpdate qualificationDetail(@Nullable Optional<String> qualificationDetail) {
    this.qualificationDetail = qualificationDetail;
    return this;
  }

  /**
   * Qualification detail.
   * @return qualificationDetail
   */
  
  @JsonProperty("qualificationDetail")
  public @Nullable Optional<String> getQualificationDetail() {
    return qualificationDetail;
  }

  @JsonProperty("qualificationDetail")
  public void setQualificationDetail(@Nullable Optional<String> qualificationDetail) {
    this.qualificationDetail = qualificationDetail;
  }

  public StaffUpdate specialization(@Nullable Optional<String> specialization) {
    this.specialization = specialization;
    return this;
  }

  /**
   * Specialisation.
   * @return specialization
   */
  
  @JsonProperty("specialization")
  public @Nullable Optional<String> getSpecialization() {
    return specialization;
  }

  @JsonProperty("specialization")
  public void setSpecialization(@Nullable Optional<String> specialization) {
    this.specialization = specialization;
  }

  public StaffUpdate interests(@Nullable Optional<String> interests) {
    this.interests = interests;
    return this;
  }

  /**
   * Interests / extra responsibilities.
   * @return interests
   */
  
  @JsonProperty("interests")
  public @Nullable Optional<String> getInterests() {
    return interests;
  }

  @JsonProperty("interests")
  public void setInterests(@Nullable Optional<String> interests) {
    this.interests = interests;
  }

  public StaffUpdate maxPeriodsPerWeek(@Nullable Optional<Integer> maxPeriodsPerWeek) {
    this.maxPeriodsPerWeek = maxPeriodsPerWeek;
    return this;
  }

  /**
   * Teaching-load ceiling in periods.
   * @return maxPeriodsPerWeek
   */
  
  @JsonProperty("maxPeriodsPerWeek")
  public @Nullable Optional<Integer> getMaxPeriodsPerWeek() {
    return maxPeriodsPerWeek;
  }

  @JsonProperty("maxPeriodsPerWeek")
  public void setMaxPeriodsPerWeek(@Nullable Optional<Integer> maxPeriodsPerWeek) {
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

