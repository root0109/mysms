// Owned API contract of lms.
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
 * Fields of Bookmark that can be updated (partial update; see updateMask).
 */

public class BookmarkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String moduleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime savedAt;

  public BookmarkUpdate title(@Nullable String title) {
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

  public BookmarkUpdate url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * URL or in-app route.
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

  public BookmarkUpdate moduleCode(@Nullable String moduleCode) {
    this.moduleCode = moduleCode;
    return this;
  }

  /**
   * Module the link points into.
   * @return moduleCode
   */
  @Size(max = 60) 
  @JsonProperty("moduleCode")
  public @Nullable String getModuleCode() {
    return moduleCode;
  }

  @JsonProperty("moduleCode")
  public void setModuleCode(@Nullable String moduleCode) {
    this.moduleCode = moduleCode;
  }

  public BookmarkUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort context.
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

  public BookmarkUpdate savedAt(@Nullable OffsetDateTime savedAt) {
    this.savedAt = savedAt;
    return this;
  }

  /**
   * Saved.
   * @return savedAt
   */
  @Valid 
  @JsonProperty("savedAt")
  public @Nullable OffsetDateTime getSavedAt() {
    return savedAt;
  }

  @JsonProperty("savedAt")
  public void setSavedAt(@Nullable OffsetDateTime savedAt) {
    this.savedAt = savedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BookmarkUpdate bookmarkUpdate = (BookmarkUpdate) o;
    return Objects.equals(this.title, bookmarkUpdate.title) &&
        Objects.equals(this.url, bookmarkUpdate.url) &&
        Objects.equals(this.moduleCode, bookmarkUpdate.moduleCode) &&
        Objects.equals(this.cohortId, bookmarkUpdate.cohortId) &&
        Objects.equals(this.savedAt, bookmarkUpdate.savedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, url, moduleCode, cohortId, savedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BookmarkUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    moduleCode: ").append(toIndentedString(moduleCode)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    savedAt: ").append(toIndentedString(savedAt)).append("\n");
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

