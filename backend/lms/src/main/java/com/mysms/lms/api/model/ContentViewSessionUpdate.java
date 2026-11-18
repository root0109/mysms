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
 * Fields of ContentViewSession that can be updated (partial update; see updateMask).
 */

public class ContentViewSessionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String sessionRef;

  public ContentViewSessionUpdate contentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item.
   * @return contentItemId
   */
  @Valid 
  @JsonProperty("contentItemId")
  public @Nullable UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public ContentViewSessionUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Document viewed.
   * @return fileId
   */
  @Valid 
  @JsonProperty("fileId")
  public @Nullable UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable UUID fileId) {
    this.fileId = fileId;
  }

  public ContentViewSessionUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
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

  public ContentViewSessionUpdate startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Start.
   * @return startedAt
   */
  @Valid 
  @JsonProperty("startedAt")
  public @Nullable OffsetDateTime getStartedAt() {
    return startedAt;
  }

  @JsonProperty("startedAt")
  public void setStartedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public ContentViewSessionUpdate endedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  /**
   * End.
   * @return endedAt
   */
  @Valid 
  @JsonProperty("endedAt")
  public @Nullable OffsetDateTime getEndedAt() {
    return endedAt;
  }

  @JsonProperty("endedAt")
  public void setEndedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  public ContentViewSessionUpdate location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Position reached (page / time code).
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public ContentViewSessionUpdate sessionRef(@Nullable String sessionRef) {
    this.sessionRef = sessionRef;
    return this;
  }

  /**
   * External session identifier (hashed).
   * @return sessionRef
   */
  @Size(max = 128) 
  @JsonProperty("sessionRef")
  public @Nullable String getSessionRef() {
    return sessionRef;
  }

  @JsonProperty("sessionRef")
  public void setSessionRef(@Nullable String sessionRef) {
    this.sessionRef = sessionRef;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentViewSessionUpdate contentViewSessionUpdate = (ContentViewSessionUpdate) o;
    return Objects.equals(this.contentItemId, contentViewSessionUpdate.contentItemId) &&
        Objects.equals(this.fileId, contentViewSessionUpdate.fileId) &&
        Objects.equals(this.cohortId, contentViewSessionUpdate.cohortId) &&
        Objects.equals(this.startedAt, contentViewSessionUpdate.startedAt) &&
        Objects.equals(this.endedAt, contentViewSessionUpdate.endedAt) &&
        Objects.equals(this.location, contentViewSessionUpdate.location) &&
        Objects.equals(this.sessionRef, contentViewSessionUpdate.sessionRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, fileId, cohortId, startedAt, endedAt, location, sessionRef);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentViewSessionUpdate {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    sessionRef: ").append(toIndentedString(sessionRef)).append("\n");
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

