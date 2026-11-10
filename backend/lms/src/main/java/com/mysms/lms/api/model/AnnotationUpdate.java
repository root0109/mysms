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
 * Fields of Annotation that can be updated (partial update; see updateMask).
 */

public class AnnotationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ebookId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  /**
   * Annotation type.
   */
  public enum AnnotationTypeEnum {
    HIGHLIGHT("highlight"),
    
    NOTE("note"),
    
    LINK("link"),
    
    MEDIA("media"),
    
    EMBED("embed"),
    
    BOOKMARK("bookmark"),
    
    QUESTION("question");

    private final String value;

    AnnotationTypeEnum(String value) {
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
    public static AnnotationTypeEnum fromValue(String value) {
      for (AnnotationTypeEnum b : AnnotationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AnnotationTypeEnum annotationType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String annotationSubtype;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String pageNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startCharPos;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String charCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String annotatedText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String embedCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mediaFileId;

  /**
   * Who can see it.
   */
  public enum VisibilityEnum {
    PRIVATE("private"),
    
    SHARED_WITH_USERS("shared_with_users"),
    
    COHORT("cohort");

    private final String value;

    VisibilityEnum(String value) {
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
    public static VisibilityEnum fromValue(String value) {
      for (VisibilityEnum b : VisibilityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable VisibilityEnum visibility;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID forumTopicId;

  public AnnotationUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Author.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public AnnotationUpdate contentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item annotated.
   * @return contentItemId
   */
  @Valid 
  @JsonProperty("contentItemId")
  public @Nullable UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public AnnotationUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public AnnotationUpdate ebookId(@Nullable UUID ebookId) {
    this.ebookId = ebookId;
    return this;
  }

  /**
   * E-book.
   * @return ebookId
   */
  @Valid 
  @JsonProperty("ebookId")
  public @Nullable UUID getEbookId() {
    return ebookId;
  }

  @JsonProperty("ebookId")
  public void setEbookId(@Nullable UUID ebookId) {
    this.ebookId = ebookId;
  }

  public AnnotationUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Document annotated.
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

  public AnnotationUpdate annotationType(@Nullable AnnotationTypeEnum annotationType) {
    this.annotationType = annotationType;
    return this;
  }

  /**
   * Annotation type.
   * @return annotationType
   */
  
  @JsonProperty("annotationType")
  public @Nullable AnnotationTypeEnum getAnnotationType() {
    return annotationType;
  }

  @JsonProperty("annotationType")
  public void setAnnotationType(@Nullable AnnotationTypeEnum annotationType) {
    this.annotationType = annotationType;
  }

  public AnnotationUpdate annotationSubtype(@Nullable String annotationSubtype) {
    this.annotationSubtype = annotationSubtype;
    return this;
  }

  /**
   * Sub-type (e.g. colour or media kind).
   * @return annotationSubtype
   */
  @Size(max = 40) 
  @JsonProperty("annotationSubtype")
  public @Nullable String getAnnotationSubtype() {
    return annotationSubtype;
  }

  @JsonProperty("annotationSubtype")
  public void setAnnotationSubtype(@Nullable String annotationSubtype) {
    this.annotationSubtype = annotationSubtype;
  }

  public AnnotationUpdate pageNumber(@Nullable String pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

  /**
   * Page.
   * @return pageNumber
   */
  @Size(max = 20) 
  @JsonProperty("pageNumber")
  public @Nullable String getPageNumber() {
    return pageNumber;
  }

  @JsonProperty("pageNumber")
  public void setPageNumber(@Nullable String pageNumber) {
    this.pageNumber = pageNumber;
  }

  public AnnotationUpdate startCharPos(@Nullable String startCharPos) {
    this.startCharPos = startCharPos;
    return this;
  }

  /**
   * Start position of the selection.
   * @return startCharPos
   */
  @Size(max = 40) 
  @JsonProperty("startCharPos")
  public @Nullable String getStartCharPos() {
    return startCharPos;
  }

  @JsonProperty("startCharPos")
  public void setStartCharPos(@Nullable String startCharPos) {
    this.startCharPos = startCharPos;
  }

  public AnnotationUpdate charCount(@Nullable String charCount) {
    this.charCount = charCount;
    return this;
  }

  /**
   * Length of the selection.
   * @return charCount
   */
  @Size(max = 40) 
  @JsonProperty("charCount")
  public @Nullable String getCharCount() {
    return charCount;
  }

  @JsonProperty("charCount")
  public void setCharCount(@Nullable String charCount) {
    this.charCount = charCount;
  }

  public AnnotationUpdate location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Location descriptor inside the document (range / CFI).
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public AnnotationUpdate annotatedText(@Nullable String annotatedText) {
    this.annotatedText = annotatedText;
    return this;
  }

  /**
   * Selected text.
   * @return annotatedText
   */
  
  @JsonProperty("annotatedText")
  public @Nullable String getAnnotatedText() {
    return annotatedText;
  }

  @JsonProperty("annotatedText")
  public void setAnnotatedText(@Nullable String annotatedText) {
    this.annotatedText = annotatedText;
  }

  public AnnotationUpdate title(@Nullable String title) {
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

  public AnnotationUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Note text.
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

  public AnnotationUpdate embedCode(@Nullable String embedCode) {
    this.embedCode = embedCode;
    return this;
  }

  /**
   * Embedded media code.
   * @return embedCode
   */
  
  @JsonProperty("embedCode")
  public @Nullable String getEmbedCode() {
    return embedCode;
  }

  @JsonProperty("embedCode")
  public void setEmbedCode(@Nullable String embedCode) {
    this.embedCode = embedCode;
  }

  public AnnotationUpdate url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * Linked URL.
   * @return url
   */
  @Size(max = 1000) 
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public AnnotationUpdate mediaFileId(@Nullable UUID mediaFileId) {
    this.mediaFileId = mediaFileId;
    return this;
  }

  /**
   * Attached media.
   * @return mediaFileId
   */
  @Valid 
  @JsonProperty("mediaFileId")
  public @Nullable UUID getMediaFileId() {
    return mediaFileId;
  }

  @JsonProperty("mediaFileId")
  public void setMediaFileId(@Nullable UUID mediaFileId) {
    this.mediaFileId = mediaFileId;
  }

  public AnnotationUpdate visibility(@Nullable VisibilityEnum visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Who can see it.
   * @return visibility
   */
  
  @JsonProperty("visibility")
  public @Nullable VisibilityEnum getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable VisibilityEnum visibility) {
    this.visibility = visibility;
  }

  public AnnotationUpdate forumTopicId(@Nullable UUID forumTopicId) {
    this.forumTopicId = forumTopicId;
    return this;
  }

  /**
   * Discussion started from the annotation.
   * @return forumTopicId
   */
  @Valid 
  @JsonProperty("forumTopicId")
  public @Nullable UUID getForumTopicId() {
    return forumTopicId;
  }

  @JsonProperty("forumTopicId")
  public void setForumTopicId(@Nullable UUID forumTopicId) {
    this.forumTopicId = forumTopicId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnnotationUpdate annotationUpdate = (AnnotationUpdate) o;
    return Objects.equals(this.userId, annotationUpdate.userId) &&
        Objects.equals(this.contentItemId, annotationUpdate.contentItemId) &&
        Objects.equals(this.cohortId, annotationUpdate.cohortId) &&
        Objects.equals(this.ebookId, annotationUpdate.ebookId) &&
        Objects.equals(this.fileId, annotationUpdate.fileId) &&
        Objects.equals(this.annotationType, annotationUpdate.annotationType) &&
        Objects.equals(this.annotationSubtype, annotationUpdate.annotationSubtype) &&
        Objects.equals(this.pageNumber, annotationUpdate.pageNumber) &&
        Objects.equals(this.startCharPos, annotationUpdate.startCharPos) &&
        Objects.equals(this.charCount, annotationUpdate.charCount) &&
        Objects.equals(this.location, annotationUpdate.location) &&
        Objects.equals(this.annotatedText, annotationUpdate.annotatedText) &&
        Objects.equals(this.title, annotationUpdate.title) &&
        Objects.equals(this.description, annotationUpdate.description) &&
        Objects.equals(this.embedCode, annotationUpdate.embedCode) &&
        Objects.equals(this.url, annotationUpdate.url) &&
        Objects.equals(this.mediaFileId, annotationUpdate.mediaFileId) &&
        Objects.equals(this.visibility, annotationUpdate.visibility) &&
        Objects.equals(this.forumTopicId, annotationUpdate.forumTopicId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, contentItemId, cohortId, ebookId, fileId, annotationType, annotationSubtype, pageNumber, startCharPos, charCount, location, annotatedText, title, description, embedCode, url, mediaFileId, visibility, forumTopicId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnnotationUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    ebookId: ").append(toIndentedString(ebookId)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    annotationType: ").append(toIndentedString(annotationType)).append("\n");
    sb.append("    annotationSubtype: ").append(toIndentedString(annotationSubtype)).append("\n");
    sb.append("    pageNumber: ").append(toIndentedString(pageNumber)).append("\n");
    sb.append("    startCharPos: ").append(toIndentedString(startCharPos)).append("\n");
    sb.append("    charCount: ").append(toIndentedString(charCount)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    annotatedText: ").append(toIndentedString(annotatedText)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    embedCode: ").append(toIndentedString(embedCode)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    mediaFileId: ").append(toIndentedString(mediaFileId)).append("\n");
    sb.append("    visibility: ").append(toIndentedString(visibility)).append("\n");
    sb.append("    forumTopicId: ").append(toIndentedString(forumTopicId)).append("\n");
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

