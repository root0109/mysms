// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

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
 * CreateUploadUrlRequest
 */

public class CreateUploadUrlRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String fileName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String mimeType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String sizeBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String checksumSha256;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String ownerEntityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ownerEntityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String collectionName;

  public CreateUploadUrlRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateUploadUrlRequest(String fileName, String mimeType, String sizeBytes) {
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
  }

  public CreateUploadUrlRequest fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * Original name.
   * @return fileName
   */
  @NotNull 
  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  @JsonProperty("fileName")
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public CreateUploadUrlRequest mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  /**
   * MIME type.
   * @return mimeType
   */
  @NotNull 
  @JsonProperty("mimeType")
  public String getMimeType() {
    return mimeType;
  }

  @JsonProperty("mimeType")
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public CreateUploadUrlRequest sizeBytes(String sizeBytes) {
    this.sizeBytes = sizeBytes;
    return this;
  }

  /**
   * Size.
   * @return sizeBytes
   */
  @NotNull @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("sizeBytes")
  public String getSizeBytes() {
    return sizeBytes;
  }

  @JsonProperty("sizeBytes")
  public void setSizeBytes(String sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public CreateUploadUrlRequest checksumSha256(@Nullable String checksumSha256) {
    this.checksumSha256 = checksumSha256;
    return this;
  }

  /**
   * SHA-256 for integrity.
   * @return checksumSha256
   */
  
  @JsonProperty("checksumSha256")
  public @Nullable String getChecksumSha256() {
    return checksumSha256;
  }

  @JsonProperty("checksumSha256")
  public void setChecksumSha256(@Nullable String checksumSha256) {
    this.checksumSha256 = checksumSha256;
  }

  public CreateUploadUrlRequest ownerEntityType(@Nullable String ownerEntityType) {
    this.ownerEntityType = ownerEntityType;
    return this;
  }

  /**
   * Owner type.
   * @return ownerEntityType
   */
  
  @JsonProperty("ownerEntityType")
  public @Nullable String getOwnerEntityType() {
    return ownerEntityType;
  }

  @JsonProperty("ownerEntityType")
  public void setOwnerEntityType(@Nullable String ownerEntityType) {
    this.ownerEntityType = ownerEntityType;
  }

  public CreateUploadUrlRequest ownerEntityId(@Nullable UUID ownerEntityId) {
    this.ownerEntityId = ownerEntityId;
    return this;
  }

  /**
   * Owner record.
   * @return ownerEntityId
   */
  @Valid 
  @JsonProperty("ownerEntityId")
  public @Nullable UUID getOwnerEntityId() {
    return ownerEntityId;
  }

  @JsonProperty("ownerEntityId")
  public void setOwnerEntityId(@Nullable UUID ownerEntityId) {
    this.ownerEntityId = ownerEntityId;
  }

  public CreateUploadUrlRequest collectionName(@Nullable String collectionName) {
    this.collectionName = collectionName;
    return this;
  }

  /**
   * Collection.
   * @return collectionName
   */
  
  @JsonProperty("collectionName")
  public @Nullable String getCollectionName() {
    return collectionName;
  }

  @JsonProperty("collectionName")
  public void setCollectionName(@Nullable String collectionName) {
    this.collectionName = collectionName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateUploadUrlRequest createUploadUrlRequest = (CreateUploadUrlRequest) o;
    return Objects.equals(this.fileName, createUploadUrlRequest.fileName) &&
        Objects.equals(this.mimeType, createUploadUrlRequest.mimeType) &&
        Objects.equals(this.sizeBytes, createUploadUrlRequest.sizeBytes) &&
        Objects.equals(this.checksumSha256, createUploadUrlRequest.checksumSha256) &&
        Objects.equals(this.ownerEntityType, createUploadUrlRequest.ownerEntityType) &&
        Objects.equals(this.ownerEntityId, createUploadUrlRequest.ownerEntityId) &&
        Objects.equals(this.collectionName, createUploadUrlRequest.collectionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, mimeType, sizeBytes, checksumSha256, ownerEntityType, ownerEntityId, collectionName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateUploadUrlRequest {\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
    sb.append("    sizeBytes: ").append(toIndentedString(sizeBytes)).append("\n");
    sb.append("    checksumSha256: ").append(toIndentedString(checksumSha256)).append("\n");
    sb.append("    ownerEntityType: ").append(toIndentedString(ownerEntityType)).append("\n");
    sb.append("    ownerEntityId: ").append(toIndentedString(ownerEntityId)).append("\n");
    sb.append("    collectionName: ").append(toIndentedString(collectionName)).append("\n");
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

