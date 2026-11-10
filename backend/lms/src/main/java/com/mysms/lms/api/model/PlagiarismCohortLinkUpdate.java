// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PlagiarismCohortLink that can be updated (partial update; see updateMask).
 */

public class PlagiarismCohortLinkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID plagiarismAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerClassRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerClassTitle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime providerClassEndAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerAssignmentRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerAssignmentTitle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime providerAssignmentStartAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime providerAssignmentDueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerAssignmentInstructions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean studentReportAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherFirstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherLastName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean classCreated;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean assignmentCreated;

  public PlagiarismCohortLinkUpdate plagiarismAccountId(@Nullable UUID plagiarismAccountId) {
    this.plagiarismAccountId = plagiarismAccountId;
    return this;
  }

  /**
   * Account used.
   * @return plagiarismAccountId
   */
  @Valid 
  @JsonProperty("plagiarismAccountId")
  public @Nullable UUID getPlagiarismAccountId() {
    return plagiarismAccountId;
  }

  @JsonProperty("plagiarismAccountId")
  public void setPlagiarismAccountId(@Nullable UUID plagiarismAccountId) {
    this.plagiarismAccountId = plagiarismAccountId;
  }

  public PlagiarismCohortLinkUpdate accountRef(@Nullable String accountRef) {
    this.accountRef = accountRef;
    return this;
  }

  /**
   * Provider account id at creation.
   * @return accountRef
   */
  @Size(max = 100) 
  @JsonProperty("accountRef")
  public @Nullable String getAccountRef() {
    return accountRef;
  }

  @JsonProperty("accountRef")
  public void setAccountRef(@Nullable String accountRef) {
    this.accountRef = accountRef;
  }

  public PlagiarismCohortLinkUpdate providerClassRef(@Nullable String providerClassRef) {
    this.providerClassRef = providerClassRef;
    return this;
  }

  /**
   * Provider class id.
   * @return providerClassRef
   */
  @Size(max = 100) 
  @JsonProperty("providerClassRef")
  public @Nullable String getProviderClassRef() {
    return providerClassRef;
  }

  @JsonProperty("providerClassRef")
  public void setProviderClassRef(@Nullable String providerClassRef) {
    this.providerClassRef = providerClassRef;
  }

  public PlagiarismCohortLinkUpdate providerClassTitle(@Nullable String providerClassTitle) {
    this.providerClassTitle = providerClassTitle;
    return this;
  }

  /**
   * Provider class title.
   * @return providerClassTitle
   */
  @Size(max = 255) 
  @JsonProperty("providerClassTitle")
  public @Nullable String getProviderClassTitle() {
    return providerClassTitle;
  }

  @JsonProperty("providerClassTitle")
  public void setProviderClassTitle(@Nullable String providerClassTitle) {
    this.providerClassTitle = providerClassTitle;
  }

  public PlagiarismCohortLinkUpdate providerClassEndAt(@Nullable OffsetDateTime providerClassEndAt) {
    this.providerClassEndAt = providerClassEndAt;
    return this;
  }

  /**
   * Provider class end.
   * @return providerClassEndAt
   */
  @Valid 
  @JsonProperty("providerClassEndAt")
  public @Nullable OffsetDateTime getProviderClassEndAt() {
    return providerClassEndAt;
  }

  @JsonProperty("providerClassEndAt")
  public void setProviderClassEndAt(@Nullable OffsetDateTime providerClassEndAt) {
    this.providerClassEndAt = providerClassEndAt;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentRef(@Nullable String providerAssignmentRef) {
    this.providerAssignmentRef = providerAssignmentRef;
    return this;
  }

  /**
   * Provider assignment id.
   * @return providerAssignmentRef
   */
  @Size(max = 100) 
  @JsonProperty("providerAssignmentRef")
  public @Nullable String getProviderAssignmentRef() {
    return providerAssignmentRef;
  }

  @JsonProperty("providerAssignmentRef")
  public void setProviderAssignmentRef(@Nullable String providerAssignmentRef) {
    this.providerAssignmentRef = providerAssignmentRef;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentTitle(@Nullable String providerAssignmentTitle) {
    this.providerAssignmentTitle = providerAssignmentTitle;
    return this;
  }

  /**
   * Provider assignment title.
   * @return providerAssignmentTitle
   */
  @Size(max = 255) 
  @JsonProperty("providerAssignmentTitle")
  public @Nullable String getProviderAssignmentTitle() {
    return providerAssignmentTitle;
  }

  @JsonProperty("providerAssignmentTitle")
  public void setProviderAssignmentTitle(@Nullable String providerAssignmentTitle) {
    this.providerAssignmentTitle = providerAssignmentTitle;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentStartAt(@Nullable OffsetDateTime providerAssignmentStartAt) {
    this.providerAssignmentStartAt = providerAssignmentStartAt;
    return this;
  }

  /**
   * Provider assignment start.
   * @return providerAssignmentStartAt
   */
  @Valid 
  @JsonProperty("providerAssignmentStartAt")
  public @Nullable OffsetDateTime getProviderAssignmentStartAt() {
    return providerAssignmentStartAt;
  }

  @JsonProperty("providerAssignmentStartAt")
  public void setProviderAssignmentStartAt(@Nullable OffsetDateTime providerAssignmentStartAt) {
    this.providerAssignmentStartAt = providerAssignmentStartAt;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentDueAt(@Nullable OffsetDateTime providerAssignmentDueAt) {
    this.providerAssignmentDueAt = providerAssignmentDueAt;
    return this;
  }

  /**
   * Provider assignment due.
   * @return providerAssignmentDueAt
   */
  @Valid 
  @JsonProperty("providerAssignmentDueAt")
  public @Nullable OffsetDateTime getProviderAssignmentDueAt() {
    return providerAssignmentDueAt;
  }

  @JsonProperty("providerAssignmentDueAt")
  public void setProviderAssignmentDueAt(@Nullable OffsetDateTime providerAssignmentDueAt) {
    this.providerAssignmentDueAt = providerAssignmentDueAt;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentInstructions(@Nullable String providerAssignmentInstructions) {
    this.providerAssignmentInstructions = providerAssignmentInstructions;
    return this;
  }

  /**
   * Instructions sent to the provider.
   * @return providerAssignmentInstructions
   */
  
  @JsonProperty("providerAssignmentInstructions")
  public @Nullable String getProviderAssignmentInstructions() {
    return providerAssignmentInstructions;
  }

  @JsonProperty("providerAssignmentInstructions")
  public void setProviderAssignmentInstructions(@Nullable String providerAssignmentInstructions) {
    this.providerAssignmentInstructions = providerAssignmentInstructions;
  }

  public PlagiarismCohortLinkUpdate studentReportAccess(@Nullable Boolean studentReportAccess) {
    this.studentReportAccess = studentReportAccess;
    return this;
  }

  /**
   * Students can see similarity reports.
   * @return studentReportAccess
   */
  
  @JsonProperty("studentReportAccess")
  public @Nullable Boolean getStudentReportAccess() {
    return studentReportAccess;
  }

  @JsonProperty("studentReportAccess")
  public void setStudentReportAccess(@Nullable Boolean studentReportAccess) {
    this.studentReportAccess = studentReportAccess;
  }

  public PlagiarismCohortLinkUpdate teacherEmail(@Nullable String teacherEmail) {
    this.teacherEmail = teacherEmail;
    return this;
  }

  /**
   * Teacher e-mail registered with the provider.
   * @return teacherEmail
   */
  @Size(max = 255) 
  @JsonProperty("teacherEmail")
  public @Nullable String getTeacherEmail() {
    return teacherEmail;
  }

  @JsonProperty("teacherEmail")
  public void setTeacherEmail(@Nullable String teacherEmail) {
    this.teacherEmail = teacherEmail;
  }

  public PlagiarismCohortLinkUpdate teacherFirstName(@Nullable String teacherFirstName) {
    this.teacherFirstName = teacherFirstName;
    return this;
  }

  /**
   * Teacher first name.
   * @return teacherFirstName
   */
  @Size(max = 100) 
  @JsonProperty("teacherFirstName")
  public @Nullable String getTeacherFirstName() {
    return teacherFirstName;
  }

  @JsonProperty("teacherFirstName")
  public void setTeacherFirstName(@Nullable String teacherFirstName) {
    this.teacherFirstName = teacherFirstName;
  }

  public PlagiarismCohortLinkUpdate teacherLastName(@Nullable String teacherLastName) {
    this.teacherLastName = teacherLastName;
    return this;
  }

  /**
   * Teacher last name.
   * @return teacherLastName
   */
  @Size(max = 100) 
  @JsonProperty("teacherLastName")
  public @Nullable String getTeacherLastName() {
    return teacherLastName;
  }

  @JsonProperty("teacherLastName")
  public void setTeacherLastName(@Nullable String teacherLastName) {
    this.teacherLastName = teacherLastName;
  }

  public PlagiarismCohortLinkUpdate classCreated(@Nullable Boolean classCreated) {
    this.classCreated = classCreated;
    return this;
  }

  /**
   * Provider class created.
   * @return classCreated
   */
  
  @JsonProperty("classCreated")
  public @Nullable Boolean getClassCreated() {
    return classCreated;
  }

  @JsonProperty("classCreated")
  public void setClassCreated(@Nullable Boolean classCreated) {
    this.classCreated = classCreated;
  }

  public PlagiarismCohortLinkUpdate assignmentCreated(@Nullable Boolean assignmentCreated) {
    this.assignmentCreated = assignmentCreated;
    return this;
  }

  /**
   * Provider assignment created.
   * @return assignmentCreated
   */
  
  @JsonProperty("assignmentCreated")
  public @Nullable Boolean getAssignmentCreated() {
    return assignmentCreated;
  }

  @JsonProperty("assignmentCreated")
  public void setAssignmentCreated(@Nullable Boolean assignmentCreated) {
    this.assignmentCreated = assignmentCreated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlagiarismCohortLinkUpdate plagiarismCohortLinkUpdate = (PlagiarismCohortLinkUpdate) o;
    return Objects.equals(this.plagiarismAccountId, plagiarismCohortLinkUpdate.plagiarismAccountId) &&
        Objects.equals(this.accountRef, plagiarismCohortLinkUpdate.accountRef) &&
        Objects.equals(this.providerClassRef, plagiarismCohortLinkUpdate.providerClassRef) &&
        Objects.equals(this.providerClassTitle, plagiarismCohortLinkUpdate.providerClassTitle) &&
        Objects.equals(this.providerClassEndAt, plagiarismCohortLinkUpdate.providerClassEndAt) &&
        Objects.equals(this.providerAssignmentRef, plagiarismCohortLinkUpdate.providerAssignmentRef) &&
        Objects.equals(this.providerAssignmentTitle, plagiarismCohortLinkUpdate.providerAssignmentTitle) &&
        Objects.equals(this.providerAssignmentStartAt, plagiarismCohortLinkUpdate.providerAssignmentStartAt) &&
        Objects.equals(this.providerAssignmentDueAt, plagiarismCohortLinkUpdate.providerAssignmentDueAt) &&
        Objects.equals(this.providerAssignmentInstructions, plagiarismCohortLinkUpdate.providerAssignmentInstructions) &&
        Objects.equals(this.studentReportAccess, plagiarismCohortLinkUpdate.studentReportAccess) &&
        Objects.equals(this.teacherEmail, plagiarismCohortLinkUpdate.teacherEmail) &&
        Objects.equals(this.teacherFirstName, plagiarismCohortLinkUpdate.teacherFirstName) &&
        Objects.equals(this.teacherLastName, plagiarismCohortLinkUpdate.teacherLastName) &&
        Objects.equals(this.classCreated, plagiarismCohortLinkUpdate.classCreated) &&
        Objects.equals(this.assignmentCreated, plagiarismCohortLinkUpdate.assignmentCreated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(plagiarismAccountId, accountRef, providerClassRef, providerClassTitle, providerClassEndAt, providerAssignmentRef, providerAssignmentTitle, providerAssignmentStartAt, providerAssignmentDueAt, providerAssignmentInstructions, studentReportAccess, teacherEmail, teacherFirstName, teacherLastName, classCreated, assignmentCreated);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PlagiarismCohortLinkUpdate {\n");
    sb.append("    plagiarismAccountId: ").append(toIndentedString(plagiarismAccountId)).append("\n");
    sb.append("    accountRef: ").append(toIndentedString(accountRef)).append("\n");
    sb.append("    providerClassRef: ").append(toIndentedString(providerClassRef)).append("\n");
    sb.append("    providerClassTitle: ").append(toIndentedString(providerClassTitle)).append("\n");
    sb.append("    providerClassEndAt: ").append(toIndentedString(providerClassEndAt)).append("\n");
    sb.append("    providerAssignmentRef: ").append(toIndentedString(providerAssignmentRef)).append("\n");
    sb.append("    providerAssignmentTitle: ").append(toIndentedString(providerAssignmentTitle)).append("\n");
    sb.append("    providerAssignmentStartAt: ").append(toIndentedString(providerAssignmentStartAt)).append("\n");
    sb.append("    providerAssignmentDueAt: ").append(toIndentedString(providerAssignmentDueAt)).append("\n");
    sb.append("    providerAssignmentInstructions: ").append(toIndentedString(providerAssignmentInstructions)).append("\n");
    sb.append("    studentReportAccess: ").append(toIndentedString(studentReportAccess)).append("\n");
    sb.append("    teacherEmail: ").append(toIndentedString(teacherEmail)).append("\n");
    sb.append("    teacherFirstName: ").append(toIndentedString(teacherFirstName)).append("\n");
    sb.append("    teacherLastName: ").append(toIndentedString(teacherLastName)).append("\n");
    sb.append("    classCreated: ").append(toIndentedString(classCreated)).append("\n");
    sb.append("    assignmentCreated: ").append(toIndentedString(assignmentCreated)).append("\n");
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

