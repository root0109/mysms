// Owned API contract of platform.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Every stored file (documents, images, videos, attachments). Other tables reference files through *_file_id.
 */

public class FileObject {

  /**
   * Lifecycle: pending (upload started), available, quarantined (failed scanning), deleted (storage reclaimed).
   */
  public enum StatusEnum {
    PENDING("pending"),
    
    AVAILABLE("available"),
    
    QUARANTINED("quarantined"),
    
    DELETED("deleted");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scannedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID uploadedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String fileName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String originalName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mimeType;

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  /**
   * Kind of file (from the fixed content-type catalogue), used for icons, viewers and processing.
   */
  public enum FileKindEnum {
    DOCUMENT("document"),
    
    PRESENTATION("presentation"),
    
    SPREADSHEET("spreadsheet"),
    
    PDF("pdf"),
    
    IMAGE("image"),
    
    AUDIO("audio"),
    
    VIDEO("video"),
    
    ARCHIVE("archive"),
    
    PACKAGE("package"),
    
    EBOOK("ebook"),
    
    OTHER("other");

    private final String value;

    FileKindEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static FileKindEnum fromValue(String value) {
      for (FileKindEnum b : FileKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable FileKindEnum fileKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isShared;

  /**
   * State of post-upload processing (package extraction, document-to-e-book conversion, video transcoding).
   */
  public enum ProcessingStatusEnum {
    NONE("none"),
    
    PENDING("pending"),
    
    IN_PROGRESS("in_progress"),
    
    SUCCEEDED("succeeded"),
    
    FAILED("failed");

    private final String value;

    ProcessingStatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ProcessingStatusEnum fromValue(String value) {
      for (ProcessingStatusEnum b : ProcessingStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ProcessingStatusEnum processingStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer processingAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String processingError;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public FileObject() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FileObject(String fileName, String sizeBytes) {
    this.fileName = fileName;
    this.sizeBytes = sizeBytes;
  }

  public FileObject status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Lifecycle: pending (upload started), available, quarantined (failed scanning), deleted (storage reclaimed).
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public FileObject scannedAt(@Nullable OffsetDateTime scannedAt) {
    this.scannedAt = scannedAt;
    return this;
  }

  /**
   * When malware/content scanning finished.
   * @return scannedAt
   */
  @Valid 
  @JsonProperty("scannedAt")
  public @Nullable OffsetDateTime getScannedAt() {
    return scannedAt;
  }

  @JsonProperty("scannedAt")
  public void setScannedAt(@Nullable OffsetDateTime scannedAt) {
    this.scannedAt = scannedAt;
  }

  public FileObject uploadedByUserId(@Nullable UUID uploadedByUserId) {
    this.uploadedByUserId = uploadedByUserId;
    return this;
  }

  /**
   * User who uploaded the file.
   * @return uploadedByUserId
   */
  @Valid 
  @JsonProperty("uploadedByUserId")
  public @Nullable UUID getUploadedByUserId() {
    return uploadedByUserId;
  }

  @JsonProperty("uploadedByUserId")
  public void setUploadedByUserId(@Nullable UUID uploadedByUserId) {
    this.uploadedByUserId = uploadedByUserId;
  }

  public FileObject id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public FileObject fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * Stored file name.
   * @return fileName
   */
  @NotNull @Size(max = 255) 
  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  @JsonProperty("fileName")
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public FileObject originalName(@Nullable String originalName) {
    this.originalName = originalName;
    return this;
  }

  /**
   * Name of the file as uploaded.
   * @return originalName
   */
  @Size(max = 255) 
  @JsonProperty("originalName")
  public @Nullable String getOriginalName() {
    return originalName;
  }

  @JsonProperty("originalName")
  public void setOriginalName(@Nullable String originalName) {
    this.originalName = originalName;
  }

  public FileObject mimeType(@Nullable String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  /**
   * MIME type.
   * @return mimeType
   */
  @Size(max = 150) 
  @JsonProperty("mimeType")
  public @Nullable String getMimeType() {
    return mimeType;
  }

  @JsonProperty("mimeType")
  public void setMimeType(@Nullable String mimeType) {
    this.mimeType = mimeType;
  }

  public FileObject sizeBytes(String sizeBytes) {
    this.sizeBytes = sizeBytes;
    return this;
  }

  /**
   * File size in bytes.
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

  public FileObject checksumSha256(@Nullable String checksumSha256) {
    this.checksumSha256 = checksumSha256;
    return this;
  }

  /**
   * SHA-256 of the content.
   * @return checksumSha256
   */
  @Size(max = 64) 
  @JsonProperty("checksumSha256")
  public @Nullable String getChecksumSha256() {
    return checksumSha256;
  }

  @JsonProperty("checksumSha256")
  public void setChecksumSha256(@Nullable String checksumSha256) {
    this.checksumSha256 = checksumSha256;
  }

  public FileObject ownerEntityType(@Nullable String ownerEntityType) {
    this.ownerEntityType = ownerEntityType;
    return this;
  }

  /**
   * Polymorphic owner type (table name) when the file belongs to one record.
   * @return ownerEntityType
   */
  @Size(max = 80) 
  @JsonProperty("ownerEntityType")
  public @Nullable String getOwnerEntityType() {
    return ownerEntityType;
  }

  @JsonProperty("ownerEntityType")
  public void setOwnerEntityType(@Nullable String ownerEntityType) {
    this.ownerEntityType = ownerEntityType;
  }

  public FileObject ownerEntityId(@Nullable UUID ownerEntityId) {
    this.ownerEntityId = ownerEntityId;
    return this;
  }

  /**
   * Polymorphic owner record id (pairs with owner_entity_type).
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

  public FileObject collectionName(@Nullable String collectionName) {
    this.collectionName = collectionName;
    return this;
  }

  /**
   * Logical collection within the owner, e.g. avatar, gallery.
   * @return collectionName
   */
  @Size(max = 80) 
  @JsonProperty("collectionName")
  public @Nullable String getCollectionName() {
    return collectionName;
  }

  @JsonProperty("collectionName")
  public void setCollectionName(@Nullable String collectionName) {
    this.collectionName = collectionName;
  }

  public FileObject sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Order within the collection.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public FileObject fileKind(@Nullable FileKindEnum fileKind) {
    this.fileKind = fileKind;
    return this;
  }

  /**
   * Kind of file (from the fixed content-type catalogue), used for icons, viewers and processing.
   * @return fileKind
   */
  
  @JsonProperty("fileKind")
  public @Nullable FileKindEnum getFileKind() {
    return fileKind;
  }

  @JsonProperty("fileKind")
  public void setFileKind(@Nullable FileKindEnum fileKind) {
    this.fileKind = fileKind;
  }

  public FileObject isShared(@Nullable Boolean isShared) {
    this.isShared = isShared;
    return this;
  }

  /**
   * Shared with a group of users (readable beyond the owner).
   * @return isShared
   */
  
  @JsonProperty("isShared")
  public @Nullable Boolean getIsShared() {
    return isShared;
  }

  @JsonProperty("isShared")
  public void setIsShared(@Nullable Boolean isShared) {
    this.isShared = isShared;
  }

  public FileObject processingStatus(@Nullable ProcessingStatusEnum processingStatus) {
    this.processingStatus = processingStatus;
    return this;
  }

  /**
   * State of post-upload processing (package extraction, document-to-e-book conversion, video transcoding).
   * @return processingStatus
   */
  
  @JsonProperty("processingStatus")
  public @Nullable ProcessingStatusEnum getProcessingStatus() {
    return processingStatus;
  }

  @JsonProperty("processingStatus")
  public void setProcessingStatus(@Nullable ProcessingStatusEnum processingStatus) {
    this.processingStatus = processingStatus;
  }

  public FileObject processingAttempts(@Nullable Integer processingAttempts) {
    this.processingAttempts = processingAttempts;
    return this;
  }

  /**
   * Processing attempts so far.
   * @return processingAttempts
   */
  
  @JsonProperty("processingAttempts")
  public @Nullable Integer getProcessingAttempts() {
    return processingAttempts;
  }

  @JsonProperty("processingAttempts")
  public void setProcessingAttempts(@Nullable Integer processingAttempts) {
    this.processingAttempts = processingAttempts;
  }

  public FileObject processingError(@Nullable String processingError) {
    this.processingError = processingError;
    return this;
  }

  /**
   * Last processing error.
   * @return processingError
   */
  
  @JsonProperty("processingError")
  public @Nullable String getProcessingError() {
    return processingError;
  }

  @JsonProperty("processingError")
  public void setProcessingError(@Nullable String processingError) {
    this.processingError = processingError;
  }

  public FileObject createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public FileObject createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public FileObject updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public FileObject updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public FileObject etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileObject fileObject = (FileObject) o;
    return Objects.equals(this.status, fileObject.status) &&
        Objects.equals(this.scannedAt, fileObject.scannedAt) &&
        Objects.equals(this.uploadedByUserId, fileObject.uploadedByUserId) &&
        Objects.equals(this.id, fileObject.id) &&
        Objects.equals(this.fileName, fileObject.fileName) &&
        Objects.equals(this.originalName, fileObject.originalName) &&
        Objects.equals(this.mimeType, fileObject.mimeType) &&
        Objects.equals(this.sizeBytes, fileObject.sizeBytes) &&
        Objects.equals(this.checksumSha256, fileObject.checksumSha256) &&
        Objects.equals(this.ownerEntityType, fileObject.ownerEntityType) &&
        Objects.equals(this.ownerEntityId, fileObject.ownerEntityId) &&
        Objects.equals(this.collectionName, fileObject.collectionName) &&
        Objects.equals(this.sortOrder, fileObject.sortOrder) &&
        Objects.equals(this.fileKind, fileObject.fileKind) &&
        Objects.equals(this.isShared, fileObject.isShared) &&
        Objects.equals(this.processingStatus, fileObject.processingStatus) &&
        Objects.equals(this.processingAttempts, fileObject.processingAttempts) &&
        Objects.equals(this.processingError, fileObject.processingError) &&
        Objects.equals(this.createTime, fileObject.createTime) &&
        Objects.equals(this.createdBy, fileObject.createdBy) &&
        Objects.equals(this.updateTime, fileObject.updateTime) &&
        Objects.equals(this.updatedBy, fileObject.updatedBy) &&
        Objects.equals(this.etag, fileObject.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, scannedAt, uploadedByUserId, id, fileName, originalName, mimeType, sizeBytes, checksumSha256, ownerEntityType, ownerEntityId, collectionName, sortOrder, fileKind, isShared, processingStatus, processingAttempts, processingError, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileObject {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    scannedAt: ").append(toIndentedString(scannedAt)).append("\n");
    sb.append("    uploadedByUserId: ").append(toIndentedString(uploadedByUserId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    originalName: ").append(toIndentedString(originalName)).append("\n");
    sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
    sb.append("    sizeBytes: ").append(toIndentedString(sizeBytes)).append("\n");
    sb.append("    checksumSha256: ").append(toIndentedString(checksumSha256)).append("\n");
    sb.append("    ownerEntityType: ").append(toIndentedString(ownerEntityType)).append("\n");
    sb.append("    ownerEntityId: ").append(toIndentedString(ownerEntityId)).append("\n");
    sb.append("    collectionName: ").append(toIndentedString(collectionName)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    fileKind: ").append(toIndentedString(fileKind)).append("\n");
    sb.append("    isShared: ").append(toIndentedString(isShared)).append("\n");
    sb.append("    processingStatus: ").append(toIndentedString(processingStatus)).append("\n");
    sb.append("    processingAttempts: ").append(toIndentedString(processingAttempts)).append("\n");
    sb.append("    processingError: ").append(toIndentedString(processingError)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

