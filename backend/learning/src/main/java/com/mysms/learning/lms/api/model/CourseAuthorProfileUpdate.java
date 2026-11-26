// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of CourseAuthorProfile that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CourseAuthorProfileUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> bio;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> qualification;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public CourseAuthorProfileUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Author name.
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

  public CourseAuthorProfileUpdate bio(@Nullable Optional<String> bio) {
    this.bio = bio;
    return this;
  }

  /**
   * Biography.
   * @return bio
   */
  
  @JsonProperty("bio")
  public @Nullable Optional<String> getBio() {
    return bio;
  }

  @JsonProperty("bio")
  public void setBio(@Nullable Optional<String> bio) {
    this.bio = bio;
  }

  public CourseAuthorProfileUpdate photoFileId(@Nullable Optional<UUID> photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Photo.
   * @return photoFileId
   */
  
  @JsonProperty("photoFileId")
  public @Nullable Optional<UUID> getPhotoFileId() {
    return photoFileId;
  }

  @JsonProperty("photoFileId")
  public void setPhotoFileId(@Nullable Optional<UUID> photoFileId) {
    this.photoFileId = photoFileId;
  }

  public CourseAuthorProfileUpdate qualification(@Nullable Optional<String> qualification) {
    this.qualification = qualification;
    return this;
  }

  /**
   * Qualification.
   * @return qualification
   */
  
  @JsonProperty("qualification")
  public @Nullable Optional<String> getQualification() {
    return qualification;
  }

  @JsonProperty("qualification")
  public void setQualification(@Nullable Optional<String> qualification) {
    this.qualification = qualification;
  }

  public CourseAuthorProfileUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

