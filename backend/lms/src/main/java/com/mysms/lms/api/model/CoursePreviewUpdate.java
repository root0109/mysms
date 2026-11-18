// Owned API contract of lms.
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
 * Fields of CoursePreview that can be updated (partial update; see updateMask).
 */

public class CoursePreviewUpdate {

  /**
   * Kind of preview.
   */
  public enum PreviewKindEnum {
    MATERIAL("material"),
    
    VIDEO("video");

    private final String value;

    PreviewKindEnum(String value) {
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
    public static PreviewKindEnum fromValue(String value) {
      for (PreviewKindEnum b : PreviewKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable PreviewKindEnum previewKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String icon;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public CoursePreviewUpdate previewKind(@Nullable PreviewKindEnum previewKind) {
    this.previewKind = previewKind;
    return this;
  }

  /**
   * Kind of preview.
   * @return previewKind
   */
  
  @JsonProperty("previewKind")
  public @Nullable PreviewKindEnum getPreviewKind() {
    return previewKind;
  }

  @JsonProperty("previewKind")
  public void setPreviewKind(@Nullable PreviewKindEnum previewKind) {
    this.previewKind = previewKind;
  }

  public CoursePreviewUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Preview file.
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

  public CoursePreviewUpdate icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * Icon key.
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

  public CoursePreviewUpdate title(@Nullable String title) {
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

  public CoursePreviewUpdate description(@Nullable String description) {
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

  public CoursePreviewUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CoursePreviewUpdate coursePreviewUpdate = (CoursePreviewUpdate) o;
    return Objects.equals(this.previewKind, coursePreviewUpdate.previewKind) &&
        Objects.equals(this.fileId, coursePreviewUpdate.fileId) &&
        Objects.equals(this.icon, coursePreviewUpdate.icon) &&
        Objects.equals(this.title, coursePreviewUpdate.title) &&
        Objects.equals(this.description, coursePreviewUpdate.description) &&
        Objects.equals(this.sortOrder, coursePreviewUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(previewKind, fileId, icon, title, description, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CoursePreviewUpdate {\n");
    sb.append("    previewKind: ").append(toIndentedString(previewKind)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

