// Owned API contract of platform.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * UploadTarget
 */

public class UploadTarget {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String uploadUrl;

  private @Nullable Object uploadHeaders = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime expiresAt;

  public UploadTarget() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UploadTarget(UUID fileId, String uploadUrl, OffsetDateTime expiresAt) {
    this.fileId = fileId;
    this.uploadUrl = uploadUrl;
    this.expiresAt = expiresAt;
  }

  public UploadTarget fileId(UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * File record created in pending state.
   * @return fileId
   */
  @NotNull @Valid 
  @JsonProperty("fileId")
  public UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public UploadTarget uploadUrl(String uploadUrl) {
    this.uploadUrl = uploadUrl;
    return this;
  }

  /**
   * Pre-signed URL to PUT the bytes to.
   * @return uploadUrl
   */
  @NotNull 
  @JsonProperty("uploadUrl")
  public String getUploadUrl() {
    return uploadUrl;
  }

  @JsonProperty("uploadUrl")
  public void setUploadUrl(String uploadUrl) {
    this.uploadUrl = uploadUrl;
  }

  public UploadTarget uploadHeaders(@Nullable Object uploadHeaders) {
    this.uploadHeaders = uploadHeaders;
    return this;
  }

  /**
   * Headers the client must send with the PUT.
   * @return uploadHeaders
   */
  
  @JsonProperty("uploadHeaders")
  public @Nullable Object getUploadHeaders() {
    return uploadHeaders;
  }

  @JsonProperty("uploadHeaders")
  public void setUploadHeaders(@Nullable Object uploadHeaders) {
    this.uploadHeaders = uploadHeaders;
  }

  public UploadTarget expiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * URL expiry.
   * @return expiresAt
   */
  @NotNull @Valid 
  @JsonProperty("expiresAt")
  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  @JsonProperty("expiresAt")
  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadTarget uploadTarget = (UploadTarget) o;
    return Objects.equals(this.fileId, uploadTarget.fileId) &&
        Objects.equals(this.uploadUrl, uploadTarget.uploadUrl) &&
        Objects.equals(this.uploadHeaders, uploadTarget.uploadHeaders) &&
        Objects.equals(this.expiresAt, uploadTarget.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, uploadUrl, uploadHeaders, expiresAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadTarget {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    uploadUrl: ").append(toIndentedString(uploadUrl)).append("\n");
    sb.append("    uploadHeaders: ").append(toIndentedString(uploadHeaders)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
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

