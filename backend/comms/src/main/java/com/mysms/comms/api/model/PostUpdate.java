// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Post that can be updated (partial update; see updateMask).
 */

public class PostUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classPageId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID authorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  /**
   * Who sees it.
   */
  public enum VisibilityEnum {
    ALL_CLASSES("all_classes"),
    
    SELECTED_CLASSES("selected_classes"),
    
    PAGE_MEMBERS("page_members");

    private final String value;

    VisibilityEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static VisibilityEnum fromValue(String value) {
      for (VisibilityEnum b : VisibilityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable VisibilityEnum visibility;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scheduledFor;

  public PostUpdate institutionId(@Nullable UUID institutionId) {
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

  public PostUpdate academicYearId(@Nullable UUID academicYearId) {
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

  public PostUpdate classPageId(@Nullable UUID classPageId) {
    this.classPageId = classPageId;
    return this;
  }

  /**
   * Page posted on.
   * @return classPageId
   */
  @Valid 
  @JsonProperty("classPageId")
  public @Nullable UUID getClassPageId() {
    return classPageId;
  }

  @JsonProperty("classPageId")
  public void setClassPageId(@Nullable UUID classPageId) {
    this.classPageId = classPageId;
  }

  public PostUpdate authorUserId(@Nullable UUID authorUserId) {
    this.authorUserId = authorUserId;
    return this;
  }

  /**
   * Author.
   * @return authorUserId
   */
  @Valid 
  @JsonProperty("authorUserId")
  public @Nullable UUID getAuthorUserId() {
    return authorUserId;
  }

  @JsonProperty("authorUserId")
  public void setAuthorUserId(@Nullable UUID authorUserId) {
    this.authorUserId = authorUserId;
  }

  public PostUpdate body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Post text.
   * @return body
   */
  
  @JsonProperty("body")
  public @Nullable String getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(@Nullable String body) {
    this.body = body;
  }

  public PostUpdate visibility(@Nullable VisibilityEnum visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Who sees it.
   * @return visibility
   */
  
  @JsonProperty("visibility")
  public @Nullable VisibilityEnum getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable VisibilityEnum visibility) {
    this.visibility = visibility;
  }

  public PostUpdate scheduledFor(@Nullable OffsetDateTime scheduledFor) {
    this.scheduledFor = scheduledFor;
    return this;
  }

  /**
   * Scheduled publish time.
   * @return scheduledFor
   */
  @Valid 
  @JsonProperty("scheduledFor")
  public @Nullable OffsetDateTime getScheduledFor() {
    return scheduledFor;
  }

  @JsonProperty("scheduledFor")
  public void setScheduledFor(@Nullable OffsetDateTime scheduledFor) {
    this.scheduledFor = scheduledFor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostUpdate postUpdate = (PostUpdate) o;
    return Objects.equals(this.institutionId, postUpdate.institutionId) &&
        Objects.equals(this.academicYearId, postUpdate.academicYearId) &&
        Objects.equals(this.classPageId, postUpdate.classPageId) &&
        Objects.equals(this.authorUserId, postUpdate.authorUserId) &&
        Objects.equals(this.body, postUpdate.body) &&
        Objects.equals(this.visibility, postUpdate.visibility) &&
        Objects.equals(this.scheduledFor, postUpdate.scheduledFor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, classPageId, authorUserId, body, visibility, scheduledFor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    classPageId: ").append(toIndentedString(classPageId)).append("\n");
    sb.append("    authorUserId: ").append(toIndentedString(authorUserId)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    visibility: ").append(toIndentedString(visibility)).append("\n");
    sb.append("    scheduledFor: ").append(toIndentedString(scheduledFor)).append("\n");
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

