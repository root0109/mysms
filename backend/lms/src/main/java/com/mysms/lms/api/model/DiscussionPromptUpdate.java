// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * Fields of DiscussionPrompt that can be updated (partial update; see updateMask).
 */

public class DiscussionPromptUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isInRepository;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean usesInlineAuthoring;

  public DiscussionPromptUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution.
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

  public DiscussionPromptUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public DiscussionPromptUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Prompt text.
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

  public DiscussionPromptUpdate isInRepository(@Nullable Boolean isInRepository) {
    this.isInRepository = isInRepository;
    return this;
  }

  /**
   * Listed in the shared content repository.
   * @return isInRepository
   */
  
  @JsonProperty("isInRepository")
  public @Nullable Boolean getIsInRepository() {
    return isInRepository;
  }

  @JsonProperty("isInRepository")
  public void setIsInRepository(@Nullable Boolean isInRepository) {
    this.isInRepository = isInRepository;
  }

  public DiscussionPromptUpdate usesInlineAuthoring(@Nullable Boolean usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
    return this;
  }

  /**
   * Body was authored with the inline editor (embedded media).
   * @return usesInlineAuthoring
   */
  
  @JsonProperty("usesInlineAuthoring")
  public @Nullable Boolean getUsesInlineAuthoring() {
    return usesInlineAuthoring;
  }

  @JsonProperty("usesInlineAuthoring")
  public void setUsesInlineAuthoring(@Nullable Boolean usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DiscussionPromptUpdate discussionPromptUpdate = (DiscussionPromptUpdate) o;
    return Objects.equals(this.institutionId, discussionPromptUpdate.institutionId) &&
        Objects.equals(this.title, discussionPromptUpdate.title) &&
        Objects.equals(this.description, discussionPromptUpdate.description) &&
        Objects.equals(this.isInRepository, discussionPromptUpdate.isInRepository) &&
        Objects.equals(this.usesInlineAuthoring, discussionPromptUpdate.usesInlineAuthoring);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, title, description, isInRepository, usesInlineAuthoring);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DiscussionPromptUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isInRepository: ").append(toIndentedString(isInRepository)).append("\n");
    sb.append("    usesInlineAuthoring: ").append(toIndentedString(usesInlineAuthoring)).append("\n");
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

