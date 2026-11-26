// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of Annotation that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class AnnotationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> ebookId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> fileId;

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
  private @Nullable Optional<@NotNull AnnotationTypeEnum> annotationType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> annotationSubtype;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> pageNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> startCharPos;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> charCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> annotatedText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> embedCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 1000) String> url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> mediaFileId;

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
  private @Nullable Optional<@NotNull VisibilityEnum> visibility;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> forumTopicId;

  public AnnotationUpdate contentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item annotated.
   * @return contentItemId
   */
  
  @JsonProperty("contentItemId")
  public @Nullable Optional<UUID> getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
  }

  public AnnotationUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public AnnotationUpdate ebookId(@Nullable Optional<UUID> ebookId) {
    this.ebookId = ebookId;
    return this;
  }

  /**
   * E-book.
   * @return ebookId
   */
  
  @JsonProperty("ebookId")
  public @Nullable Optional<UUID> getEbookId() {
    return ebookId;
  }

  @JsonProperty("ebookId")
  public void setEbookId(@Nullable Optional<UUID> ebookId) {
    this.ebookId = ebookId;
  }

  public AnnotationUpdate fileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Document annotated.
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

  public AnnotationUpdate annotationType(@Nullable Optional<AnnotationTypeEnum> annotationType) {
    this.annotationType = annotationType;
    return this;
  }

  /**
   * Annotation type.
   * @return annotationType
   */
  
  @JsonProperty("annotationType")
  public @Nullable Optional<AnnotationTypeEnum> getAnnotationType() {
    return annotationType;
  }

  @JsonProperty("annotationType")
  public void setAnnotationType(@Nullable Optional<AnnotationTypeEnum> annotationType) {
    this.annotationType = annotationType;
  }

  public AnnotationUpdate annotationSubtype(@Nullable Optional<String> annotationSubtype) {
    this.annotationSubtype = annotationSubtype;
    return this;
  }

  /**
   * Sub-type (e.g. colour or media kind).
   * @return annotationSubtype
   */
  
  @JsonProperty("annotationSubtype")
  public @Nullable Optional<String> getAnnotationSubtype() {
    return annotationSubtype;
  }

  @JsonProperty("annotationSubtype")
  public void setAnnotationSubtype(@Nullable Optional<String> annotationSubtype) {
    this.annotationSubtype = annotationSubtype;
  }

  public AnnotationUpdate pageNumber(@Nullable Optional<String> pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

  /**
   * Page.
   * @return pageNumber
   */
  
  @JsonProperty("pageNumber")
  public @Nullable Optional<String> getPageNumber() {
    return pageNumber;
  }

  @JsonProperty("pageNumber")
  public void setPageNumber(@Nullable Optional<String> pageNumber) {
    this.pageNumber = pageNumber;
  }

  public AnnotationUpdate startCharPos(@Nullable Optional<String> startCharPos) {
    this.startCharPos = startCharPos;
    return this;
  }

  /**
   * Start position of the selection.
   * @return startCharPos
   */
  
  @JsonProperty("startCharPos")
  public @Nullable Optional<String> getStartCharPos() {
    return startCharPos;
  }

  @JsonProperty("startCharPos")
  public void setStartCharPos(@Nullable Optional<String> startCharPos) {
    this.startCharPos = startCharPos;
  }

  public AnnotationUpdate charCount(@Nullable Optional<String> charCount) {
    this.charCount = charCount;
    return this;
  }

  /**
   * Length of the selection.
   * @return charCount
   */
  
  @JsonProperty("charCount")
  public @Nullable Optional<String> getCharCount() {
    return charCount;
  }

  @JsonProperty("charCount")
  public void setCharCount(@Nullable Optional<String> charCount) {
    this.charCount = charCount;
  }

  public AnnotationUpdate location(@Nullable Optional<String> location) {
    this.location = location;
    return this;
  }

  /**
   * Location descriptor inside the document (range / CFI).
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable Optional<String> getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable Optional<String> location) {
    this.location = location;
  }

  public AnnotationUpdate annotatedText(@Nullable Optional<String> annotatedText) {
    this.annotatedText = annotatedText;
    return this;
  }

  /**
   * Selected text.
   * @return annotatedText
   */
  
  @JsonProperty("annotatedText")
  public @Nullable Optional<String> getAnnotatedText() {
    return annotatedText;
  }

  @JsonProperty("annotatedText")
  public void setAnnotatedText(@Nullable Optional<String> annotatedText) {
    this.annotatedText = annotatedText;
  }

  public AnnotationUpdate title(@Nullable Optional<String> title) {
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

  public AnnotationUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Note text.
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

  public AnnotationUpdate embedCode(@Nullable Optional<String> embedCode) {
    this.embedCode = embedCode;
    return this;
  }

  /**
   * Embedded media code.
   * @return embedCode
   */
  
  @JsonProperty("embedCode")
  public @Nullable Optional<String> getEmbedCode() {
    return embedCode;
  }

  @JsonProperty("embedCode")
  public void setEmbedCode(@Nullable Optional<String> embedCode) {
    this.embedCode = embedCode;
  }

  public AnnotationUpdate url(@Nullable Optional<String> url) {
    this.url = url;
    return this;
  }

  /**
   * Linked URL.
   * @return url
   */
  
  @JsonProperty("url")
  public @Nullable Optional<String> getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable Optional<String> url) {
    this.url = url;
  }

  public AnnotationUpdate mediaFileId(@Nullable Optional<UUID> mediaFileId) {
    this.mediaFileId = mediaFileId;
    return this;
  }

  /**
   * Attached media.
   * @return mediaFileId
   */
  
  @JsonProperty("mediaFileId")
  public @Nullable Optional<UUID> getMediaFileId() {
    return mediaFileId;
  }

  @JsonProperty("mediaFileId")
  public void setMediaFileId(@Nullable Optional<UUID> mediaFileId) {
    this.mediaFileId = mediaFileId;
  }

  public AnnotationUpdate visibility(@Nullable Optional<VisibilityEnum> visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Who can see it.
   * @return visibility
   */
  
  @JsonProperty("visibility")
  public @Nullable Optional<VisibilityEnum> getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable Optional<VisibilityEnum> visibility) {
    this.visibility = visibility;
  }

  public AnnotationUpdate forumTopicId(@Nullable Optional<UUID> forumTopicId) {
    this.forumTopicId = forumTopicId;
    return this;
  }

  /**
   * Discussion started from the annotation.
   * @return forumTopicId
   */
  
  @JsonProperty("forumTopicId")
  public @Nullable Optional<UUID> getForumTopicId() {
    return forumTopicId;
  }

  @JsonProperty("forumTopicId")
  public void setForumTopicId(@Nullable Optional<UUID> forumTopicId) {
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
    return Objects.equals(this.contentItemId, annotationUpdate.contentItemId) &&
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
    return Objects.hash(contentItemId, cohortId, ebookId, fileId, annotationType, annotationSubtype, pageNumber, startCharPos, charCount, location, annotatedText, title, description, embedCode, url, mediaFileId, visibility, forumTopicId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnnotationUpdate {\n");
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

