// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ForumSection that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ForumSectionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> forumId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  /**
   * Section type (from the fixed sub-forum type catalogue).
   */
  public enum SectionTypeEnum {
    GENERAL("general"),
    
    QUESTION_ANSWER("question_answer"),
    
    INDIVIDUAL("individual"),
    
    GROUP("group"),
    
    ANNOUNCEMENT("announcement"),
    
    ASSIGNMENT("assignment"),
    
    DISCUSSION_PROMPT("discussion_prompt"),
    
    MENTOR("mentor"),
    
    OTHER("other");

    private final String value;

    SectionTypeEnum(String value) {
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
    public static SectionTypeEnum fromValue(String value) {
      for (SectionTypeEnum b : SectionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull SectionTypeEnum> sectionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortGroupId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public ForumSectionUpdate forumId(@Nullable Optional<UUID> forumId) {
    this.forumId = forumId;
    return this;
  }

  /**
   * Forum.
   * @return forumId
   */
  
  @JsonProperty("forumId")
  public @Nullable Optional<UUID> getForumId() {
    return forumId;
  }

  @JsonProperty("forumId")
  public void setForumId(@Nullable Optional<UUID> forumId) {
    this.forumId = forumId;
  }

  public ForumSectionUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public ForumSectionUpdate description(@Nullable Optional<String> description) {
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

  public ForumSectionUpdate sectionType(@Nullable Optional<SectionTypeEnum> sectionType) {
    this.sectionType = sectionType;
    return this;
  }

  /**
   * Section type (from the fixed sub-forum type catalogue).
   * @return sectionType
   */
  
  @JsonProperty("sectionType")
  public @Nullable Optional<SectionTypeEnum> getSectionType() {
    return sectionType;
  }

  @JsonProperty("sectionType")
  public void setSectionType(@Nullable Optional<SectionTypeEnum> sectionType) {
    this.sectionType = sectionType;
  }

  public ForumSectionUpdate cohortGroupId(@Nullable Optional<UUID> cohortGroupId) {
    this.cohortGroupId = cohortGroupId;
    return this;
  }

  /**
   * Group the section is restricted to.
   * @return cohortGroupId
   */
  
  @JsonProperty("cohortGroupId")
  public @Nullable Optional<UUID> getCohortGroupId() {
    return cohortGroupId;
  }

  @JsonProperty("cohortGroupId")
  public void setCohortGroupId(@Nullable Optional<UUID> cohortGroupId) {
    this.cohortGroupId = cohortGroupId;
  }

  public ForumSectionUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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
    ForumSectionUpdate forumSectionUpdate = (ForumSectionUpdate) o;
    return Objects.equals(this.forumId, forumSectionUpdate.forumId) &&
        Objects.equals(this.title, forumSectionUpdate.title) &&
        Objects.equals(this.description, forumSectionUpdate.description) &&
        Objects.equals(this.sectionType, forumSectionUpdate.sectionType) &&
        Objects.equals(this.cohortGroupId, forumSectionUpdate.cohortGroupId) &&
        Objects.equals(this.sortOrder, forumSectionUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumId, title, description, sectionType, cohortGroupId, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumSectionUpdate {\n");
    sb.append("    forumId: ").append(toIndentedString(forumId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    sectionType: ").append(toIndentedString(sectionType)).append("\n");
    sb.append("    cohortGroupId: ").append(toIndentedString(cohortGroupId)).append("\n");
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

