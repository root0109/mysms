// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
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
 * Fields of ClassPage that can be updated (partial update; see updateMask).
 */

public class ClassPageUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID pageCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID coverImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public ClassPageUpdate institutionId(@Nullable UUID institutionId) {
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

  public ClassPageUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public ClassPageUpdate pageCategoryId(@Nullable UUID pageCategoryId) {
    this.pageCategoryId = pageCategoryId;
    return this;
  }

  /**
   * Category.
   * @return pageCategoryId
   */
  @Valid 
  @JsonProperty("pageCategoryId")
  public @Nullable UUID getPageCategoryId() {
    return pageCategoryId;
  }

  @JsonProperty("pageCategoryId")
  public void setPageCategoryId(@Nullable UUID pageCategoryId) {
    this.pageCategoryId = pageCategoryId;
  }

  public ClassPageUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Page name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ClassPageUpdate description(@Nullable String description) {
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

  public ClassPageUpdate coverImageFileId(@Nullable UUID coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
    return this;
  }

  /**
   * Cover image.
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

  public ClassPageUpdate ownerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Page owner.
   * @return ownerUserId
   */
  @Valid 
  @JsonProperty("ownerUserId")
  public @Nullable UUID getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public ClassPageUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
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
    ClassPageUpdate classPageUpdate = (ClassPageUpdate) o;
    return Objects.equals(this.institutionId, classPageUpdate.institutionId) &&
        Objects.equals(this.academicYearId, classPageUpdate.academicYearId) &&
        Objects.equals(this.pageCategoryId, classPageUpdate.pageCategoryId) &&
        Objects.equals(this.name, classPageUpdate.name) &&
        Objects.equals(this.description, classPageUpdate.description) &&
        Objects.equals(this.coverImageFileId, classPageUpdate.coverImageFileId) &&
        Objects.equals(this.ownerUserId, classPageUpdate.ownerUserId) &&
        Objects.equals(this.isActive, classPageUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, pageCategoryId, name, description, coverImageFileId, ownerUserId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassPageUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    pageCategoryId: ").append(toIndentedString(pageCategoryId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    coverImageFileId: ").append(toIndentedString(coverImageFileId)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
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

