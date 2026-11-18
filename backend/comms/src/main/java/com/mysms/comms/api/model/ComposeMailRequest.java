// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ComposeMailRequest
 */

public class ComposeMailRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> toUserIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> toAddresses = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID aboutStudentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> attachmentFileIds = new ArrayList<>();

  public ComposeMailRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComposeMailRequest(String subject, String body) {
    this.subject = subject;
    this.body = body;
  }

  public ComposeMailRequest toUserIds(List<UUID> toUserIds) {
    this.toUserIds = toUserIds;
    return this;
  }

  public ComposeMailRequest addToUserIdsItem(UUID toUserIdsItem) {
    if (this.toUserIds == null) {
      this.toUserIds = new ArrayList<>();
    }
    this.toUserIds.add(toUserIdsItem);
    return this;
  }

  /**
   * Recipients.
   * @return toUserIds
   */
  
  @JsonProperty("toUserIds")
  public List<UUID> getToUserIds() {
    return toUserIds;
  }

  @JsonProperty("toUserIds")
  public void setToUserIds(List<UUID> toUserIds) {
    this.toUserIds = toUserIds;
  }

  public ComposeMailRequest toAddresses(List<String> toAddresses) {
    this.toAddresses = toAddresses;
    return this;
  }

  public ComposeMailRequest addToAddressesItem(String toAddressesItem) {
    if (this.toAddresses == null) {
      this.toAddresses = new ArrayList<>();
    }
    this.toAddresses.add(toAddressesItem);
    return this;
  }

  /**
   * External addresses.
   * @return toAddresses
   */
  
  @JsonProperty("toAddresses")
  public List<String> getToAddresses() {
    return toAddresses;
  }

  @JsonProperty("toAddresses")
  public void setToAddresses(List<String> toAddresses) {
    this.toAddresses = toAddresses;
  }

  public ComposeMailRequest aboutStudentId(@Nullable UUID aboutStudentId) {
    this.aboutStudentId = aboutStudentId;
    return this;
  }

  /**
   * Student concerned.
   * @return aboutStudentId
   */
  @Valid 
  @JsonProperty("aboutStudentId")
  public @Nullable UUID getAboutStudentId() {
    return aboutStudentId;
  }

  @JsonProperty("aboutStudentId")
  public void setAboutStudentId(@Nullable UUID aboutStudentId) {
    this.aboutStudentId = aboutStudentId;
  }

  public ComposeMailRequest subject(String subject) {
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

  public ComposeMailRequest body(String body) {
    this.body = body;
    return this;
  }

  /**
   * Body.
   * @return body
   */
  @NotNull 
  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(String body) {
    this.body = body;
  }

  public ComposeMailRequest attachmentFileIds(List<UUID> attachmentFileIds) {
    this.attachmentFileIds = attachmentFileIds;
    return this;
  }

  public ComposeMailRequest addAttachmentFileIdsItem(UUID attachmentFileIdsItem) {
    if (this.attachmentFileIds == null) {
      this.attachmentFileIds = new ArrayList<>();
    }
    this.attachmentFileIds.add(attachmentFileIdsItem);
    return this;
  }

  /**
   * Attachments.
   * @return attachmentFileIds
   */
  
  @JsonProperty("attachmentFileIds")
  public List<UUID> getAttachmentFileIds() {
    return attachmentFileIds;
  }

  @JsonProperty("attachmentFileIds")
  public void setAttachmentFileIds(List<UUID> attachmentFileIds) {
    this.attachmentFileIds = attachmentFileIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComposeMailRequest composeMailRequest = (ComposeMailRequest) o;
    return Objects.equals(this.toUserIds, composeMailRequest.toUserIds) &&
        Objects.equals(this.toAddresses, composeMailRequest.toAddresses) &&
        Objects.equals(this.aboutStudentId, composeMailRequest.aboutStudentId) &&
        Objects.equals(this.subject, composeMailRequest.subject) &&
        Objects.equals(this.body, composeMailRequest.body) &&
        Objects.equals(this.attachmentFileIds, composeMailRequest.attachmentFileIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(toUserIds, toAddresses, aboutStudentId, subject, body, attachmentFileIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComposeMailRequest {\n");
    sb.append("    toUserIds: ").append(toIndentedString(toUserIds)).append("\n");
    sb.append("    toAddresses: ").append(toIndentedString(toAddresses)).append("\n");
    sb.append("    aboutStudentId: ").append(toIndentedString(aboutStudentId)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    attachmentFileIds: ").append(toIndentedString(attachmentFileIds)).append("\n");
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

