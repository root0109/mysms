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
 * Fields of Bookmark that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class BookmarkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 1000) String> url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> moduleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> savedAt;

  public BookmarkUpdate title(@Nullable Optional<String> title) {
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

  public BookmarkUpdate url(@Nullable Optional<String> url) {
    this.url = url;
    return this;
  }

  /**
   * URL or in-app route.
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

  public BookmarkUpdate moduleCode(@Nullable Optional<String> moduleCode) {
    this.moduleCode = moduleCode;
    return this;
  }

  /**
   * Module the link points into.
   * @return moduleCode
   */
  
  @JsonProperty("moduleCode")
  public @Nullable Optional<String> getModuleCode() {
    return moduleCode;
  }

  @JsonProperty("moduleCode")
  public void setModuleCode(@Nullable Optional<String> moduleCode) {
    this.moduleCode = moduleCode;
  }

  public BookmarkUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort context.
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

  public BookmarkUpdate savedAt(@Nullable Optional<OffsetDateTime> savedAt) {
    this.savedAt = savedAt;
    return this;
  }

  /**
   * Saved.
   * @return savedAt
   */
  
  @JsonProperty("savedAt")
  public @Nullable Optional<OffsetDateTime> getSavedAt() {
    return savedAt;
  }

  @JsonProperty("savedAt")
  public void setSavedAt(@Nullable Optional<OffsetDateTime> savedAt) {
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

