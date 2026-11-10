// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of DriveItem that can be updated (partial update; see updateMask).
 */

public class DriveItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String itemType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String icon;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String keywords;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String sizeBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String linkUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime uploadedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isShared;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer likeCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer dislikeCount;

  public DriveItemUpdate ownerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Owner.
   * @return ownerUserId
   */
  @Valid 
  @JsonProperty("ownerUserId")
  public @Nullable UUID getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public DriveItemUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Stored file (NULL for a link).
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

  public DriveItemUpdate itemType(@Nullable String itemType) {
    this.itemType = itemType;
    return this;
  }

  /**
   * File type label (document, presentation, video, link...).
   * @return itemType
   */
  @Size(max = 40) 
  @JsonProperty("itemType")
  public @Nullable String getItemType() {
    return itemType;
  }

  @JsonProperty("itemType")
  public void setItemType(@Nullable String itemType) {
    this.itemType = itemType;
  }

  public DriveItemUpdate icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * Icon key shown in the drive.
   * @return icon
   */
  @Size(max = 100) 
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  @JsonProperty("icon")
  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public DriveItemUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public DriveItemUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public DriveItemUpdate keywords(@Nullable String keywords) {
    this.keywords = keywords;
    return this;
  }

  /**
   * Search keywords.
   * @return keywords
   */
  @Size(max = 500) 
  @JsonProperty("keywords")
  public @Nullable String getKeywords() {
    return keywords;
  }

  @JsonProperty("keywords")
  public void setKeywords(@Nullable String keywords) {
    this.keywords = keywords;
  }

  public DriveItemUpdate sizeBytes(@Nullable String sizeBytes) {
    this.sizeBytes = sizeBytes;
    return this;
  }

  /**
   * Size in bytes.
   * @return sizeBytes
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("sizeBytes")
  public @Nullable String getSizeBytes() {
    return sizeBytes;
  }

  @JsonProperty("sizeBytes")
  public void setSizeBytes(@Nullable String sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public DriveItemUpdate linkUrl(@Nullable String linkUrl) {
    this.linkUrl = linkUrl;
    return this;
  }

  /**
   * URL when the item is a link.
   * @return linkUrl
   */
  @Size(max = 1000) 
  @JsonProperty("linkUrl")
  public @Nullable String getLinkUrl() {
    return linkUrl;
  }

  @JsonProperty("linkUrl")
  public void setLinkUrl(@Nullable String linkUrl) {
    this.linkUrl = linkUrl;
  }

  public DriveItemUpdate uploadedAt(@Nullable OffsetDateTime uploadedAt) {
    this.uploadedAt = uploadedAt;
    return this;
  }

  /**
   * Upload time.
   * @return uploadedAt
   */
  @Valid 
  @JsonProperty("uploadedAt")
  public @Nullable OffsetDateTime getUploadedAt() {
    return uploadedAt;
  }

  @JsonProperty("uploadedAt")
  public void setUploadedAt(@Nullable OffsetDateTime uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public DriveItemUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course the item relates to.
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public DriveItemUpdate isShared(@Nullable Boolean isShared) {
    this.isShared = isShared;
    return this;
  }

  /**
   * Shared with at least one user (see platform.drive_item_share).
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

  public DriveItemUpdate likeCount(@Nullable Integer likeCount) {
    this.likeCount = likeCount;
    return this;
  }

  /**
   * Cached likes.
   * @return likeCount
   */
  
  @JsonProperty("likeCount")
  public @Nullable Integer getLikeCount() {
    return likeCount;
  }

  @JsonProperty("likeCount")
  public void setLikeCount(@Nullable Integer likeCount) {
    this.likeCount = likeCount;
  }

  public DriveItemUpdate dislikeCount(@Nullable Integer dislikeCount) {
    this.dislikeCount = dislikeCount;
    return this;
  }

  /**
   * Cached dislikes.
   * @return dislikeCount
   */
  
  @JsonProperty("dislikeCount")
  public @Nullable Integer getDislikeCount() {
    return dislikeCount;
  }

  @JsonProperty("dislikeCount")
  public void setDislikeCount(@Nullable Integer dislikeCount) {
    this.dislikeCount = dislikeCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DriveItemUpdate driveItemUpdate = (DriveItemUpdate) o;
    return Objects.equals(this.ownerUserId, driveItemUpdate.ownerUserId) &&
        Objects.equals(this.fileId, driveItemUpdate.fileId) &&
        Objects.equals(this.itemType, driveItemUpdate.itemType) &&
        Objects.equals(this.icon, driveItemUpdate.icon) &&
        Objects.equals(this.title, driveItemUpdate.title) &&
        Objects.equals(this.description, driveItemUpdate.description) &&
        Objects.equals(this.keywords, driveItemUpdate.keywords) &&
        Objects.equals(this.sizeBytes, driveItemUpdate.sizeBytes) &&
        Objects.equals(this.linkUrl, driveItemUpdate.linkUrl) &&
        Objects.equals(this.uploadedAt, driveItemUpdate.uploadedAt) &&
        Objects.equals(this.courseId, driveItemUpdate.courseId) &&
        Objects.equals(this.isShared, driveItemUpdate.isShared) &&
        Objects.equals(this.likeCount, driveItemUpdate.likeCount) &&
        Objects.equals(this.dislikeCount, driveItemUpdate.dislikeCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerUserId, fileId, itemType, icon, title, description, keywords, sizeBytes, linkUrl, uploadedAt, courseId, isShared, likeCount, dislikeCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DriveItemUpdate {\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    itemType: ").append(toIndentedString(itemType)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    keywords: ").append(toIndentedString(keywords)).append("\n");
    sb.append("    sizeBytes: ").append(toIndentedString(sizeBytes)).append("\n");
    sb.append("    linkUrl: ").append(toIndentedString(linkUrl)).append("\n");
    sb.append("    uploadedAt: ").append(toIndentedString(uploadedAt)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    isShared: ").append(toIndentedString(isShared)).append("\n");
    sb.append("    likeCount: ").append(toIndentedString(likeCount)).append("\n");
    sb.append("    dislikeCount: ").append(toIndentedString(dislikeCount)).append("\n");
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

