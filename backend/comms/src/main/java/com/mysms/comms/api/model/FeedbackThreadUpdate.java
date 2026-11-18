// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of FeedbackThread that can be updated (partial update; see updateMask).
 */

public class FeedbackThreadUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID guardianId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assignedToUserId;

  public FeedbackThreadUpdate institutionId(@Nullable UUID institutionId) {
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

  public FeedbackThreadUpdate guardianId(@Nullable UUID guardianId) {
    this.guardianId = guardianId;
    return this;
  }

  /**
   * Guardian.
   * @return guardianId
   */
  @Valid 
  @JsonProperty("guardianId")
  public @Nullable UUID getGuardianId() {
    return guardianId;
  }

  @JsonProperty("guardianId")
  public void setGuardianId(@Nullable UUID guardianId) {
    this.guardianId = guardianId;
  }

  public FeedbackThreadUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student concerned.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public FeedbackThreadUpdate assignedToUserId(@Nullable UUID assignedToUserId) {
    this.assignedToUserId = assignedToUserId;
    return this;
  }

  /**
   * Staff handling it.
   * @return assignedToUserId
   */
  @Valid 
  @JsonProperty("assignedToUserId")
  public @Nullable UUID getAssignedToUserId() {
    return assignedToUserId;
  }

  @JsonProperty("assignedToUserId")
  public void setAssignedToUserId(@Nullable UUID assignedToUserId) {
    this.assignedToUserId = assignedToUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeedbackThreadUpdate feedbackThreadUpdate = (FeedbackThreadUpdate) o;
    return Objects.equals(this.institutionId, feedbackThreadUpdate.institutionId) &&
        Objects.equals(this.guardianId, feedbackThreadUpdate.guardianId) &&
        Objects.equals(this.studentId, feedbackThreadUpdate.studentId) &&
        Objects.equals(this.assignedToUserId, feedbackThreadUpdate.assignedToUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, guardianId, studentId, assignedToUserId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeedbackThreadUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    guardianId: ").append(toIndentedString(guardianId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    assignedToUserId: ").append(toIndentedString(assignedToUserId)).append("\n");
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

