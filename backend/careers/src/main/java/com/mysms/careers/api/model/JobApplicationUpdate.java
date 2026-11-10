// Owned API contract of careers (JAVA-CODING-RULES §7); first materialized from api/openapi/careers.yaml on 2026-11-10.
package com.mysms.careers.api.model;

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
 * Fields of JobApplication that can be updated (partial update; see updateMask).
 */

public class JobApplicationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID applicantUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID resumeFileId;

  public JobApplicationUpdate applicantUserId(@Nullable UUID applicantUserId) {
    this.applicantUserId = applicantUserId;
    return this;
  }

  /**
   * Applicant.
   * @return applicantUserId
   */
  @Valid 
  @JsonProperty("applicantUserId")
  public @Nullable UUID getApplicantUserId() {
    return applicantUserId;
  }

  @JsonProperty("applicantUserId")
  public void setApplicantUserId(@Nullable UUID applicantUserId) {
    this.applicantUserId = applicantUserId;
  }

  public JobApplicationUpdate resumeFileId(@Nullable UUID resumeFileId) {
    this.resumeFileId = resumeFileId;
    return this;
  }

  /**
   * Résumé.
   * @return resumeFileId
   */
  @Valid 
  @JsonProperty("resumeFileId")
  public @Nullable UUID getResumeFileId() {
    return resumeFileId;
  }

  @JsonProperty("resumeFileId")
  public void setResumeFileId(@Nullable UUID resumeFileId) {
    this.resumeFileId = resumeFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobApplicationUpdate jobApplicationUpdate = (JobApplicationUpdate) o;
    return Objects.equals(this.applicantUserId, jobApplicationUpdate.applicantUserId) &&
        Objects.equals(this.resumeFileId, jobApplicationUpdate.resumeFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicantUserId, resumeFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobApplicationUpdate {\n");
    sb.append("    applicantUserId: ").append(toIndentedString(applicantUserId)).append("\n");
    sb.append("    resumeFileId: ").append(toIndentedString(resumeFileId)).append("\n");
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

