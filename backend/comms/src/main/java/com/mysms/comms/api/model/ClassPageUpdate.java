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
 * Fields of ClassPage that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ClassPageUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> pageCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> coverImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> ownerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public ClassPageUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public ClassPageUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public ClassPageUpdate pageCategoryId(@Nullable Optional<UUID> pageCategoryId) {
    this.pageCategoryId = pageCategoryId;
    return this;
  }

  /**
   * Category.
   * @return pageCategoryId
   */
  
  @JsonProperty("pageCategoryId")
  public @Nullable Optional<UUID> getPageCategoryId() {
    return pageCategoryId;
  }

  @JsonProperty("pageCategoryId")
  public void setPageCategoryId(@Nullable Optional<UUID> pageCategoryId) {
    this.pageCategoryId = pageCategoryId;
  }

  public ClassPageUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Page name.
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

  public ClassPageUpdate description(@Nullable Optional<String> description) {
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

  public ClassPageUpdate coverImageFileId(@Nullable Optional<UUID> coverImageFileId) {
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

  public ClassPageUpdate ownerUserId(@Nullable Optional<UUID> ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Page owner.
   * @return ownerUserId
   */
  
  @JsonProperty("ownerUserId")
  public @Nullable Optional<UUID> getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable Optional<UUID> ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public ClassPageUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Active.
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

