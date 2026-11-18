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
 * Fields of ClassPageAttachment that can be updated (partial update; see updateMask).
 */

public class ClassPageAttachmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public ClassPageAttachmentUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * File.
   * @return fileId
   */
  @Valid 
  @JsonProperty("fileId")
  public @Nullable UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable UUID fileId) {
    this.fileId = fileId;
  }

  public ClassPageAttachmentUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Visible.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClassPageAttachmentUpdate classPageAttachmentUpdate = (ClassPageAttachmentUpdate) o;
    return Objects.equals(this.fileId, classPageAttachmentUpdate.fileId) &&
        Objects.equals(this.isActive, classPageAttachmentUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassPageAttachmentUpdate {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

