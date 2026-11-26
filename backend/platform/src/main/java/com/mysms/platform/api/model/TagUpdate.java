// Owned API contract of platform.
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
 * Fields of Tag that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class TagUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 50) String> tagType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 140) String> slug;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Object> translations;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 50) String> code;

  public TagUpdate tagType(@Nullable Optional<String> tagType) {
    this.tagType = tagType;
    return this;
  }

  /**
   * Tag family, e.g. general, post, library_media, lms_course.
   * @return tagType
   */
  
  @JsonProperty("tagType")
  public @Nullable Optional<String> getTagType() {
    return tagType;
  }

  @JsonProperty("tagType")
  public void setTagType(@Nullable Optional<String> tagType) {
    this.tagType = tagType;
  }

  public TagUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Tag label.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public TagUpdate slug(@Nullable Optional<String> slug) {
    this.slug = slug;
    return this;
  }

  /**
   * URL-safe tag key.
   * @return slug
   */
  
  @JsonProperty("slug")
  public @Nullable Optional<String> getSlug() {
    return slug;
  }

  @JsonProperty("slug")
  public void setSlug(@Nullable Optional<String> slug) {
    this.slug = slug;
  }

  public TagUpdate translations(@Nullable Optional<Object> translations) {
    this.translations = translations;
    return this;
  }

  /**
   * Localised name/slug values keyed by locale.
   * @return translations
   */
  
  @JsonProperty("translations")
  public @Nullable Optional<Object> getTranslations() {
    return translations;
  }

  @JsonProperty("translations")
  public void setTranslations(@Nullable Optional<Object> translations) {
    this.translations = translations;
  }

  public TagUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  public TagUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Short code (e.g. subject tag code).
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
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

