// Owned API contract of hr.
package com.mysms.hrms.hr.api.model;

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
 * Fields of LeaveApplication that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LeaveApplicationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> leaveTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> absenceReasonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> fromAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> toAt;

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
  private @Nullable Optional<@NotNull DayPartEnum> dayPart;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> remarks;

  public LeaveApplicationUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public LeaveApplicationUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
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

  public LeaveApplicationUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Applicant.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public LeaveApplicationUpdate leaveTypeId(@Nullable Optional<UUID> leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
    return this;
  }

  /**
   * Leave type.
   * @return leaveTypeId
   */
  
  @JsonProperty("leaveTypeId")
  public @Nullable Optional<UUID> getLeaveTypeId() {
    return leaveTypeId;
  }

  @JsonProperty("leaveTypeId")
  public void setLeaveTypeId(@Nullable Optional<UUID> leaveTypeId) {
    this.leaveTypeId = leaveTypeId;
  }

  public LeaveApplicationUpdate absenceReasonId(@Nullable Optional<UUID> absenceReasonId) {
    this.absenceReasonId = absenceReasonId;
    return this;
  }

  /**
   * Reason category.
   * @return absenceReasonId
   */
  
  @JsonProperty("absenceReasonId")
  public @Nullable Optional<UUID> getAbsenceReasonId() {
    return absenceReasonId;
  }

  @JsonProperty("absenceReasonId")
  public void setAbsenceReasonId(@Nullable Optional<UUID> absenceReasonId) {
    this.absenceReasonId = absenceReasonId;
  }

  public LeaveApplicationUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class whose cover is affected.
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public LeaveApplicationUpdate fromAt(@Nullable Optional<OffsetDateTime> fromAt) {
    this.fromAt = fromAt;
    return this;
  }

  /**
   * Leave start.
   * @return fromAt
   */
  
  @JsonProperty("fromAt")
  public @Nullable Optional<OffsetDateTime> getFromAt() {
    return fromAt;
  }

  @JsonProperty("fromAt")
  public void setFromAt(@Nullable Optional<OffsetDateTime> fromAt) {
    this.fromAt = fromAt;
  }

  public LeaveApplicationUpdate toAt(@Nullable Optional<OffsetDateTime> toAt) {
    this.toAt = toAt;
    return this;
  }

  /**
   * Leave end.
   * @return toAt
   */
  
  @JsonProperty("toAt")
  public @Nullable Optional<OffsetDateTime> getToAt() {
    return toAt;
  }

  @JsonProperty("toAt")
  public void setToAt(@Nullable Optional<OffsetDateTime> toAt) {
    this.toAt = toAt;
  }

  public LeaveApplicationUpdate dayPart(@Nullable Optional<DayPartEnum> dayPart) {
    this.dayPart = dayPart;
    return this;
  }

  /**
   * Part of day for single-day leave.
   * @return dayPart
   */
  
  @JsonProperty("dayPart")
  public @Nullable Optional<DayPartEnum> getDayPart() {
    return dayPart;
  }

  @JsonProperty("dayPart")
  public void setDayPart(@Nullable Optional<DayPartEnum> dayPart) {
    this.dayPart = dayPart;
  }

  public LeaveApplicationUpdate remarks(@Nullable Optional<String> remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Applicant remarks.
   * @return remarks
   */
  
  @JsonProperty("remarks")
  public @Nullable Optional<String> getRemarks() {
    return remarks;
  }

  @JsonProperty("remarks")
  public void setRemarks(@Nullable Optional<String> remarks) {
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

