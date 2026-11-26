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
 * Fields of FeedbackThread that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class FeedbackThreadUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> guardianId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> assignedToUserId;

  public FeedbackThreadUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public FeedbackThreadUpdate guardianId(@Nullable Optional<UUID> guardianId) {
    this.guardianId = guardianId;
    return this;
  }

  /**
   * Guardian.
   * @return guardianId
   */
  
  @JsonProperty("guardianId")
  public @Nullable Optional<UUID> getGuardianId() {
    return guardianId;
  }

  @JsonProperty("guardianId")
  public void setGuardianId(@Nullable Optional<UUID> guardianId) {
    this.guardianId = guardianId;
  }

  public FeedbackThreadUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student concerned.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public FeedbackThreadUpdate assignedToUserId(@Nullable Optional<UUID> assignedToUserId) {
    this.assignedToUserId = assignedToUserId;
    return this;
  }

  /**
   * Staff handling it.
   * @return assignedToUserId
   */
  
  @JsonProperty("assignedToUserId")
  public @Nullable Optional<UUID> getAssignedToUserId() {
    return assignedToUserId;
  }

  @JsonProperty("assignedToUserId")
  public void setAssignedToUserId(@Nullable Optional<UUID> assignedToUserId) {
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

