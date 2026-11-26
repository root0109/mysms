// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of DriveItem that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class DriveItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> itemType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> icon;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> keywords;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?[0-9]+$") String> sizeBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 1000) String> linkUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> uploadedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isShared;

  public DriveItemUpdate fileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Stored file (NULL for a link).
   * @return fileId
   */
  
  @JsonProperty("fileId")
  public @Nullable Optional<UUID> getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
  }

  public DriveItemUpdate itemType(@Nullable Optional<String> itemType) {
    this.itemType = itemType;
    return this;
  }

  /**
   * File type label (document, presentation, video, link...).
   * @return itemType
   */
  
  @JsonProperty("itemType")
  public @Nullable Optional<String> getItemType() {
    return itemType;
  }

  @JsonProperty("itemType")
  public void setItemType(@Nullable Optional<String> itemType) {
    this.itemType = itemType;
  }

  public DriveItemUpdate icon(@Nullable Optional<String> icon) {
    this.icon = icon;
    return this;
  }

  /**
   * Icon key shown in the drive.
   * @return icon
   */
  
  @JsonProperty("icon")
  public @Nullable Optional<String> getIcon() {
    return icon;
  }

  @JsonProperty("icon")
  public void setIcon(@Nullable Optional<String> icon) {
    this.icon = icon;
  }

  public DriveItemUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public DriveItemUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public DriveItemUpdate keywords(@Nullable Optional<String> keywords) {
    this.keywords = keywords;
    return this;
  }

  /**
   * Search keywords.
   * @return keywords
   */
  
  @JsonProperty("keywords")
  public @Nullable Optional<String> getKeywords() {
    return keywords;
  }

  @JsonProperty("keywords")
  public void setKeywords(@Nullable Optional<String> keywords) {
    this.keywords = keywords;
  }

  public DriveItemUpdate sizeBytes(@Nullable Optional<String> sizeBytes) {
    this.sizeBytes = sizeBytes;
    return this;
  }

  /**
   * Size in bytes.
   * @return sizeBytes
   */
  
  @JsonProperty("sizeBytes")
  public @Nullable Optional<String> getSizeBytes() {
    return sizeBytes;
  }

  @JsonProperty("sizeBytes")
  public void setSizeBytes(@Nullable Optional<String> sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public DriveItemUpdate linkUrl(@Nullable Optional<String> linkUrl) {
    this.linkUrl = linkUrl;
    return this;
  }

  /**
   * URL when the item is a link.
   * @return linkUrl
   */
  
  @JsonProperty("linkUrl")
  public @Nullable Optional<String> getLinkUrl() {
    return linkUrl;
  }

  @JsonProperty("linkUrl")
  public void setLinkUrl(@Nullable Optional<String> linkUrl) {
    this.linkUrl = linkUrl;
  }

  public DriveItemUpdate uploadedAt(@Nullable Optional<OffsetDateTime> uploadedAt) {
    this.uploadedAt = uploadedAt;
    return this;
  }

  /**
   * Upload time.
   * @return uploadedAt
   */
  
  @JsonProperty("uploadedAt")
  public @Nullable Optional<OffsetDateTime> getUploadedAt() {
    return uploadedAt;
  }

  @JsonProperty("uploadedAt")
  public void setUploadedAt(@Nullable Optional<OffsetDateTime> uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public DriveItemUpdate courseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course the item relates to.
   * @return courseId
   */
  
  @JsonProperty("courseId")
  public @Nullable Optional<UUID> getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable Optional<UUID> courseId) {
    this.courseId = courseId;
  }

  public DriveItemUpdate isShared(@Nullable Optional<Boolean> isShared) {
    this.isShared = isShared;
    return this;
  }

  /**
   * Shared with at least one user (see platform.drive_item_share).
   * @return isShared
   */
  
  @JsonProperty("isShared")
  public @Nullable Optional<Boolean> getIsShared() {
    return isShared;
  }

  @JsonProperty("isShared")
  public void setIsShared(@Nullable Optional<Boolean> isShared) {
    this.isShared = isShared;
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
    return Objects.equals(this.fileId, driveItemUpdate.fileId) &&
        Objects.equals(this.itemType, driveItemUpdate.itemType) &&
        Objects.equals(this.icon, driveItemUpdate.icon) &&
        Objects.equals(this.title, driveItemUpdate.title) &&
        Objects.equals(this.description, driveItemUpdate.description) &&
        Objects.equals(this.keywords, driveItemUpdate.keywords) &&
        Objects.equals(this.sizeBytes, driveItemUpdate.sizeBytes) &&
        Objects.equals(this.linkUrl, driveItemUpdate.linkUrl) &&
        Objects.equals(this.uploadedAt, driveItemUpdate.uploadedAt) &&
        Objects.equals(this.courseId, driveItemUpdate.courseId) &&
        Objects.equals(this.isShared, driveItemUpdate.isShared);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, itemType, icon, title, description, keywords, sizeBytes, linkUrl, uploadedAt, courseId, isShared);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DriveItemUpdate {\n");
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

