// Owned API contract of careers.
package com.mysms.students.careers.api.model;

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
 * EmailApplicantRequest
 */

public class EmailApplicantRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String message;

  public EmailApplicantRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmailApplicantRequest(String subject, String message) {
    this.subject = subject;
    this.message = message;
  }

  public EmailApplicantRequest subject(String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Subject.
   * @return subject
   */
  @NotNull 
  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  public EmailApplicantRequest message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Body.
   * @return message
   */
  @NotNull 
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailApplicantRequest emailApplicantRequest = (EmailApplicantRequest) o;
    return Objects.equals(this.subject, emailApplicantRequest.subject) &&
        Objects.equals(this.message, emailApplicantRequest.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmailApplicantRequest {\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

