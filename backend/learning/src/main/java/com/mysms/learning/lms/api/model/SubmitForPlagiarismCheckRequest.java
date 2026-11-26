// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * SubmitForPlagiarismCheckRequest
 */

public class SubmitForPlagiarismCheckRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID entityAttachmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID plagiarismAccountId;

  public SubmitForPlagiarismCheckRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SubmitForPlagiarismCheckRequest(UUID entityAttachmentId) {
    this.entityAttachmentId = entityAttachmentId;
  }

  public SubmitForPlagiarismCheckRequest entityAttachmentId(UUID entityAttachmentId) {
    this.entityAttachmentId = entityAttachmentId;
    return this;
  }

  /**
   * Submitted attachment.
   * @return entityAttachmentId
   */
  @NotNull @Valid 
  @JsonProperty("entityAttachmentId")
  public UUID getEntityAttachmentId() {
    return entityAttachmentId;
  }

  @JsonProperty("entityAttachmentId")
  public void setEntityAttachmentId(UUID entityAttachmentId) {
    this.entityAttachmentId = entityAttachmentId;
  }

  public SubmitForPlagiarismCheckRequest plagiarismAccountId(@Nullable UUID plagiarismAccountId) {
    this.plagiarismAccountId = plagiarismAccountId;
    return this;
  }

  /**
   * Account (default tenant default).
   * @return plagiarismAccountId
   */
  @Valid 
  @JsonProperty("plagiarismAccountId")
  public @Nullable UUID getPlagiarismAccountId() {
    return plagiarismAccountId;
  }

  @JsonProperty("plagiarismAccountId")
  public void setPlagiarismAccountId(@Nullable UUID plagiarismAccountId) {
    this.plagiarismAccountId = plagiarismAccountId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubmitForPlagiarismCheckRequest submitForPlagiarismCheckRequest = (SubmitForPlagiarismCheckRequest) o;
    return Objects.equals(this.entityAttachmentId, submitForPlagiarismCheckRequest.entityAttachmentId) &&
        Objects.equals(this.plagiarismAccountId, submitForPlagiarismCheckRequest.plagiarismAccountId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityAttachmentId, plagiarismAccountId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubmitForPlagiarismCheckRequest {\n");
    sb.append("    entityAttachmentId: ").append(toIndentedString(entityAttachmentId)).append("\n");
    sb.append("    plagiarismAccountId: ").append(toIndentedString(plagiarismAccountId)).append("\n");
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

