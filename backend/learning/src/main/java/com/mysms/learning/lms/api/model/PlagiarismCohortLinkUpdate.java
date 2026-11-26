// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of PlagiarismCohortLink that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PlagiarismCohortLinkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> plagiarismAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> accountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> providerClassRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> providerClassTitle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> providerClassEndAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> providerAssignmentRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> providerAssignmentTitle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> providerAssignmentStartAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> providerAssignmentDueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> providerAssignmentInstructions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> studentReportAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> teacherEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> teacherFirstName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> teacherLastName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> classCreated;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> assignmentCreated;

  public PlagiarismCohortLinkUpdate plagiarismAccountId(@Nullable Optional<UUID> plagiarismAccountId) {
    this.plagiarismAccountId = plagiarismAccountId;
    return this;
  }

  /**
   * Account used.
   * @return plagiarismAccountId
   */
  
  @JsonProperty("plagiarismAccountId")
  public @Nullable Optional<UUID> getPlagiarismAccountId() {
    return plagiarismAccountId;
  }

  @JsonProperty("plagiarismAccountId")
  public void setPlagiarismAccountId(@Nullable Optional<UUID> plagiarismAccountId) {
    this.plagiarismAccountId = plagiarismAccountId;
  }

  public PlagiarismCohortLinkUpdate accountRef(@Nullable Optional<String> accountRef) {
    this.accountRef = accountRef;
    return this;
  }

  /**
   * Provider account id at creation.
   * @return accountRef
   */
  
  @JsonProperty("accountRef")
  public @Nullable Optional<String> getAccountRef() {
    return accountRef;
  }

  @JsonProperty("accountRef")
  public void setAccountRef(@Nullable Optional<String> accountRef) {
    this.accountRef = accountRef;
  }

  public PlagiarismCohortLinkUpdate providerClassRef(@Nullable Optional<String> providerClassRef) {
    this.providerClassRef = providerClassRef;
    return this;
  }

  /**
   * Provider class id.
   * @return providerClassRef
   */
  
  @JsonProperty("providerClassRef")
  public @Nullable Optional<String> getProviderClassRef() {
    return providerClassRef;
  }

  @JsonProperty("providerClassRef")
  public void setProviderClassRef(@Nullable Optional<String> providerClassRef) {
    this.providerClassRef = providerClassRef;
  }

  public PlagiarismCohortLinkUpdate providerClassTitle(@Nullable Optional<String> providerClassTitle) {
    this.providerClassTitle = providerClassTitle;
    return this;
  }

  /**
   * Provider class title.
   * @return providerClassTitle
   */
  
  @JsonProperty("providerClassTitle")
  public @Nullable Optional<String> getProviderClassTitle() {
    return providerClassTitle;
  }

  @JsonProperty("providerClassTitle")
  public void setProviderClassTitle(@Nullable Optional<String> providerClassTitle) {
    this.providerClassTitle = providerClassTitle;
  }

  public PlagiarismCohortLinkUpdate providerClassEndAt(@Nullable Optional<OffsetDateTime> providerClassEndAt) {
    this.providerClassEndAt = providerClassEndAt;
    return this;
  }

  /**
   * Provider class end.
   * @return providerClassEndAt
   */
  
  @JsonProperty("providerClassEndAt")
  public @Nullable Optional<OffsetDateTime> getProviderClassEndAt() {
    return providerClassEndAt;
  }

  @JsonProperty("providerClassEndAt")
  public void setProviderClassEndAt(@Nullable Optional<OffsetDateTime> providerClassEndAt) {
    this.providerClassEndAt = providerClassEndAt;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentRef(@Nullable Optional<String> providerAssignmentRef) {
    this.providerAssignmentRef = providerAssignmentRef;
    return this;
  }

  /**
   * Provider assignment id.
   * @return providerAssignmentRef
   */
  
  @JsonProperty("providerAssignmentRef")
  public @Nullable Optional<String> getProviderAssignmentRef() {
    return providerAssignmentRef;
  }

  @JsonProperty("providerAssignmentRef")
  public void setProviderAssignmentRef(@Nullable Optional<String> providerAssignmentRef) {
    this.providerAssignmentRef = providerAssignmentRef;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentTitle(@Nullable Optional<String> providerAssignmentTitle) {
    this.providerAssignmentTitle = providerAssignmentTitle;
    return this;
  }

  /**
   * Provider assignment title.
   * @return providerAssignmentTitle
   */
  
  @JsonProperty("providerAssignmentTitle")
  public @Nullable Optional<String> getProviderAssignmentTitle() {
    return providerAssignmentTitle;
  }

  @JsonProperty("providerAssignmentTitle")
  public void setProviderAssignmentTitle(@Nullable Optional<String> providerAssignmentTitle) {
    this.providerAssignmentTitle = providerAssignmentTitle;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentStartAt(@Nullable Optional<OffsetDateTime> providerAssignmentStartAt) {
    this.providerAssignmentStartAt = providerAssignmentStartAt;
    return this;
  }

  /**
   * Provider assignment start.
   * @return providerAssignmentStartAt
   */
  
  @JsonProperty("providerAssignmentStartAt")
  public @Nullable Optional<OffsetDateTime> getProviderAssignmentStartAt() {
    return providerAssignmentStartAt;
  }

  @JsonProperty("providerAssignmentStartAt")
  public void setProviderAssignmentStartAt(@Nullable Optional<OffsetDateTime> providerAssignmentStartAt) {
    this.providerAssignmentStartAt = providerAssignmentStartAt;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentDueAt(@Nullable Optional<OffsetDateTime> providerAssignmentDueAt) {
    this.providerAssignmentDueAt = providerAssignmentDueAt;
    return this;
  }

  /**
   * Provider assignment due.
   * @return providerAssignmentDueAt
   */
  
  @JsonProperty("providerAssignmentDueAt")
  public @Nullable Optional<OffsetDateTime> getProviderAssignmentDueAt() {
    return providerAssignmentDueAt;
  }

  @JsonProperty("providerAssignmentDueAt")
  public void setProviderAssignmentDueAt(@Nullable Optional<OffsetDateTime> providerAssignmentDueAt) {
    this.providerAssignmentDueAt = providerAssignmentDueAt;
  }

  public PlagiarismCohortLinkUpdate providerAssignmentInstructions(@Nullable Optional<String> providerAssignmentInstructions) {
    this.providerAssignmentInstructions = providerAssignmentInstructions;
    return this;
  }

  /**
   * Instructions sent to the provider.
   * @return providerAssignmentInstructions
   */
  
  @JsonProperty("providerAssignmentInstructions")
  public @Nullable Optional<String> getProviderAssignmentInstructions() {
    return providerAssignmentInstructions;
  }

  @JsonProperty("providerAssignmentInstructions")
  public void setProviderAssignmentInstructions(@Nullable Optional<String> providerAssignmentInstructions) {
    this.providerAssignmentInstructions = providerAssignmentInstructions;
  }

  public PlagiarismCohortLinkUpdate studentReportAccess(@Nullable Optional<Boolean> studentReportAccess) {
    this.studentReportAccess = studentReportAccess;
    return this;
  }

  /**
   * Students can see similarity reports.
   * @return studentReportAccess
   */
  
  @JsonProperty("studentReportAccess")
  public @Nullable Optional<Boolean> getStudentReportAccess() {
    return studentReportAccess;
  }

  @JsonProperty("studentReportAccess")
  public void setStudentReportAccess(@Nullable Optional<Boolean> studentReportAccess) {
    this.studentReportAccess = studentReportAccess;
  }

  public PlagiarismCohortLinkUpdate teacherEmail(@Nullable Optional<String> teacherEmail) {
    this.teacherEmail = teacherEmail;
    return this;
  }

  /**
   * Teacher e-mail registered with the provider.
   * @return teacherEmail
   */
  
  @JsonProperty("teacherEmail")
  public @Nullable Optional<String> getTeacherEmail() {
    return teacherEmail;
  }

  @JsonProperty("teacherEmail")
  public void setTeacherEmail(@Nullable Optional<String> teacherEmail) {
    this.teacherEmail = teacherEmail;
  }

  public PlagiarismCohortLinkUpdate teacherFirstName(@Nullable Optional<String> teacherFirstName) {
    this.teacherFirstName = teacherFirstName;
    return this;
  }

  /**
   * Teacher first name.
   * @return teacherFirstName
   */
  
  @JsonProperty("teacherFirstName")
  public @Nullable Optional<String> getTeacherFirstName() {
    return teacherFirstName;
  }

  @JsonProperty("teacherFirstName")
  public void setTeacherFirstName(@Nullable Optional<String> teacherFirstName) {
    this.teacherFirstName = teacherFirstName;
  }

  public PlagiarismCohortLinkUpdate teacherLastName(@Nullable Optional<String> teacherLastName) {
    this.teacherLastName = teacherLastName;
    return this;
  }

  /**
   * Teacher last name.
   * @return teacherLastName
   */
  
  @JsonProperty("teacherLastName")
  public @Nullable Optional<String> getTeacherLastName() {
    return teacherLastName;
  }

  @JsonProperty("teacherLastName")
  public void setTeacherLastName(@Nullable Optional<String> teacherLastName) {
    this.teacherLastName = teacherLastName;
  }

  public PlagiarismCohortLinkUpdate classCreated(@Nullable Optional<Boolean> classCreated) {
    this.classCreated = classCreated;
    return this;
  }

  /**
   * Provider class created.
   * @return classCreated
   */
  
  @JsonProperty("classCreated")
  public @Nullable Optional<Boolean> getClassCreated() {
    return classCreated;
  }

  @JsonProperty("classCreated")
  public void setClassCreated(@Nullable Optional<Boolean> classCreated) {
    this.classCreated = classCreated;
  }

  public PlagiarismCohortLinkUpdate assignmentCreated(@Nullable Optional<Boolean> assignmentCreated) {
    this.assignmentCreated = assignmentCreated;
    return this;
  }

  /**
   * Provider assignment created.
   * @return assignmentCreated
   */
  
  @JsonProperty("assignmentCreated")
  public @Nullable Optional<Boolean> getAssignmentCreated() {
    return assignmentCreated;
  }

  @JsonProperty("assignmentCreated")
  public void setAssignmentCreated(@Nullable Optional<Boolean> assignmentCreated) {
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

