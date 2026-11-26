// Owned API contract of frontoffice.
package com.mysms.campus.frontoffice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of VisitorLog that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class VisitorLogUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> visitorName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> visitorType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 320) String> email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> phone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> companyName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> address;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> relationToStudent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> relatedPersonName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> visitorCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> purpose;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> hostStaffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> visitDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> entryTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> exitTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> remarks;

  public VisitorLogUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public VisitorLogUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
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

  public VisitorLogUpdate visitorName(@Nullable Optional<String> visitorName) {
    this.visitorName = visitorName;
    return this;
  }

  /**
   * Visitor.
   * @return visitorName
   */
  
  @JsonProperty("visitorName")
  public @Nullable Optional<String> getVisitorName() {
    return visitorName;
  }

  @JsonProperty("visitorName")
  public void setVisitorName(@Nullable Optional<String> visitorName) {
    this.visitorName = visitorName;
  }

  public VisitorLogUpdate visitorType(@Nullable Optional<String> visitorType) {
    this.visitorType = visitorType;
    return this;
  }

  /**
   * Visitor's relation to the school (parent, vendor, official...).
   * @return visitorType
   */
  
  @JsonProperty("visitorType")
  public @Nullable Optional<String> getVisitorType() {
    return visitorType;
  }

  @JsonProperty("visitorType")
  public void setVisitorType(@Nullable Optional<String> visitorType) {
    this.visitorType = visitorType;
  }

  public VisitorLogUpdate email(@Nullable Optional<String> email) {
    this.email = email;
    return this;
  }

  /**
   * E-mail.
   * @return email
   */
  
  @JsonProperty("email")
  public @Nullable Optional<String> getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable Optional<String> email) {
    this.email = email;
  }

  public VisitorLogUpdate phone(@Nullable Optional<String> phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Phone.
   * @return phone
   */
  
  @JsonProperty("phone")
  public @Nullable Optional<String> getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable Optional<String> phone) {
    this.phone = phone;
  }

  public VisitorLogUpdate companyName(@Nullable Optional<String> companyName) {
    this.companyName = companyName;
    return this;
  }

  /**
   * Company.
   * @return companyName
   */
  
  @JsonProperty("companyName")
  public @Nullable Optional<String> getCompanyName() {
    return companyName;
  }

  @JsonProperty("companyName")
  public void setCompanyName(@Nullable Optional<String> companyName) {
    this.companyName = companyName;
  }

  public VisitorLogUpdate address(@Nullable Optional<String> address) {
    this.address = address;
    return this;
  }

  /**
   * Address.
   * @return address
   */
  
  @JsonProperty("address")
  public @Nullable Optional<String> getAddress() {
    return address;
  }

  @JsonProperty("address")
  public void setAddress(@Nullable Optional<String> address) {
    this.address = address;
  }

  public VisitorLogUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student visited.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public VisitorLogUpdate relationToStudent(@Nullable Optional<String> relationToStudent) {
    this.relationToStudent = relationToStudent;
    return this;
  }

  /**
   * Relation to that student.
   * @return relationToStudent
   */
  
  @JsonProperty("relationToStudent")
  public @Nullable Optional<String> getRelationToStudent() {
    return relationToStudent;
  }

  @JsonProperty("relationToStudent")
  public void setRelationToStudent(@Nullable Optional<String> relationToStudent) {
    this.relationToStudent = relationToStudent;
  }

  public VisitorLogUpdate relatedPersonName(@Nullable Optional<String> relatedPersonName) {
    this.relatedPersonName = relatedPersonName;
    return this;
  }

  /**
   * Name of the related person.
   * @return relatedPersonName
   */
  
  @JsonProperty("relatedPersonName")
  public @Nullable Optional<String> getRelatedPersonName() {
    return relatedPersonName;
  }

  @JsonProperty("relatedPersonName")
  public void setRelatedPersonName(@Nullable Optional<String> relatedPersonName) {
    this.relatedPersonName = relatedPersonName;
  }

  public VisitorLogUpdate visitorCount(@Nullable Optional<Integer> visitorCount) {
    this.visitorCount = visitorCount;
    return this;
  }

  /**
   * Number of visitors.
   * @return visitorCount
   */
  
  @JsonProperty("visitorCount")
  public @Nullable Optional<Integer> getVisitorCount() {
    return visitorCount;
  }

  @JsonProperty("visitorCount")
  public void setVisitorCount(@Nullable Optional<Integer> visitorCount) {
    this.visitorCount = visitorCount;
  }

  public VisitorLogUpdate purpose(@Nullable Optional<String> purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * Purpose.
   * @return purpose
   */
  
  @JsonProperty("purpose")
  public @Nullable Optional<String> getPurpose() {
    return purpose;
  }

  @JsonProperty("purpose")
  public void setPurpose(@Nullable Optional<String> purpose) {
    this.purpose = purpose;
  }

  public VisitorLogUpdate hostStaffId(@Nullable Optional<UUID> hostStaffId) {
    this.hostStaffId = hostStaffId;
    return this;
  }

  /**
   * Staff member met.
   * @return hostStaffId
   */
  
  @JsonProperty("hostStaffId")
  public @Nullable Optional<UUID> getHostStaffId() {
    return hostStaffId;
  }

  @JsonProperty("hostStaffId")
  public void setHostStaffId(@Nullable Optional<UUID> hostStaffId) {
    this.hostStaffId = hostStaffId;
  }

  public VisitorLogUpdate visitDate(@Nullable Optional<LocalDate> visitDate) {
    this.visitDate = visitDate;
    return this;
  }

  /**
   * Date.
   * @return visitDate
   */
  
  @JsonProperty("visitDate")
  public @Nullable Optional<LocalDate> getVisitDate() {
    return visitDate;
  }

  @JsonProperty("visitDate")
  public void setVisitDate(@Nullable Optional<LocalDate> visitDate) {
    this.visitDate = visitDate;
  }

  public VisitorLogUpdate entryTime(@Nullable Optional<String> entryTime) {
    this.entryTime = entryTime;
    return this;
  }

  /**
   * Entry time.
   * @return entryTime
   */
  
  @JsonProperty("entryTime")
  public @Nullable Optional<String> getEntryTime() {
    return entryTime;
  }

  @JsonProperty("entryTime")
  public void setEntryTime(@Nullable Optional<String> entryTime) {
    this.entryTime = entryTime;
  }

  public VisitorLogUpdate exitTime(@Nullable Optional<String> exitTime) {
    this.exitTime = exitTime;
    return this;
  }

  /**
   * Exit time.
   * @return exitTime
   */
  
  @JsonProperty("exitTime")
  public @Nullable Optional<String> getExitTime() {
    return exitTime;
  }

  @JsonProperty("exitTime")
  public void setExitTime(@Nullable Optional<String> exitTime) {
    this.exitTime = exitTime;
  }

  public VisitorLogUpdate remarks(@Nullable Optional<String> remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Remarks.
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
    VisitorLogUpdate visitorLogUpdate = (VisitorLogUpdate) o;
    return Objects.equals(this.institutionId, visitorLogUpdate.institutionId) &&
        Objects.equals(this.academicYearId, visitorLogUpdate.academicYearId) &&
        Objects.equals(this.visitorName, visitorLogUpdate.visitorName) &&
        Objects.equals(this.visitorType, visitorLogUpdate.visitorType) &&
        Objects.equals(this.email, visitorLogUpdate.email) &&
        Objects.equals(this.phone, visitorLogUpdate.phone) &&
        Objects.equals(this.companyName, visitorLogUpdate.companyName) &&
        Objects.equals(this.address, visitorLogUpdate.address) &&
        Objects.equals(this.studentId, visitorLogUpdate.studentId) &&
        Objects.equals(this.relationToStudent, visitorLogUpdate.relationToStudent) &&
        Objects.equals(this.relatedPersonName, visitorLogUpdate.relatedPersonName) &&
        Objects.equals(this.visitorCount, visitorLogUpdate.visitorCount) &&
        Objects.equals(this.purpose, visitorLogUpdate.purpose) &&
        Objects.equals(this.hostStaffId, visitorLogUpdate.hostStaffId) &&
        Objects.equals(this.visitDate, visitorLogUpdate.visitDate) &&
        Objects.equals(this.entryTime, visitorLogUpdate.entryTime) &&
        Objects.equals(this.exitTime, visitorLogUpdate.exitTime) &&
        Objects.equals(this.remarks, visitorLogUpdate.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, visitorName, visitorType, email, phone, companyName, address, studentId, relationToStudent, relatedPersonName, visitorCount, purpose, hostStaffId, visitDate, entryTime, exitTime, remarks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VisitorLogUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    visitorName: ").append(toIndentedString(visitorName)).append("\n");
    sb.append("    visitorType: ").append(toIndentedString(visitorType)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    companyName: ").append(toIndentedString(companyName)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    relationToStudent: ").append(toIndentedString(relationToStudent)).append("\n");
    sb.append("    relatedPersonName: ").append(toIndentedString(relatedPersonName)).append("\n");
    sb.append("    visitorCount: ").append(toIndentedString(visitorCount)).append("\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
    sb.append("    hostStaffId: ").append(toIndentedString(hostStaffId)).append("\n");
    sb.append("    visitDate: ").append(toIndentedString(visitDate)).append("\n");
    sb.append("    entryTime: ").append(toIndentedString(entryTime)).append("\n");
    sb.append("    exitTime: ").append(toIndentedString(exitTime)).append("\n");
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

