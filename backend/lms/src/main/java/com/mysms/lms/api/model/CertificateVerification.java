// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CertificateVerification
 */

public class CertificateVerification {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean valid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String studentName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate issueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate expiryDate;

  public CertificateVerification() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CertificateVerification(Boolean valid) {
    this.valid = valid;
  }

  public CertificateVerification valid(Boolean valid) {
    this.valid = valid;
    return this;
  }

  /**
   * Certificate exists and is published.
   * @return valid
   */
  @NotNull 
  @JsonProperty("valid")
  public Boolean getValid() {
    return valid;
  }

  @JsonProperty("valid")
  public void setValid(Boolean valid) {
    this.valid = valid;
  }

  public CertificateVerification studentName(@Nullable String studentName) {
    this.studentName = studentName;
    return this;
  }

  /**
   * Student.
   * @return studentName
   */
  
  @JsonProperty("studentName")
  public @Nullable String getStudentName() {
    return studentName;
  }

  @JsonProperty("studentName")
  public void setStudentName(@Nullable String studentName) {
    this.studentName = studentName;
  }

  public CertificateVerification title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Course / cohort.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public CertificateVerification issueDate(@Nullable LocalDate issueDate) {
    this.issueDate = issueDate;
    return this;
  }

  /**
   * Issued.
   * @return issueDate
   */
  @Valid 
  @JsonProperty("issueDate")
  public @Nullable LocalDate getIssueDate() {
    return issueDate;
  }

  @JsonProperty("issueDate")
  public void setIssueDate(@Nullable LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public CertificateVerification expiryDate(@Nullable LocalDate expiryDate) {
    this.expiryDate = expiryDate;
    return this;
  }

  /**
   * Expires.
   * @return expiryDate
   */
  @Valid 
  @JsonProperty("expiryDate")
  public @Nullable LocalDate getExpiryDate() {
    return expiryDate;
  }

  @JsonProperty("expiryDate")
  public void setExpiryDate(@Nullable LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CertificateVerification certificateVerification = (CertificateVerification) o;
    return Objects.equals(this.valid, certificateVerification.valid) &&
        Objects.equals(this.studentName, certificateVerification.studentName) &&
        Objects.equals(this.title, certificateVerification.title) &&
        Objects.equals(this.issueDate, certificateVerification.issueDate) &&
        Objects.equals(this.expiryDate, certificateVerification.expiryDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valid, studentName, title, issueDate, expiryDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CertificateVerification {\n");
    sb.append("    valid: ").append(toIndentedString(valid)).append("\n");
    sb.append("    studentName: ").append(toIndentedString(studentName)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
    sb.append("    expiryDate: ").append(toIndentedString(expiryDate)).append("\n");
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

