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
 * ReplyToHomeworkFeedbackRequest
 */

public class ReplyToHomeworkFeedbackRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String studentReply;

  public ReplyToHomeworkFeedbackRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReplyToHomeworkFeedbackRequest(String studentReply) {
    this.studentReply = studentReply;
  }

  public ReplyToHomeworkFeedbackRequest etag(@Nullable String etag) {
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

  public ReplyToHomeworkFeedbackRequest studentReply(String studentReply) {
    this.studentReply = studentReply;
    return this;
  }

  /**
   * Reply.
   * @return studentReply
   */
  @NotNull 
  @JsonProperty("studentReply")
  public String getStudentReply() {
    return studentReply;
  }

  @JsonProperty("studentReply")
  public void setStudentReply(String studentReply) {
    this.studentReply = studentReply;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReplyToHomeworkFeedbackRequest replyToHomeworkFeedbackRequest = (ReplyToHomeworkFeedbackRequest) o;
    return Objects.equals(this.etag, replyToHomeworkFeedbackRequest.etag) &&
        Objects.equals(this.studentReply, replyToHomeworkFeedbackRequest.studentReply);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, studentReply);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReplyToHomeworkFeedbackRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    studentReply: ").append(toIndentedString(studentReply)).append("\n");
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

