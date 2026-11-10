// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Tag that can be updated (partial update; see updateMask).
 */

public class TagUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String tagType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String slug;

  private @Nullable Object translations = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  public TagUpdate tagType(@Nullable String tagType) {
    this.tagType = tagType;
    return this;
  }

  /**
   * Tag family, e.g. general, post, library_media, lms_course.
   * @return tagType
   */
  @Size(max = 50) 
  @JsonProperty("tagType")
  public @Nullable String getTagType() {
    return tagType;
  }

  @JsonProperty("tagType")
  public void setTagType(@Nullable String tagType) {
    this.tagType = tagType;
  }

  public TagUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Tag label.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public TagUpdate slug(@Nullable String slug) {
    this.slug = slug;
    return this;
  }

  /**
   * URL-safe tag key.
   * @return slug
   */
  @Size(max = 140) 
  @JsonProperty("slug")
  public @Nullable String getSlug() {
    return slug;
  }

  @JsonProperty("slug")
  public void setSlug(@Nullable String slug) {
    this.slug = slug;
  }

  public TagUpdate translations(@Nullable Object translations) {
    this.translations = translations;
    return this;
  }

  /**
   * Localised name/slug values keyed by locale.
   * @return translations
   */
  
  @JsonProperty("translations")
  public @Nullable Object getTranslations() {
    return translations;
  }

  @JsonProperty("translations")
  public void setTranslations(@Nullable Object translations) {
    this.translations = translations;
  }

  public TagUpdate sortOrder(@Nullable Integer sortOrder) {
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

  public TagUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Short code (e.g. subject tag code).
   * @return code
   */
  @Size(max = 50) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TagUpdate tagUpdate = (TagUpdate) o;
    return Objects.equals(this.tagType, tagUpdate.tagType) &&
        Objects.equals(this.name, tagUpdate.name) &&
        Objects.equals(this.slug, tagUpdate.slug) &&
        Objects.equals(this.translations, tagUpdate.translations) &&
        Objects.equals(this.sortOrder, tagUpdate.sortOrder) &&
        Objects.equals(this.code, tagUpdate.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tagType, name, slug, translations, sortOrder, code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TagUpdate {\n");
    sb.append("    tagType: ").append(toIndentedString(tagType)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    slug: ").append(toIndentedString(slug)).append("\n");
    sb.append("    translations: ").append(toIndentedString(translations)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
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

