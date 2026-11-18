// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of Magazine that can be updated (partial update; see updateMask).
 */

public class MagazineUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer publicationYear;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID coverImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID documentFileId;

  public MagazineUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public MagazineUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public MagazineUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public MagazineUpdate publicationYear(@Nullable Integer publicationYear) {
    this.publicationYear = publicationYear;
    return this;
  }

  /**
   * Year.
   * @return publicationYear
   */
  
  @JsonProperty("publicationYear")
  public @Nullable Integer getPublicationYear() {
    return publicationYear;
  }

  @JsonProperty("publicationYear")
  public void setPublicationYear(@Nullable Integer publicationYear) {
    this.publicationYear = publicationYear;
  }

  public MagazineUpdate coverImageFileId(@Nullable UUID coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
    return this;
  }

  /**
   * Cover.
   * @return coverImageFileId
   */
  @Valid 
  @JsonProperty("coverImageFileId")
  public @Nullable UUID getCoverImageFileId() {
    return coverImageFileId;
  }

  @JsonProperty("coverImageFileId")
  public void setCoverImageFileId(@Nullable UUID coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
  }

  public MagazineUpdate documentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
    return this;
  }

  /**
   * Magazine file.
   * @return documentFileId
   */
  @Valid 
  @JsonProperty("documentFileId")
  public @Nullable UUID getDocumentFileId() {
    return documentFileId;
  }

  @JsonProperty("documentFileId")
  public void setDocumentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MagazineUpdate magazineUpdate = (MagazineUpdate) o;
    return Objects.equals(this.institutionId, magazineUpdate.institutionId) &&
        Objects.equals(this.academicYearId, magazineUpdate.academicYearId) &&
        Objects.equals(this.title, magazineUpdate.title) &&
        Objects.equals(this.publicationYear, magazineUpdate.publicationYear) &&
        Objects.equals(this.coverImageFileId, magazineUpdate.coverImageFileId) &&
        Objects.equals(this.documentFileId, magazineUpdate.documentFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, title, publicationYear, coverImageFileId, documentFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MagazineUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    publicationYear: ").append(toIndentedString(publicationYear)).append("\n");
    sb.append("    coverImageFileId: ").append(toIndentedString(coverImageFileId)).append("\n");
    sb.append("    documentFileId: ").append(toIndentedString(documentFileId)).append("\n");
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

