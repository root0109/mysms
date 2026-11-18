// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of CourseAuthorProfile that can be updated (partial update; see updateMask).
 */

public class CourseAuthorProfileUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bio;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String qualification;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public CourseAuthorProfileUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Author name.
   * @return name
   */
  @Size(max = 200) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public CourseAuthorProfileUpdate bio(@Nullable String bio) {
    this.bio = bio;
    return this;
  }

  /**
   * Biography.
   * @return bio
   */
  
  @JsonProperty("bio")
  public @Nullable String getBio() {
    return bio;
  }

  @JsonProperty("bio")
  public void setBio(@Nullable String bio) {
    this.bio = bio;
  }

  public CourseAuthorProfileUpdate photoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Photo.
   * @return photoFileId
   */
  @Valid 
  @JsonProperty("photoFileId")
  public @Nullable UUID getPhotoFileId() {
    return photoFileId;
  }

  @JsonProperty("photoFileId")
  public void setPhotoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
  }

  public CourseAuthorProfileUpdate qualification(@Nullable String qualification) {
    this.qualification = qualification;
    return this;
  }

  /**
   * Qualification.
   * @return qualification
   */
  @Size(max = 255) 
  @JsonProperty("qualification")
  public @Nullable String getQualification() {
    return qualification;
  }

  @JsonProperty("qualification")
  public void setQualification(@Nullable String qualification) {
    this.qualification = qualification;
  }

  public CourseAuthorProfileUpdate sortOrder(@Nullable Integer sortOrder) {
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
    CourseAuthorProfileUpdate courseAuthorProfileUpdate = (CourseAuthorProfileUpdate) o;
    return Objects.equals(this.name, courseAuthorProfileUpdate.name) &&
        Objects.equals(this.bio, courseAuthorProfileUpdate.bio) &&
        Objects.equals(this.photoFileId, courseAuthorProfileUpdate.photoFileId) &&
        Objects.equals(this.qualification, courseAuthorProfileUpdate.qualification) &&
        Objects.equals(this.sortOrder, courseAuthorProfileUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, bio, photoFileId, qualification, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseAuthorProfileUpdate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    bio: ").append(toIndentedString(bio)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
    sb.append("    qualification: ").append(toIndentedString(qualification)).append("\n");
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

