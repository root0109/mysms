// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GradeAssignmentSubmissionRequest
 */

public class GradeAssignmentSubmissionRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String marks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  public GradeAssignmentSubmissionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradeAssignmentSubmissionRequest(String marks) {
    this.marks = marks;
  }

  public GradeAssignmentSubmissionRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  public GradeAssignmentSubmissionRequest marks(String marks) {
    this.marks = marks;
    return this;
  }

  /**
   * Marks.
   * @return marks
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("marks")
  public String getMarks() {
    return marks;
  }

  @JsonProperty("marks")
  public void setMarks(String marks) {
    this.marks = marks;
  }

  public GradeAssignmentSubmissionRequest feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback.
   * @return feedback
   */
  
  @JsonProperty("feedback")
  public @Nullable String getFeedback() {
    return feedback;
  }

  @JsonProperty("feedback")
  public void setFeedback(@Nullable String feedback) {
    this.feedback = feedback;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradeAssignmentSubmissionRequest gradeAssignmentSubmissionRequest = (GradeAssignmentSubmissionRequest) o;
    return Objects.equals(this.etag, gradeAssignmentSubmissionRequest.etag) &&
        Objects.equals(this.marks, gradeAssignmentSubmissionRequest.marks) &&
        Objects.equals(this.feedback, gradeAssignmentSubmissionRequest.feedback);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, marks, feedback);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradeAssignmentSubmissionRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    marks: ").append(toIndentedString(marks)).append("\n");
    sb.append("    feedback: ").append(toIndentedString(feedback)).append("\n");
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

