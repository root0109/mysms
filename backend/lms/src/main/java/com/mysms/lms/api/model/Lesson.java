// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Lesson inside a chapter.
 */

public class Lesson {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID supersededById;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean includeInPreview;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Object content = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherNotes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Object teacherContent = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String youtubeVideoId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String embeddedQuestion;

  /**
   * Type of attached file.
   */
  public enum FileTypeEnum {
    IMAGE("image"),
    
    DOCUMENT("document"),
    
    PDF("pdf");

    private final String value;

    FileTypeEnum(String value) {
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
    public static FileTypeEnum fromValue(String value) {
      for (FileTypeEnum b : FileTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable FileTypeEnum fileType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isScormPackage;

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

  public Lesson() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Lesson(UUID chapterId, String title, Integer sortOrder) {
    this.chapterId = chapterId;
    this.title = title;
    this.sortOrder = sortOrder;
  }

  public Lesson version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Version number; published versions are immutable, edits create a new version.
   * @return version
   */
  
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public Lesson supersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
    return this;
  }

  /**
   * Newer version that replaced this one.
   * @return supersededById
   */
  @Valid 
  @JsonProperty("supersededById")
  public @Nullable UUID getSupersededById() {
    return supersededById;
  }

  @JsonProperty("supersededById")
  public void setSupersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
  }

  public Lesson publishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
    return this;
  }

