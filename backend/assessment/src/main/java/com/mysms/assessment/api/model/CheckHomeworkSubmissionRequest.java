// Owned API contract of assessment.
package com.mysms.assessment.api.model;

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
 * CheckHomeworkSubmissionRequest
 */

public class CheckHomeworkSubmissionRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherComments;

  public CheckHomeworkSubmissionRequest etag(@Nullable String etag) {
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

  public CheckHomeworkSubmissionRequest teacherComments(@Nullable String teacherComments) {
    this.teacherComments = teacherComments;
    return this;
  }

  /**
   * Comments.
   * @return teacherComments
   */
  
  @JsonProperty("teacherComments")
  public @Nullable String getTeacherComments() {
    return teacherComments;
  }

  @JsonProperty("teacherComments")
  public void setTeacherComments(@Nullable String teacherComments) {
    this.teacherComments = teacherComments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckHomeworkSubmissionRequest checkHomeworkSubmissionRequest = (CheckHomeworkSubmissionRequest) o;
    return Objects.equals(this.etag, checkHomeworkSubmissionRequest.etag) &&
        Objects.equals(this.teacherComments, checkHomeworkSubmissionRequest.teacherComments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, teacherComments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckHomeworkSubmissionRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    teacherComments: ").append(toIndentedString(teacherComments)).append("\n");
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

