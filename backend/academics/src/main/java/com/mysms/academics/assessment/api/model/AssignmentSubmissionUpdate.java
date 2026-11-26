// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of AssignmentSubmission that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AssignmentSubmissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> assignmentId;


  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> content;






  public AssignmentSubmissionUpdate assignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
    return this;
  }

  /**
   * Assignment.
   * @return assignmentId
   */
  
  @JsonProperty("assignmentId")
  public @Nullable Optional<UUID> getAssignmentId() {
    return assignmentId;
  }

  @JsonProperty("assignmentId")
  public void setAssignmentId(@Nullable Optional<UUID> assignmentId) {
    this.assignmentId = assignmentId;
  }

  public AssignmentSubmissionUpdate content(@Nullable Optional<String> content) {
    this.content = content;
    return this;
  }

  /**
   * Submission text.
   * @return content
   */
  
  @JsonProperty("content")
  public @Nullable Optional<String> getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(@Nullable Optional<String> content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignmentSubmissionUpdate assignmentSubmissionUpdate = (AssignmentSubmissionUpdate) o;
    return Objects.equals(this.assignmentId, assignmentSubmissionUpdate.assignmentId) &&
        Objects.equals(this.content, assignmentSubmissionUpdate.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignmentId, content);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignmentSubmissionUpdate {\n");
    sb.append("    assignmentId: ").append(toIndentedString(assignmentId)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
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

