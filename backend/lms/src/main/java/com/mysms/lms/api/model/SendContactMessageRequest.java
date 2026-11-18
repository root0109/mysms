// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * SendContactMessageRequest
 */

public class SendContactMessageRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String content;

  public SendContactMessageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SendContactMessageRequest(String subject, String content) {
    this.subject = subject;
    this.content = content;
  }

  public SendContactMessageRequest subject(String subject) {
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

  public SendContactMessageRequest content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Message.
   * @return content
   */
  @NotNull 
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
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
    SendContactMessageRequest sendContactMessageRequest = (SendContactMessageRequest) o;
    return Objects.equals(this.subject, sendContactMessageRequest.subject) &&
        Objects.equals(this.content, sendContactMessageRequest.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, content);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SendContactMessageRequest {\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
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

