// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * EnrollApplicantRequest
 */

public class EnrollApplicantRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String rollNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean createPortalAccounts;

  public EnrollApplicantRequest classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (K-12).
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

  public EnrollApplicantRequest batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (higher education).
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public EnrollApplicantRequest rollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
    return this;
  }

  /**
   * Roll number.
   * @return rollNumber
   */
  
  @JsonProperty("rollNumber")
  public @Nullable String getRollNumber() {
    return rollNumber;
  }

  @JsonProperty("rollNumber")
  public void setRollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
  }

  public EnrollApplicantRequest createPortalAccounts(@Nullable Boolean createPortalAccounts) {
    this.createPortalAccounts = createPortalAccounts;
    return this;
  }

  /**
   * Create student / guardian logins.
   * @return createPortalAccounts
   */
  
  @JsonProperty("createPortalAccounts")
  public @Nullable Boolean getCreatePortalAccounts() {
    return createPortalAccounts;
  }

  @JsonProperty("createPortalAccounts")
  public void setCreatePortalAccounts(@Nullable Boolean createPortalAccounts) {
    this.createPortalAccounts = createPortalAccounts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrollApplicantRequest enrollApplicantRequest = (EnrollApplicantRequest) o;
    return Objects.equals(this.classSectionId, enrollApplicantRequest.classSectionId) &&
        Objects.equals(this.batchId, enrollApplicantRequest.batchId) &&
        Objects.equals(this.rollNumber, enrollApplicantRequest.rollNumber) &&
        Objects.equals(this.createPortalAccounts, enrollApplicantRequest.createPortalAccounts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classSectionId, batchId, rollNumber, createPortalAccounts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrollApplicantRequest {\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    rollNumber: ").append(toIndentedString(rollNumber)).append("\n");
    sb.append("    createPortalAccounts: ").append(toIndentedString(createPortalAccounts)).append("\n");
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

