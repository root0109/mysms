// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GenerateCohortCertificatesRequest
 */

public class GenerateCohortCertificatesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> userIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String templateCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate issueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean publish;

  public GenerateCohortCertificatesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GenerateCohortCertificatesRequest(String templateCode, LocalDate issueDate) {
    this.templateCode = templateCode;
    this.issueDate = issueDate;
  }

  public GenerateCohortCertificatesRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public GenerateCohortCertificatesRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Subset.
   * @return userIds
   */
  
  @JsonProperty("userIds")
  public List<UUID> getUserIds() {
    return userIds;
  }

  @JsonProperty("userIds")
  public void setUserIds(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public GenerateCohortCertificatesRequest templateCode(String templateCode) {
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

  public GenerateCohortCertificatesRequest issueDate(LocalDate issueDate) {
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

  public GenerateCohortCertificatesRequest publish(@Nullable Boolean publish) {
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
    GenerateCohortCertificatesRequest generateCohortCertificatesRequest = (GenerateCohortCertificatesRequest) o;
    return Objects.equals(this.userIds, generateCohortCertificatesRequest.userIds) &&
        Objects.equals(this.templateCode, generateCohortCertificatesRequest.templateCode) &&
        Objects.equals(this.issueDate, generateCohortCertificatesRequest.issueDate) &&
        Objects.equals(this.publish, generateCohortCertificatesRequest.publish);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds, templateCode, issueDate, publish);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GenerateCohortCertificatesRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    templateCode: ").append(toIndentedString(templateCode)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
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