  /**
   * When this version was published (absent = draft).
   * @return publishedAt
   */
  @Valid 
  @JsonProperty("publishedAt")
  public @Nullable OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  @JsonProperty("publishedAt")
  public void setPublishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  public Lesson id(@Nullable UUID id) {
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

  public Lesson chapterId(UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter.
   * @return chapterId
   */
  @NotNull @Valid 
  @JsonProperty("chapterId")
  public UUID getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(UUID chapterId) {
    this.chapterId = chapterId;
  }

  public Lesson title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Lesson title.
   * @return title
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public Lesson sortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position in the chapter.
   * @return sortOrder
   */
  @NotNull 
  @JsonProperty("sortOrder")
  public Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Lesson includeInPreview(@Nullable Boolean includeInPreview) {
    this.includeInPreview = includeInPreview;
    return this;
  }

  /**
   * Visible to non-enrolled visitors.
   * @return includeInPreview
   */
  
  @JsonProperty("includeInPreview")
  public @Nullable Boolean getIncludeInPreview() {
    return includeInPreview;
  }

  @JsonProperty("includeInPreview")
  public void setIncludeInPreview(@Nullable Boolean includeInPreview) {
    this.includeInPreview = includeInPreview;
  }

  public Lesson body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Markdown body.
   * @return body
   */
  
  @JsonProperty("body")
  public @Nullable String getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(@Nullable String body) {
    this.body = body;
  }

  public Lesson content(@Nullable Object content) {
    this.content = content;
    return this;
  }

  /**
   * Structured (block editor) content.
   * @return content
   */
  
  @JsonProperty("content")
  public @Nullable Object getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(@Nullable Object content) {
    this.content = content;
  }

  public Lesson teacherNotes(@Nullable String teacherNotes) {
    this.teacherNotes = teacherNotes;
    return this;
  }

  /**
   * Notes visible to teachers only (markdown).
   * @return teacherNotes
   */
  
  @JsonProperty("teacherNotes")
  public @Nullable String getTeacherNotes() {
    return teacherNotes;
  }

  @JsonProperty("teacherNotes")
  public void setTeacherNotes(@Nullable String teacherNotes) {
    this.teacherNotes = teacherNotes;
  }

  public Lesson teacherContent(@Nullable Object teacherContent) {
    this.teacherContent = teacherContent;
    return this;
  }

  /**
   * Structured teacher-only content.
   * @return teacherContent
   */
  
  @JsonProperty("teacherContent")
  public @Nullable Object getTeacherContent() {
    return teacherContent;
  }

  @JsonProperty("teacherContent")
  public void setTeacherContent(@Nullable Object teacherContent) {
    this.teacherContent = teacherContent;
  }

  public Lesson youtubeVideoId(@Nullable String youtubeVideoId) {
    this.youtubeVideoId = youtubeVideoId;
    return this;
  }

  /**
   * External YouTube video id.
   * @return youtubeVideoId
   */
  @Size(max = 60) 
  @JsonProperty("youtubeVideoId")
  public @Nullable String getYoutubeVideoId() {
    return youtubeVideoId;
  }

  @JsonProperty("youtubeVideoId")
  public void setYoutubeVideoId(@Nullable String youtubeVideoId) {
    this.youtubeVideoId = youtubeVideoId;
  }

  public Lesson embeddedQuestion(@Nullable String embeddedQuestion) {
    this.embeddedQuestion = embeddedQuestion;
    return this;
  }

  /**
   * Reflection question shown in the lesson.
   * @return embeddedQuestion
   */
  
  @JsonProperty("embeddedQuestion")
  public @Nullable String getEmbeddedQuestion() {
    return embeddedQuestion;
  }

  @JsonProperty("embeddedQuestion")
  public void setEmbeddedQuestion(@Nullable String embeddedQuestion) {
    this.embeddedQuestion = embeddedQuestion;
  }

  public Lesson fileType(@Nullable FileTypeEnum fileType) {
    this.fileType = fileType;
    return this;
  }

  /**
   * Type of attached file.
   * @return fileType
   */
  
  @JsonProperty("fileType")
  public @Nullable FileTypeEnum getFileType() {
    return fileType;
  }

  @JsonProperty("fileType")
  public void setFileType(@Nullable FileTypeEnum fileType) {
    this.fileType = fileType;
  }

  public Lesson isScormPackage(@Nullable Boolean isScormPackage) {
    this.isScormPackage = isScormPackage;
    return this;
  }

  /**
   * Lesson is part of a SCORM chapter.
   * @return isScormPackage
   */
  
  @JsonProperty("isScormPackage")
  public @Nullable Boolean getIsScormPackage() {
    return isScormPackage;
  }

  @JsonProperty("isScormPackage")
  public void setIsScormPackage(@Nullable Boolean isScormPackage) {
    this.isScormPackage = isScormPackage;
  }

  public Lesson createTime(@Nullable OffsetDateTime createTime) {
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

  public Lesson createdBy(@Nullable UUID createdBy) {
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

  public Lesson updateTime(@Nullable OffsetDateTime updateTime) {
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

  public Lesson updatedBy(@Nullable UUID updatedBy) {
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

  public Lesson etag(@Nullable String etag) {
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
    Lesson lesson = (Lesson) o;
    return Objects.equals(this.version, lesson.version) &&
        Objects.equals(this.supersededById, lesson.supersededById) &&
        Objects.equals(this.publishedAt, lesson.publishedAt) &&
        Objects.equals(this.id, lesson.id) &&
        Objects.equals(this.chapterId, lesson.chapterId) &&
        Objects.equals(this.title, lesson.title) &&
        Objects.equals(this.sortOrder, lesson.sortOrder) &&
        Objects.equals(this.includeInPreview, lesson.includeInPreview) &&
        Objects.equals(this.body, lesson.body) &&
        Objects.equals(this.content, lesson.content) &&
        Objects.equals(this.teacherNotes, lesson.teacherNotes) &&
        Objects.equals(this.teacherContent, lesson.teacherContent) &&
        Objects.equals(this.youtubeVideoId, lesson.youtubeVideoId) &&
        Objects.equals(this.embeddedQuestion, lesson.embeddedQuestion) &&
        Objects.equals(this.fileType, lesson.fileType) &&
        Objects.equals(this.isScormPackage, lesson.isScormPackage) &&
        Objects.equals(this.createTime, lesson.createTime) &&
        Objects.equals(this.createdBy, lesson.createdBy) &&
        Objects.equals(this.updateTime, lesson.updateTime) &&
        Objects.equals(this.updatedBy, lesson.updatedBy) &&
        Objects.equals(this.etag, lesson.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, supersededById, publishedAt, id, chapterId, title, sortOrder, includeInPreview, body, content, teacherNotes, teacherContent, youtubeVideoId, embeddedQuestion, fileType, isScormPackage, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Lesson {\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    supersededById: ").append(toIndentedString(supersededById)).append("\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    includeInPreview: ").append(toIndentedString(includeInPreview)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    teacherNotes: ").append(toIndentedString(teacherNotes)).append("\n");
    sb.append("    teacherContent: ").append(toIndentedString(teacherContent)).append("\n");
    sb.append("    youtubeVideoId: ").append(toIndentedString(youtubeVideoId)).append("\n");
    sb.append("    embeddedQuestion: ").append(toIndentedString(embeddedQuestion)).append("\n");
    sb.append("    fileType: ").append(toIndentedString(fileType)).append("\n");
    sb.append("    isScormPackage: ").append(toIndentedString(isScormPackage)).append("\n");
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

