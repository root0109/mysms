// Owned API contract of sis.
package com.mysms.students.sis.api.model;

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
 * Fields of Student that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StudentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> admissionNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> admissionDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> joiningDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> studentCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> governmentStudentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> bonafideCertificateNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> exitDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> exitReason;

  public StudentUpdate personId(@Nullable Optional<UUID> personId) {
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

  public StudentUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student login account.
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

  public StudentUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public StudentUpdate admissionNumber(@Nullable Optional<String> admissionNumber) {
    this.admissionNumber = admissionNumber;
    return this;
  }

  /**
   * Admission / registration number.
   * @return admissionNumber
   */
  
  @JsonProperty("admissionNumber")
  public @Nullable Optional<String> getAdmissionNumber() {
    return admissionNumber;
  }

  @JsonProperty("admissionNumber")
  public void setAdmissionNumber(@Nullable Optional<String> admissionNumber) {
    this.admissionNumber = admissionNumber;
  }

  public StudentUpdate admissionDate(@Nullable Optional<LocalDate> admissionDate) {
    this.admissionDate = admissionDate;
    return this;
  }

  /**
   * Date of admission.
   * @return admissionDate
   */
  
  @JsonProperty("admissionDate")
  public @Nullable Optional<LocalDate> getAdmissionDate() {
    return admissionDate;
  }

  @JsonProperty("admissionDate")
  public void setAdmissionDate(@Nullable Optional<LocalDate> admissionDate) {
    this.admissionDate = admissionDate;
  }

  public StudentUpdate joiningDate(@Nullable Optional<LocalDate> joiningDate) {
    this.joiningDate = joiningDate;
    return this;
  }

  /**
   * First attendance date.
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

  public StudentUpdate studentCategoryId(@Nullable Optional<UUID> studentCategoryId) {
    this.studentCategoryId = studentCategoryId;
    return this;
  }

  /**
   * Category.
   * @return studentCategoryId
   */
  
  @JsonProperty("studentCategoryId")
  public @Nullable Optional<UUID> getStudentCategoryId() {
    return studentCategoryId;
  }

  @JsonProperty("studentCategoryId")
  public void setStudentCategoryId(@Nullable Optional<UUID> studentCategoryId) {
    this.studentCategoryId = studentCategoryId;
  }

  public StudentUpdate governmentStudentId(@Nullable Optional<String> governmentStudentId) {
    this.governmentStudentId = governmentStudentId;
    return this;
  }

  /**
   * External government education-registry id (e.g. EMIS/UDISE+ PEN).
   * @return governmentStudentId
   */
  
  @JsonProperty("governmentStudentId")
  public @Nullable Optional<String> getGovernmentStudentId() {
    return governmentStudentId;
  }

  @JsonProperty("governmentStudentId")
  public void setGovernmentStudentId(@Nullable Optional<String> governmentStudentId) {
    this.governmentStudentId = governmentStudentId;
  }

  public StudentUpdate bonafideCertificateNumber(@Nullable Optional<String> bonafideCertificateNumber) {
    this.bonafideCertificateNumber = bonafideCertificateNumber;
    return this;
  }

  /**
   * Number of the last bonafide certificate issued.
   * @return bonafideCertificateNumber
   */
  
  @JsonProperty("bonafideCertificateNumber")
  public @Nullable Optional<String> getBonafideCertificateNumber() {
    return bonafideCertificateNumber;
  }

  @JsonProperty("bonafideCertificateNumber")
  public void setBonafideCertificateNumber(@Nullable Optional<String> bonafideCertificateNumber) {
    this.bonafideCertificateNumber = bonafideCertificateNumber;
  }

  public StudentUpdate exitDate(@Nullable Optional<LocalDate> exitDate) {
    this.exitDate = exitDate;
    return this;
  }

  /**
   * Date the student left.
   * @return exitDate
   */
  
  @JsonProperty("exitDate")
  public @Nullable Optional<LocalDate> getExitDate() {
    return exitDate;
  }

  @JsonProperty("exitDate")
  public void setExitDate(@Nullable Optional<LocalDate> exitDate) {
    this.exitDate = exitDate;
  }

  public StudentUpdate exitReason(@Nullable Optional<String> exitReason) {
    this.exitReason = exitReason;
    return this;
  }

  /**
   * Reason for leaving.
   * @return exitReason
   */
  
  @JsonProperty("exitReason")
  public @Nullable Optional<String> getExitReason() {
    return exitReason;
  }

  @JsonProperty("exitReason")
  public void setExitReason(@Nullable Optional<String> exitReason) {
    this.exitReason = exitReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentUpdate studentUpdate = (StudentUpdate) o;
    return Objects.equals(this.personId, studentUpdate.personId) &&
        Objects.equals(this.userId, studentUpdate.userId) &&
        Objects.equals(this.institutionId, studentUpdate.institutionId) &&
        Objects.equals(this.admissionNumber, studentUpdate.admissionNumber) &&
        Objects.equals(this.admissionDate, studentUpdate.admissionDate) &&
        Objects.equals(this.joiningDate, studentUpdate.joiningDate) &&
        Objects.equals(this.studentCategoryId, studentUpdate.studentCategoryId) &&
        Objects.equals(this.governmentStudentId, studentUpdate.governmentStudentId) &&
        Objects.equals(this.bonafideCertificateNumber, studentUpdate.bonafideCertificateNumber) &&
        Objects.equals(this.exitDate, studentUpdate.exitDate) &&
        Objects.equals(this.exitReason, studentUpdate.exitReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personId, userId, institutionId, admissionNumber, admissionDate, joiningDate, studentCategoryId, governmentStudentId, bonafideCertificateNumber, exitDate, exitReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentUpdate {\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    admissionNumber: ").append(toIndentedString(admissionNumber)).append("\n");
    sb.append("    admissionDate: ").append(toIndentedString(admissionDate)).append("\n");
    sb.append("    joiningDate: ").append(toIndentedString(joiningDate)).append("\n");
    sb.append("    studentCategoryId: ").append(toIndentedString(studentCategoryId)).append("\n");
    sb.append("    governmentStudentId: ").append(toIndentedString(governmentStudentId)).append("\n");
    sb.append("    bonafideCertificateNumber: ").append(toIndentedString(bonafideCertificateNumber)).append("\n");
    sb.append("    exitDate: ").append(toIndentedString(exitDate)).append("\n");
    sb.append("    exitReason: ").append(toIndentedString(exitReason)).append("\n");
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

