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
 * Fields of QuestionHotspot that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class QuestionHotspotUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> hotspotType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> imageFileId;

  public QuestionHotspotUpdate hotspotType(@Nullable Optional<String> hotspotType) {
    this.hotspotType = hotspotType;
    return this;
  }

  /**
   * Selection mode (single, multiple).
   * @return hotspotType
   */
  
  @JsonProperty("hotspotType")
  public @Nullable Optional<String> getHotspotType() {
    return hotspotType;
  }

  @JsonProperty("hotspotType")
  public void setHotspotType(@Nullable Optional<String> hotspotType) {
    this.hotspotType = hotspotType;
  }

  public QuestionHotspotUpdate imageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Image.
   * @return imageFileId
   */
  
  @JsonProperty("imageFileId")
  public @Nullable Optional<UUID> getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuestionHotspotUpdate questionHotspotUpdate = (QuestionHotspotUpdate) o;
    return Objects.equals(this.hotspotType, questionHotspotUpdate.hotspotType) &&
        Objects.equals(this.imageFileId, questionHotspotUpdate.imageFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hotspotType, imageFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuestionHotspotUpdate {\n");
    sb.append("    hotspotType: ").append(toIndentedString(hotspotType)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
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

