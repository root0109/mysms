// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * IssueCertificateFromEvaluationRequest
 */

public class IssueCertificateFromEvaluationRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String templateCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate issueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate expiryDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean publish;

  public IssueCertificateFromEvaluationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IssueCertificateFromEvaluationRequest(String templateCode, LocalDate issueDate) {
    this.templateCode = templateCode;
    this.issueDate = issueDate;
  }

  public IssueCertificateFromEvaluationRequest templateCode(String templateCode) {
    this.templateCode = templateCode;
    return this;
  }

  /**
   * Template.
   * @return templateCode
   */
  @NotNull 
  @JsonProperty("templateCode")
  public String getTemplateCode() {
    return templateCode;
  }

  @JsonProperty("templateCode")
  public void setTemplateCode(String templateCode) {
    this.templateCode = templateCode;
  }

  public IssueCertificateFromEvaluationRequest issueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
    return this;
  }

  /**
   * Issue date.
   * @return issueDate
   */
  @NotNull @Valid 
  @JsonProperty("issueDate")
  public LocalDate getIssueDate() {
    return issueDate;
  }

  @JsonProperty("issueDate")
  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public IssueCertificateFromEvaluationRequest expiryDate(@Nullable LocalDate expiryDate) {
    this.expiryDate = expiryDate;
    return this;
  }

  /**
   * Expiry.
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

  public IssueCertificateFromEvaluationRequest publish(@Nullable Boolean publish) {
    this.publish = publish;
    return this;
  }

  /**
   * Publish.
   * @return publish
   */
  
  @JsonProperty("publish")
  public @Nullable Boolean getPublish() {
    return publish;
  }

  @JsonProperty("publish")
  public void setPublish(@Nullable Boolean publish) {
    this.publish = publish;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueCertificateFromEvaluationRequest issueCertificateFromEvaluationRequest = (IssueCertificateFromEvaluationRequest) o;
    return Objects.equals(this.templateCode, issueCertificateFromEvaluationRequest.templateCode) &&
        Objects.equals(this.issueDate, issueCertificateFromEvaluationRequest.issueDate) &&
        Objects.equals(this.expiryDate, issueCertificateFromEvaluationRequest.expiryDate) &&
        Objects.equals(this.publish, issueCertificateFromEvaluationRequest.publish);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateCode, issueDate, expiryDate, publish);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IssueCertificateFromEvaluationRequest {\n");
    sb.append("    templateCode: ").append(toIndentedString(templateCode)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
    sb.append("    expiryDate: ").append(toIndentedString(expiryDate)).append("\n");
    sb.append("    publish: ").append(toIndentedString(publish)).append("\n");
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

