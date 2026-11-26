// Owned API contract of library.
package com.mysms.campus.library.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Media that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class MediaUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> acquiredAcademicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 300) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> isbn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> internalCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> edition;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> mediaTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> mediaCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> coverImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public MediaUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public MediaUpdate acquiredAcademicYearId(@Nullable Optional<UUID> acquiredAcademicYearId) {
    this.acquiredAcademicYearId = acquiredAcademicYearId;
    return this;
  }

  /**
   * Academic year the title was catalogued / acquired.
   * @return acquiredAcademicYearId
   */
  
  @JsonProperty("acquiredAcademicYearId")
  public @Nullable Optional<UUID> getAcquiredAcademicYearId() {
    return acquiredAcademicYearId;
  }

  @JsonProperty("acquiredAcademicYearId")
  public void setAcquiredAcademicYearId(@Nullable Optional<UUID> acquiredAcademicYearId) {
    this.acquiredAcademicYearId = acquiredAcademicYearId;
  }

  public MediaUpdate title(@Nullable Optional<String> title) {
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

  public MediaUpdate isbn(@Nullable Optional<String> isbn) {
    this.isbn = isbn;
    return this;
  }

  /**
   * ISBN.
   * @return isbn
   */
  
  @JsonProperty("isbn")
  public @Nullable Optional<String> getIsbn() {
    return isbn;
  }

  @JsonProperty("isbn")
  public void setIsbn(@Nullable Optional<String> isbn) {
    this.isbn = isbn;
  }

  public MediaUpdate internalCode(@Nullable Optional<String> internalCode) {
    this.internalCode = internalCode;
    return this;
  }

  /**
   * Library accession / call code.
   * @return internalCode
   */
  
  @JsonProperty("internalCode")
  public @Nullable Optional<String> getInternalCode() {
    return internalCode;
  }

  @JsonProperty("internalCode")
  public void setInternalCode(@Nullable Optional<String> internalCode) {
    this.internalCode = internalCode;
  }

  public MediaUpdate edition(@Nullable Optional<String> edition) {
    this.edition = edition;
    return this;
  }

  /**
   * Edition.
   * @return edition
   */
  
  @JsonProperty("edition")
  public @Nullable Optional<String> getEdition() {
    return edition;
  }

  @JsonProperty("edition")
  public void setEdition(@Nullable Optional<String> edition) {
    this.edition = edition;
  }

  public MediaUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

  public MediaUpdate mediaTypeId(@Nullable Optional<UUID> mediaTypeId) {
    this.mediaTypeId = mediaTypeId;
    return this;
  }

  /**
   * Type.
   * @return mediaTypeId
   */
  
  @JsonProperty("mediaTypeId")
  public @Nullable Optional<UUID> getMediaTypeId() {
    return mediaTypeId;
  }

  @JsonProperty("mediaTypeId")
  public void setMediaTypeId(@Nullable Optional<UUID> mediaTypeId) {
    this.mediaTypeId = mediaTypeId;
  }

  public MediaUpdate mediaCategoryId(@Nullable Optional<UUID> mediaCategoryId) {
    this.mediaCategoryId = mediaCategoryId;
    return this;
  }

  /**
   * Category.
   * @return mediaCategoryId
   */
  
  @JsonProperty("mediaCategoryId")
  public @Nullable Optional<UUID> getMediaCategoryId() {
    return mediaCategoryId;
  }

  @JsonProperty("mediaCategoryId")
  public void setMediaCategoryId(@Nullable Optional<UUID> mediaCategoryId) {
    this.mediaCategoryId = mediaCategoryId;
  }

  public MediaUpdate coverImageFileId(@Nullable Optional<UUID> coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
    return this;
  }

  /**
   * Cover image.
   * @return coverImageFileId
   */
  
  @JsonProperty("coverImageFileId")
  public @Nullable Optional<UUID> getCoverImageFileId() {
    return coverImageFileId;
  }

  @JsonProperty("coverImageFileId")
  public void setCoverImageFileId(@Nullable Optional<UUID> coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
  }

  public MediaUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether in circulation.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MediaUpdate mediaUpdate = (MediaUpdate) o;
    return Objects.equals(this.institutionId, mediaUpdate.institutionId) &&
        Objects.equals(this.acquiredAcademicYearId, mediaUpdate.acquiredAcademicYearId) &&
        Objects.equals(this.title, mediaUpdate.title) &&
        Objects.equals(this.isbn, mediaUpdate.isbn) &&
        Objects.equals(this.internalCode, mediaUpdate.internalCode) &&
        Objects.equals(this.edition, mediaUpdate.edition) &&
        Objects.equals(this.description, mediaUpdate.description) &&
        Objects.equals(this.mediaTypeId, mediaUpdate.mediaTypeId) &&
        Objects.equals(this.mediaCategoryId, mediaUpdate.mediaCategoryId) &&
        Objects.equals(this.coverImageFileId, mediaUpdate.coverImageFileId) &&
        Objects.equals(this.isActive, mediaUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, acquiredAcademicYearId, title, isbn, internalCode, edition, description, mediaTypeId, mediaCategoryId, coverImageFileId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MediaUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    acquiredAcademicYearId: ").append(toIndentedString(acquiredAcademicYearId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    isbn: ").append(toIndentedString(isbn)).append("\n");
    sb.append("    internalCode: ").append(toIndentedString(internalCode)).append("\n");
    sb.append("    edition: ").append(toIndentedString(edition)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    mediaTypeId: ").append(toIndentedString(mediaTypeId)).append("\n");
    sb.append("    mediaCategoryId: ").append(toIndentedString(mediaCategoryId)).append("\n");
    sb.append("    coverImageFileId: ").append(toIndentedString(coverImageFileId)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

