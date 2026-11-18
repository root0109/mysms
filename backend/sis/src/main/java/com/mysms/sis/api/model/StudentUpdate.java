// Owned API contract of sis.
package com.mysms.sis.api.model;

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
 * Fields of Student that can be updated (partial update; see updateMask).
 */

public class StudentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String admissionNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate admissionDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate joiningDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String governmentStudentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bonafideCertificateNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate exitDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String exitReason;

  public StudentUpdate personId(@Nullable UUID personId) {
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

  public StudentUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student login account.
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

  public StudentUpdate institutionId(@Nullable UUID institutionId) {
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

  public StudentUpdate admissionNumber(@Nullable String admissionNumber) {
    this.admissionNumber = admissionNumber;
    return this;
  }

  /**
   * Admission / registration number.
   * @return admissionNumber
   */
  @Size(max = 30) 
  @JsonProperty("admissionNumber")
  public @Nullable String getAdmissionNumber() {
    return admissionNumber;
  }

  @JsonProperty("admissionNumber")
  public void setAdmissionNumber(@Nullable String admissionNumber) {
    this.admissionNumber = admissionNumber;
  }

  public StudentUpdate admissionDate(@Nullable LocalDate admissionDate) {
    this.admissionDate = admissionDate;
    return this;
  }

  /**
   * Date of admission.
   * @return admissionDate
   */
  @Valid 
  @JsonProperty("admissionDate")
  public @Nullable LocalDate getAdmissionDate() {
    return admissionDate;
  }

  @JsonProperty("admissionDate")
  public void setAdmissionDate(@Nullable LocalDate admissionDate) {
    this.admissionDate = admissionDate;
  }

  public StudentUpdate joiningDate(@Nullable LocalDate joiningDate) {
    this.joiningDate = joiningDate;
    return this;
  }

  /**
   * First attendance date.
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

  public StudentUpdate studentCategoryId(@Nullable UUID studentCategoryId) {
    this.studentCategoryId = studentCategoryId;
    return this;
  }

  /**
   * Category.
   * @return studentCategoryId
   */
  @Valid 
  @JsonProperty("studentCategoryId")
  public @Nullable UUID getStudentCategoryId() {
    return studentCategoryId;
  }

  @JsonProperty("studentCategoryId")
  public void setStudentCategoryId(@Nullable UUID studentCategoryId) {
    this.studentCategoryId = studentCategoryId;
  }

  public StudentUpdate governmentStudentId(@Nullable String governmentStudentId) {
    this.governmentStudentId = governmentStudentId;
    return this;
  }

  /**
   * External government education-registry id (e.g. EMIS/UDISE+ PEN).
   * @return governmentStudentId
   */
  @Size(max = 40) 
  @JsonProperty("governmentStudentId")
  public @Nullable String getGovernmentStudentId() {
    return governmentStudentId;
  }

  @JsonProperty("governmentStudentId")
  public void setGovernmentStudentId(@Nullable String governmentStudentId) {
    this.governmentStudentId = governmentStudentId;
  }

  public StudentUpdate bonafideCertificateNumber(@Nullable String bonafideCertificateNumber) {
    this.bonafideCertificateNumber = bonafideCertificateNumber;
    return this;
  }

  /**
   * Number of the last bonafide certificate issued.
   * @return bonafideCertificateNumber
   */
  @Size(max = 40) 
  @JsonProperty("bonafideCertificateNumber")
  public @Nullable String getBonafideCertificateNumber() {
    return bonafideCertificateNumber;
  }

  @JsonProperty("bonafideCertificateNumber")
  public void setBonafideCertificateNumber(@Nullable String bonafideCertificateNumber) {
    this.bonafideCertificateNumber = bonafideCertificateNumber;
  }

  public StudentUpdate exitDate(@Nullable LocalDate exitDate) {
    this.exitDate = exitDate;
    return this;
  }

  /**
   * Date the student left.
   * @return exitDate
   */
  @Valid 
  @JsonProperty("exitDate")
  public @Nullable LocalDate getExitDate() {
    return exitDate;
  }

  @JsonProperty("exitDate")
  public void setExitDate(@Nullable LocalDate exitDate) {
    this.exitDate = exitDate;
  }

  public StudentUpdate exitReason(@Nullable String exitReason) {
    this.exitReason = exitReason;
    return this;
  }

  /**
   * Reason for leaving.
   * @return exitReason
   */
  
  @JsonProperty("exitReason")
  public @Nullable String getExitReason() {
    return exitReason;
  }

  @JsonProperty("exitReason")
  public void setExitReason(@Nullable String exitReason) {
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

