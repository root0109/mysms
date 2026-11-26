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
 * Fields of Post that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PostUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classPageId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> body;

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
  private @Nullable Optional<@NotNull VisibilityEnum> visibility;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> scheduledFor;

  public PostUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public PostUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
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

  public PostUpdate classPageId(@Nullable Optional<UUID> classPageId) {
    this.classPageId = classPageId;
    return this;
  }

  /**
   * Page posted on.
   * @return classPageId
   */
  
  @JsonProperty("classPageId")
  public @Nullable Optional<UUID> getClassPageId() {
    return classPageId;
  }

  @JsonProperty("classPageId")
  public void setClassPageId(@Nullable Optional<UUID> classPageId) {
    this.classPageId = classPageId;
  }

  public PostUpdate body(@Nullable Optional<String> body) {
    this.body = body;
    return this;
  }

  /**
   * Post text.
   * @return body
   */
  
  @JsonProperty("body")
  public @Nullable Optional<String> getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(@Nullable Optional<String> body) {
    this.body = body;
  }

  public PostUpdate visibility(@Nullable Optional<VisibilityEnum> visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Who sees it.
   * @return visibility
   */
  
  @JsonProperty("visibility")
  public @Nullable Optional<VisibilityEnum> getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable Optional<VisibilityEnum> visibility) {
    this.visibility = visibility;
  }

  public PostUpdate scheduledFor(@Nullable Optional<OffsetDateTime> scheduledFor) {
    this.scheduledFor = scheduledFor;
    return this;
  }

  /**
   * Scheduled publish time.
   * @return scheduledFor
   */
  
  @JsonProperty("scheduledFor")
  public @Nullable Optional<OffsetDateTime> getScheduledFor() {
    return scheduledFor;
  }

  @JsonProperty("scheduledFor")
  public void setScheduledFor(@Nullable Optional<OffsetDateTime> scheduledFor) {
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
        Objects.equals(this.body, postUpdate.body) &&
        Objects.equals(this.visibility, postUpdate.visibility) &&
        Objects.equals(this.scheduledFor, postUpdate.scheduledFor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, classPageId, body, visibility, scheduledFor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    classPageId: ").append(toIndentedString(classPageId)).append("\n");
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

