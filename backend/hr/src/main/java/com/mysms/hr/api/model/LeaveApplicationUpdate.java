// Owned API contract of hr (JAVA-CODING-RULES §7); first materialized from api/openapi/hr.yaml on 2026-11-10.
package com.mysms.hr.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of LeaveApplication that can be updated (partial update; see updateMask).
 */

public class LeaveApplicationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID leaveTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID absenceReasonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime fromAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime toAt;

  /**
   * Part of day for single-day leave.
   */
  public enum DayPartEnum {
    FULL_DAY("full_day"),
    
    FORENOON("forenoon"),
    
    AFTERNOON("afternoon");

    private final String value;

    DayPartEnum(String value) {
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
    public static DayPartEnum fromValue(String value) {
      for (DayPartEnum b : DayPartEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DayPartEnum dayPart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String remarks;

  public LeaveApplicationUpdate institutionId(@Nullable UUID institutionId) {
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

  public LeaveApplicationUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public LeaveApplicationUpdate staffId(@Nullable UUID staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Applicant.
   * @return staffId
   */
  @Valid 
  @JsonProperty("staffId")
  public @Nullable UUID getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable UUID staffId) {
    this.staffId = staffId;
  }

  public LeaveApplicationUpdate leaveTypeId(@Nullable UUID leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
    return this;
  }

  /**
   * Leave type.
   * @return leaveTypeId
   */
  @Valid 
  @JsonProperty("leaveTypeId")
  public @Nullable UUID getLeaveTypeId() {
    return leaveTypeId;
  }

  @JsonProperty("leaveTypeId")
  public void setLeaveTypeId(@Nullable UUID leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
  }

  public LeaveApplicationUpdate absenceReasonId(@Nullable UUID absenceReasonId) {
    this.absenceReasonId = absenceReasonId;
    return this;
  }

  /**
   * Reason category.
   * @return absenceReasonId
   */
  @Valid 
  @JsonProperty("absenceReasonId")
  public @Nullable UUID getAbsenceReasonId() {
    return absenceReasonId;
  }

  @JsonProperty("absenceReasonId")
  public void setAbsenceReasonId(@Nullable UUID absenceReasonId) {
    this.absenceReasonId = absenceReasonId;
  }

  public LeaveApplicationUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class whose cover is affected.
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public LeaveApplicationUpdate fromAt(@Nullable OffsetDateTime fromAt) {
    this.fromAt = fromAt;
    return this;
  }

  /**
   * Leave start.
   * @return fromAt
   */
  @Valid 
  @JsonProperty("fromAt")
  public @Nullable OffsetDateTime getFromAt() {
    return fromAt;
  }

  @JsonProperty("fromAt")
  public void setFromAt(@Nullable OffsetDateTime fromAt) {
    this.fromAt = fromAt;
  }

  public LeaveApplicationUpdate toAt(@Nullable OffsetDateTime toAt) {
    this.toAt = toAt;
    return this;
  }

  /**
   * Leave end.
   * @return toAt
   */
  @Valid 
  @JsonProperty("toAt")
  public @Nullable OffsetDateTime getToAt() {
    return toAt;
  }

  @JsonProperty("toAt")
  public void setToAt(@Nullable OffsetDateTime toAt) {
    this.toAt = toAt;
  }

  public LeaveApplicationUpdate dayPart(@Nullable DayPartEnum dayPart) {
    this.dayPart = dayPart;
    return this;
  }

  /**
   * Part of day for single-day leave.
   * @return dayPart
   */
  
  @JsonProperty("dayPart")
  public @Nullable DayPartEnum getDayPart() {
    return dayPart;
  }

  @JsonProperty("dayPart")
  public void setDayPart(@Nullable DayPartEnum dayPart) {
    this.dayPart = dayPart;
  }

  public LeaveApplicationUpdate remarks(@Nullable String remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Applicant remarks.
   * @return remarks
   */
  
  @JsonProperty("remarks")
  public @Nullable String getRemarks() {
    return remarks;
  }

  @JsonProperty("remarks")
  public void setRemarks(@Nullable String remarks) {
    this.remarks = remarks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeaveApplicationUpdate leaveApplicationUpdate = (LeaveApplicationUpdate) o;
    return Objects.equals(this.institutionId, leaveApplicationUpdate.institutionId) &&
        Objects.equals(this.academicYearId, leaveApplicationUpdate.academicYearId) &&
        Objects.equals(this.staffId, leaveApplicationUpdate.staffId) &&
        Objects.equals(this.leaveTypeId, leaveApplicationUpdate.leaveTypeId) &&
        Objects.equals(this.absenceReasonId, leaveApplicationUpdate.absenceReasonId) &&
        Objects.equals(this.classSectionId, leaveApplicationUpdate.classSectionId) &&
        Objects.equals(this.fromAt, leaveApplicationUpdate.fromAt) &&
        Objects.equals(this.toAt, leaveApplicationUpdate.toAt) &&
        Objects.equals(this.dayPart, leaveApplicationUpdate.dayPart) &&
        Objects.equals(this.remarks, leaveApplicationUpdate.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, staffId, leaveTypeId, absenceReasonId, classSectionId, fromAt, toAt, dayPart, remarks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LeaveApplicationUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    leaveTypeId: ").append(toIndentedString(leaveTypeId)).append("\n");
    sb.append("    absenceReasonId: ").append(toIndentedString(absenceReasonId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    fromAt: ").append(toIndentedString(fromAt)).append("\n");
    sb.append("    toAt: ").append(toIndentedString(toAt)).append("\n");
    sb.append("    dayPart: ").append(toIndentedString(dayPart)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
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

