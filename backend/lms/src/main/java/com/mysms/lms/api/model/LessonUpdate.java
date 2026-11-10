// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Lesson that can be updated (partial update; see updateMask).
 */

public class LessonUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean includeInPreview;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  private @Nullable Object content = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherNotes;

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

  public LessonUpdate chapterId(@Nullable UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter.
   * @return chapterId
   */
  @Valid 
  @JsonProperty("chapterId")
  public @Nullable UUID getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(@Nullable UUID chapterId) {
    this.chapterId = chapterId;
  }

  public LessonUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Lesson title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public LessonUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position in the chapter.
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

  public LessonUpdate includeInPreview(@Nullable Boolean includeInPreview) {
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

  public LessonUpdate body(@Nullable String body) {
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

  public LessonUpdate content(@Nullable Object content) {
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

  public LessonUpdate teacherNotes(@Nullable String teacherNotes) {
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

  public LessonUpdate teacherContent(@Nullable Object teacherContent) {
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

  public LessonUpdate youtubeVideoId(@Nullable String youtubeVideoId) {
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

  public LessonUpdate embeddedQuestion(@Nullable String embeddedQuestion) {
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

  public LessonUpdate fileType(@Nullable FileTypeEnum fileType) {
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

  public LessonUpdate isScormPackage(@Nullable Boolean isScormPackage) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonUpdate lessonUpdate = (LessonUpdate) o;
    return Objects.equals(this.chapterId, lessonUpdate.chapterId) &&
        Objects.equals(this.title, lessonUpdate.title) &&
        Objects.equals(this.sortOrder, lessonUpdate.sortOrder) &&
        Objects.equals(this.includeInPreview, lessonUpdate.includeInPreview) &&
        Objects.equals(this.body, lessonUpdate.body) &&
        Objects.equals(this.content, lessonUpdate.content) &&
        Objects.equals(this.teacherNotes, lessonUpdate.teacherNotes) &&
        Objects.equals(this.teacherContent, lessonUpdate.teacherContent) &&
        Objects.equals(this.youtubeVideoId, lessonUpdate.youtubeVideoId) &&
        Objects.equals(this.embeddedQuestion, lessonUpdate.embeddedQuestion) &&
        Objects.equals(this.fileType, lessonUpdate.fileType) &&
        Objects.equals(this.isScormPackage, lessonUpdate.isScormPackage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chapterId, title, sortOrder, includeInPreview, body, content, teacherNotes, teacherContent, youtubeVideoId, embeddedQuestion, fileType, isScormPackage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonUpdate {\n");
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

