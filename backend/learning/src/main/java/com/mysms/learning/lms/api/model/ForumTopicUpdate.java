// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ForumTopic that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ForumTopicUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> forumSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  /**
   * Topic type (from the fixed topic type catalogue).
   */
  public enum TopicTypeEnum {
    DISCUSSION("discussion"),
    
    QUESTION("question"),
    
    ANNOUNCEMENT("announcement"),
    
    ASSIGNMENT("assignment"),
    
    DISCUSSION_PROMPT("discussion_prompt"),
    
    INDIVIDUAL("individual"),
    
    GROUP("group"),
    
    OTHER("other");

    private final String value;

    TopicTypeEnum(String value) {
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
    public static TopicTypeEnum fromValue(String value) {
      for (TopicTypeEnum b : TopicTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull TopicTypeEnum> topicType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> opensAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> closesAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> discussionPromptId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> linkedEntityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> linkedEntityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> individualUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isGraded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> pointsPossible;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> gradeWeightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> gradingDueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isGradingCompleted;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isHidden;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isTeacherTopic;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isSystemGenerated;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> lastPostId;

  public ForumTopicUpdate forumSectionId(@Nullable Optional<UUID> forumSectionId) {
    this.forumSectionId = forumSectionId;
    return this;
  }

  /**
   * Section.
   * @return forumSectionId
   */
  
  @JsonProperty("forumSectionId")
  public @Nullable Optional<UUID> getForumSectionId() {
    return forumSectionId;
  }

  @JsonProperty("forumSectionId")
  public void setForumSectionId(@Nullable Optional<UUID> forumSectionId) {
    this.forumSectionId = forumSectionId;
  }

  public ForumTopicUpdate title(@Nullable Optional<String> title) {
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

  public ForumTopicUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Opening text.
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

  public ForumTopicUpdate topicType(@Nullable Optional<TopicTypeEnum> topicType) {
    this.topicType = topicType;
    return this;
  }

  /**
   * Topic type (from the fixed topic type catalogue).
   * @return topicType
   */
  
  @JsonProperty("topicType")
  public @Nullable Optional<TopicTypeEnum> getTopicType() {
    return topicType;
  }

  @JsonProperty("topicType")
  public void setTopicType(@Nullable Optional<TopicTypeEnum> topicType) {
    this.topicType = topicType;
  }

  public ForumTopicUpdate opensAt(@Nullable Optional<OffsetDateTime> opensAt) {
    this.opensAt = opensAt;
    return this;
  }

  /**
   * Opens.
   * @return opensAt
   */
  
  @JsonProperty("opensAt")
  public @Nullable Optional<OffsetDateTime> getOpensAt() {
    return opensAt;
  }

  @JsonProperty("opensAt")
  public void setOpensAt(@Nullable Optional<OffsetDateTime> opensAt) {
    this.opensAt = opensAt;
  }

  public ForumTopicUpdate closesAt(@Nullable Optional<OffsetDateTime> closesAt) {
    this.closesAt = closesAt;
    return this;
  }

  /**
   * Closes.
   * @return closesAt
   */
  
  @JsonProperty("closesAt")
  public @Nullable Optional<OffsetDateTime> getClosesAt() {
    return closesAt;
  }

  @JsonProperty("closesAt")
  public void setClosesAt(@Nullable Optional<OffsetDateTime> closesAt) {
    this.closesAt = closesAt;
  }

  public ForumTopicUpdate contentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Placement in a chapter (graded discussions).
   * @return contentItemId
   */
  
  @JsonProperty("contentItemId")
  public @Nullable Optional<UUID> getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(@Nullable Optional<UUID> contentItemId) {
    this.contentItemId = contentItemId;
  }

  public ForumTopicUpdate discussionPromptId(@Nullable Optional<UUID> discussionPromptId) {
    this.discussionPromptId = discussionPromptId;
    return this;
  }

  /**
   * Prompt the topic was created from.
   * @return discussionPromptId
   */
  
  @JsonProperty("discussionPromptId")
  public @Nullable Optional<UUID> getDiscussionPromptId() {
    return discussionPromptId;
  }

  @JsonProperty("discussionPromptId")
  public void setDiscussionPromptId(@Nullable Optional<UUID> discussionPromptId) {
    this.discussionPromptId = discussionPromptId;
  }

  public ForumTopicUpdate linkedEntityType(@Nullable Optional<String> linkedEntityType) {
    this.linkedEntityType = linkedEntityType;
    return this;
  }

  /**
   * Kind of record the topic discusses (assignment, notice, content_item...).
   * @return linkedEntityType
   */
  
  @JsonProperty("linkedEntityType")
  public @Nullable Optional<String> getLinkedEntityType() {
    return linkedEntityType;
  }

  @JsonProperty("linkedEntityType")
  public void setLinkedEntityType(@Nullable Optional<String> linkedEntityType) {
    this.linkedEntityType = linkedEntityType;
  }

  public ForumTopicUpdate linkedEntityId(@Nullable Optional<UUID> linkedEntityId) {
    this.linkedEntityId = linkedEntityId;
    return this;
  }

  /**
   * Polymorphic id of the record the topic discusses.
   * @return linkedEntityId
   */
  
  @JsonProperty("linkedEntityId")
  public @Nullable Optional<UUID> getLinkedEntityId() {
    return linkedEntityId;
  }

  @JsonProperty("linkedEntityId")
  public void setLinkedEntityId(@Nullable Optional<UUID> linkedEntityId) {
    this.linkedEntityId = linkedEntityId;
  }

  public ForumTopicUpdate individualUserId(@Nullable Optional<UUID> individualUserId) {
    this.individualUserId = individualUserId;
    return this;
  }

  /**
   * Student of an individual (one-to-one) topic.
   * @return individualUserId
   */
  
  @JsonProperty("individualUserId")
  public @Nullable Optional<UUID> getIndividualUserId() {
    return individualUserId;
  }

  @JsonProperty("individualUserId")
  public void setIndividualUserId(@Nullable Optional<UUID> individualUserId) {
    this.individualUserId = individualUserId;
  }

  public ForumTopicUpdate isGraded(@Nullable Optional<Boolean> isGraded) {
    this.isGraded = isGraded;
    return this;
  }

  /**
   * Graded.
   * @return isGraded
   */
  
  @JsonProperty("isGraded")
  public @Nullable Optional<Boolean> getIsGraded() {
    return isGraded;
  }

  @JsonProperty("isGraded")
  public void setIsGraded(@Nullable Optional<Boolean> isGraded) {
    this.isGraded = isGraded;
  }

  public ForumTopicUpdate pointsPossible(@Nullable Optional<String> pointsPossible) {
    this.pointsPossible = pointsPossible;
    return this;
  }

  /**
   * Maximum points.
   * @return pointsPossible
   */
  
  @JsonProperty("pointsPossible")
  public @Nullable Optional<String> getPointsPossible() {
    return pointsPossible;
  }

  @JsonProperty("pointsPossible")
  public void setPointsPossible(@Nullable Optional<String> pointsPossible) {
    this.pointsPossible = pointsPossible;
  }

  public ForumTopicUpdate gradeWeightPercent(@Nullable Optional<String> gradeWeightPercent) {
    this.gradeWeightPercent = gradeWeightPercent;
    return this;
  }

  /**
   * Weight.
   * @return gradeWeightPercent
   */
  
  @JsonProperty("gradeWeightPercent")
  public @Nullable Optional<String> getGradeWeightPercent() {
    return gradeWeightPercent;
  }

  @JsonProperty("gradeWeightPercent")
  public void setGradeWeightPercent(@Nullable Optional<String> gradeWeightPercent) {
    this.gradeWeightPercent = gradeWeightPercent;
  }

  public ForumTopicUpdate gradingDueAt(@Nullable Optional<OffsetDateTime> gradingDueAt) {
    this.gradingDueAt = gradingDueAt;
    return this;
  }

  /**
   * Grading due date.
   * @return gradingDueAt
   */
  
  @JsonProperty("gradingDueAt")
  public @Nullable Optional<OffsetDateTime> getGradingDueAt() {
    return gradingDueAt;
  }

  @JsonProperty("gradingDueAt")
  public void setGradingDueAt(@Nullable Optional<OffsetDateTime> gradingDueAt) {
    this.gradingDueAt = gradingDueAt;
  }

  public ForumTopicUpdate isGradingCompleted(@Nullable Optional<Boolean> isGradingCompleted) {
    this.isGradingCompleted = isGradingCompleted;
    return this;
  }

  /**
   * Grading finished.
   * @return isGradingCompleted
   */
  
  @JsonProperty("isGradingCompleted")
  public @Nullable Optional<Boolean> getIsGradingCompleted() {
    return isGradingCompleted;
  }

  @JsonProperty("isGradingCompleted")
  public void setIsGradingCompleted(@Nullable Optional<Boolean> isGradingCompleted) {
    this.isGradingCompleted = isGradingCompleted;
  }

  public ForumTopicUpdate isHidden(@Nullable Optional<Boolean> isHidden) {
    this.isHidden = isHidden;
    return this;
  }

  /**
   * Hidden from students.
   * @return isHidden
   */
  
  @JsonProperty("isHidden")
  public @Nullable Optional<Boolean> getIsHidden() {
    return isHidden;
  }

  @JsonProperty("isHidden")
  public void setIsHidden(@Nullable Optional<Boolean> isHidden) {
    this.isHidden = isHidden;
  }

  public ForumTopicUpdate isTeacherTopic(@Nullable Optional<Boolean> isTeacherTopic) {
    this.isTeacherTopic = isTeacherTopic;
    return this;
  }

  /**
   * Started by a teacher.
   * @return isTeacherTopic
   */
  
  @JsonProperty("isTeacherTopic")
  public @Nullable Optional<Boolean> getIsTeacherTopic() {
    return isTeacherTopic;
  }

  @JsonProperty("isTeacherTopic")
  public void setIsTeacherTopic(@Nullable Optional<Boolean> isTeacherTopic) {
    this.isTeacherTopic = isTeacherTopic;
  }

  public ForumTopicUpdate isSystemGenerated(@Nullable Optional<Boolean> isSystemGenerated) {
    this.isSystemGenerated = isSystemGenerated;
    return this;
  }

  /**
   * Created automatically.
   * @return isSystemGenerated
   */
  
  @JsonProperty("isSystemGenerated")
  public @Nullable Optional<Boolean> getIsSystemGenerated() {
    return isSystemGenerated;
  }

  @JsonProperty("isSystemGenerated")
  public void setIsSystemGenerated(@Nullable Optional<Boolean> isSystemGenerated) {
    this.isSystemGenerated = isSystemGenerated;
  }

  public ForumTopicUpdate lastPostId(@Nullable Optional<UUID> lastPostId) {
    this.lastPostId = lastPostId;
    return this;
  }

  /**
   * Latest post (cached).
   * @return lastPostId
   */
  
  @JsonProperty("lastPostId")
  public @Nullable Optional<UUID> getLastPostId() {
    return lastPostId;
  }

  @JsonProperty("lastPostId")
  public void setLastPostId(@Nullable Optional<UUID> lastPostId) {
    this.lastPostId = lastPostId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForumTopicUpdate forumTopicUpdate = (ForumTopicUpdate) o;
    return Objects.equals(this.forumSectionId, forumTopicUpdate.forumSectionId) &&
        Objects.equals(this.title, forumTopicUpdate.title) &&
        Objects.equals(this.description, forumTopicUpdate.description) &&
        Objects.equals(this.topicType, forumTopicUpdate.topicType) &&
        Objects.equals(this.opensAt, forumTopicUpdate.opensAt) &&
        Objects.equals(this.closesAt, forumTopicUpdate.closesAt) &&
        Objects.equals(this.contentItemId, forumTopicUpdate.contentItemId) &&
        Objects.equals(this.discussionPromptId, forumTopicUpdate.discussionPromptId) &&
        Objects.equals(this.linkedEntityType, forumTopicUpdate.linkedEntityType) &&
        Objects.equals(this.linkedEntityId, forumTopicUpdate.linkedEntityId) &&
        Objects.equals(this.individualUserId, forumTopicUpdate.individualUserId) &&
        Objects.equals(this.isGraded, forumTopicUpdate.isGraded) &&
        Objects.equals(this.pointsPossible, forumTopicUpdate.pointsPossible) &&
        Objects.equals(this.gradeWeightPercent, forumTopicUpdate.gradeWeightPercent) &&
        Objects.equals(this.gradingDueAt, forumTopicUpdate.gradingDueAt) &&
        Objects.equals(this.isGradingCompleted, forumTopicUpdate.isGradingCompleted) &&
        Objects.equals(this.isHidden, forumTopicUpdate.isHidden) &&
        Objects.equals(this.isTeacherTopic, forumTopicUpdate.isTeacherTopic) &&
        Objects.equals(this.isSystemGenerated, forumTopicUpdate.isSystemGenerated) &&
        Objects.equals(this.lastPostId, forumTopicUpdate.lastPostId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumSectionId, title, description, topicType, opensAt, closesAt, contentItemId, discussionPromptId, linkedEntityType, linkedEntityId, individualUserId, isGraded, pointsPossible, gradeWeightPercent, gradingDueAt, isGradingCompleted, isHidden, isTeacherTopic, isSystemGenerated, lastPostId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumTopicUpdate {\n");
    sb.append("    forumSectionId: ").append(toIndentedString(forumSectionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    topicType: ").append(toIndentedString(topicType)).append("\n");
    sb.append("    opensAt: ").append(toIndentedString(opensAt)).append("\n");
    sb.append("    closesAt: ").append(toIndentedString(closesAt)).append("\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    discussionPromptId: ").append(toIndentedString(discussionPromptId)).append("\n");
    sb.append("    linkedEntityType: ").append(toIndentedString(linkedEntityType)).append("\n");
    sb.append("    linkedEntityId: ").append(toIndentedString(linkedEntityId)).append("\n");
    sb.append("    individualUserId: ").append(toIndentedString(individualUserId)).append("\n");
    sb.append("    isGraded: ").append(toIndentedString(isGraded)).append("\n");
    sb.append("    pointsPossible: ").append(toIndentedString(pointsPossible)).append("\n");
    sb.append("    gradeWeightPercent: ").append(toIndentedString(gradeWeightPercent)).append("\n");
    sb.append("    gradingDueAt: ").append(toIndentedString(gradingDueAt)).append("\n");
    sb.append("    isGradingCompleted: ").append(toIndentedString(isGradingCompleted)).append("\n");
    sb.append("    isHidden: ").append(toIndentedString(isHidden)).append("\n");
    sb.append("    isTeacherTopic: ").append(toIndentedString(isTeacherTopic)).append("\n");
    sb.append("    isSystemGenerated: ").append(toIndentedString(isSystemGenerated)).append("\n");
    sb.append("    lastPostId: ").append(toIndentedString(lastPostId)).append("\n");
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

