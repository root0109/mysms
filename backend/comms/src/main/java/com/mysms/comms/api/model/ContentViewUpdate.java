// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of ContentView that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ContentViewUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> viewerUserId;

  /**
   * Content type.
   */
  public enum ContentTypeEnum {
    IMAGE("image"),
    
    VIDEO("video"),
    
    ASSIGNMENT("assignment"),
    
    HOMEWORK("homework");

    private final String value;

    ContentTypeEnum(String value) {
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
    public static ContentTypeEnum fromValue(String value) {
      for (ContentTypeEnum b : ContentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ContentTypeEnum> contentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> contentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> viewedAt;

  public ContentViewUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public ContentViewUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public ContentViewUpdate viewerUserId(@Nullable Optional<UUID> viewerUserId) {
    this.viewerUserId = viewerUserId;
    return this;
  }

  /**
   * Guardian who viewed.
   * @return viewerUserId
   */
  
  @JsonProperty("viewerUserId")
  public @Nullable Optional<UUID> getViewerUserId() {
    return viewerUserId;
  }

  @JsonProperty("viewerUserId")
  public void setViewerUserId(@Nullable Optional<UUID> viewerUserId) {
    this.viewerUserId = viewerUserId;
  }

  public ContentViewUpdate contentType(@Nullable Optional<ContentTypeEnum> contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * Content type.
   * @return contentType
   */
  
  @JsonProperty("contentType")
  public @Nullable Optional<ContentTypeEnum> getContentType() {
    return contentType;
  }

  @JsonProperty("contentType")
  public void setContentType(@Nullable Optional<ContentTypeEnum> contentType) {
    this.contentType = contentType;
  }

  public ContentViewUpdate contentId(@Nullable Optional<UUID> contentId) {
    this.contentId = contentId;
    return this;
  }

  /**
   * Polymorphic id of the item viewed (table given by content_type).
   * @return contentId
   */
  
  @JsonProperty("contentId")
  public @Nullable Optional<UUID> getContentId() {
    return contentId;
  }

  @JsonProperty("contentId")
  public void setContentId(@Nullable Optional<UUID> contentId) {
    this.contentId = contentId;
  }

  public ContentViewUpdate viewedAt(@Nullable Optional<OffsetDateTime> viewedAt) {
    this.viewedAt = viewedAt;
    return this;
  }

  /**
   * When viewed.
   * @return viewedAt
   */
  
  @JsonProperty("viewedAt")
  public @Nullable Optional<OffsetDateTime> getViewedAt() {
    return viewedAt;
  }

  @JsonProperty("viewedAt")
  public void setViewedAt(@Nullable Optional<OffsetDateTime> viewedAt) {
    this.viewedAt = viewedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentViewUpdate contentViewUpdate = (ContentViewUpdate) o;
    return Objects.equals(this.institutionId, contentViewUpdate.institutionId) &&
        Objects.equals(this.studentId, contentViewUpdate.studentId) &&
        Objects.equals(this.viewerUserId, contentViewUpdate.viewerUserId) &&
        Objects.equals(this.contentType, contentViewUpdate.contentType) &&
        Objects.equals(this.contentId, contentViewUpdate.contentId) &&
        Objects.equals(this.viewedAt, contentViewUpdate.viewedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, studentId, viewerUserId, contentType, contentId, viewedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentViewUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    viewerUserId: ").append(toIndentedString(viewerUserId)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    contentId: ").append(toIndentedString(contentId)).append("\n");
    sb.append("    viewedAt: ").append(toIndentedString(viewedAt)).append("\n");
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

