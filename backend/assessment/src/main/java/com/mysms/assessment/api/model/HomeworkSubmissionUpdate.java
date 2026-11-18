// Owned API contract of assessment.
package com.mysms.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of HomeworkSubmission that can be updated (partial update; see updateMask).
 */

public class HomeworkSubmissionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID homeworkId;


  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate submittedOn;

  public HomeworkSubmissionUpdate homeworkId(@Nullable UUID homeworkId) {
    this.homeworkId = homeworkId;
    return this;
  }

  /**
   * Homework.
   * @return homeworkId
   */
  @Valid 
  @JsonProperty("homeworkId")
  public @Nullable UUID getHomeworkId() {
    return homeworkId;
  }

  @JsonProperty("homeworkId")
  public void setHomeworkId(@Nullable UUID homeworkId) {
    this.homeworkId = homeworkId;
  }

  public HomeworkSubmissionUpdate submittedOn(@Nullable LocalDate submittedOn) {
    this.submittedOn = submittedOn;
    return this;
  }

  /**
   * Submission date.
   * @return submittedOn
   */
  @Valid 
  @JsonProperty("submittedOn")
  public @Nullable LocalDate getSubmittedOn() {
    return submittedOn;
  }

  @JsonProperty("submittedOn")
  public void setSubmittedOn(@Nullable LocalDate submittedOn) {
    this.submittedOn = submittedOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HomeworkSubmissionUpdate homeworkSubmissionUpdate = (HomeworkSubmissionUpdate) o;
    return Objects.equals(this.homeworkId, homeworkSubmissionUpdate.homeworkId) &&
        Objects.equals(this.submittedOn, homeworkSubmissionUpdate.submittedOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(homeworkId, submittedOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HomeworkSubmissionUpdate {\n");
    sb.append("    homeworkId: ").append(toIndentedString(homeworkId)).append("\n");
    sb.append("    submittedOn: ").append(toIndentedString(submittedOn)).append("\n");
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

