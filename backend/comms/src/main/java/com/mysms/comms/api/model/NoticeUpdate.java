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
 * Fields of Notice that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NoticeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  /**
   * Audience.
   */
  public enum AudienceEnum {
    SCHOOL("school"),
    
    propertyClass("class"),
    
    TEACHERS("teachers"),
    
    COHORT("cohort"),
    
    ROLES("roles");

    private final String value;

    AudienceEnum(String value) {
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
    public static AudienceEnum fromValue(String value) {
      for (AudienceEnum b : AudienceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull AudienceEnum> audience;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> backgroundId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> publishAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> expireAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showInCalendar;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowReplies;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> discussionTopicId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isClosed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isDeactivated;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> authorComment;

  public NoticeUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide; audiences in comms.notice_audience).
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

  public NoticeUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year (NULL = not tied to a year, e.g. cohort or tenant notices).
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

  public NoticeUpdate audience(@Nullable Optional<AudienceEnum> audience) {
    this.audience = audience;
    return this;
  }

  /**
   * Audience.
   * @return audience
   */
  
  @JsonProperty("audience")
  public @Nullable Optional<AudienceEnum> getAudience() {
    return audience;
  }

  @JsonProperty("audience")
  public void setAudience(@Nullable Optional<AudienceEnum> audience) {
    this.audience = audience;
  }

  public NoticeUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (audience = class).
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public NoticeUpdate backgroundId(@Nullable Optional<UUID> backgroundId) {
    this.backgroundId = backgroundId;
    return this;
  }

  /**
   * Background image.
   * @return backgroundId
   */
  
  @JsonProperty("backgroundId")
  public @Nullable Optional<UUID> getBackgroundId() {
    return backgroundId;
  }

  @JsonProperty("backgroundId")
  public void setBackgroundId(@Nullable Optional<UUID> backgroundId) {
    this.backgroundId = backgroundId;
  }

  public NoticeUpdate title(@Nullable Optional<String> title) {
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

  public NoticeUpdate body(@Nullable Optional<String> body) {
    this.body = body;
    return this;
  }

  /**
   * Body.
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

  public NoticeUpdate attachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  
  @JsonProperty("attachmentFileId")
  public @Nullable Optional<UUID> getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public NoticeUpdate publishAt(@Nullable Optional<OffsetDateTime> publishAt) {
    this.publishAt = publishAt;
    return this;
  }

  /**
   * Publish time.
   * @return publishAt
   */
  
  @JsonProperty("publishAt")
  public @Nullable Optional<OffsetDateTime> getPublishAt() {
    return publishAt;
  }

  @JsonProperty("publishAt")
  public void setPublishAt(@Nullable Optional<OffsetDateTime> publishAt) {
    this.publishAt = publishAt;
  }

  public NoticeUpdate expireAt(@Nullable Optional<OffsetDateTime> expireAt) {
    this.expireAt = expireAt;
    return this;
  }

  /**
   * Expiry time.
   * @return expireAt
   */
  
  @JsonProperty("expireAt")
  public @Nullable Optional<OffsetDateTime> getExpireAt() {
    return expireAt;
  }

  @JsonProperty("expireAt")
  public void setExpireAt(@Nullable Optional<OffsetDateTime> expireAt) {
    this.expireAt = expireAt;
  }

  public NoticeUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort the notice is posted to (audience = cohort).
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public NoticeUpdate showInCalendar(@Nullable Optional<Boolean> showInCalendar) {
    this.showInCalendar = showInCalendar;
    return this;
  }

  /**
   * Also shown in the calendar.
   * @return showInCalendar
   */
  
  @JsonProperty("showInCalendar")
  public @Nullable Optional<Boolean> getShowInCalendar() {
    return showInCalendar;
  }

  @JsonProperty("showInCalendar")
  public void setShowInCalendar(@Nullable Optional<Boolean> showInCalendar) {
    this.showInCalendar = showInCalendar;
  }

  public NoticeUpdate allowReplies(@Nullable Optional<Boolean> allowReplies) {
    this.allowReplies = allowReplies;
    return this;
  }

  /**
   * Readers may reply (through the linked discussion topic).
   * @return allowReplies
   */
  
  @JsonProperty("allowReplies")
  public @Nullable Optional<Boolean> getAllowReplies() {
    return allowReplies;
  }

  @JsonProperty("allowReplies")
  public void setAllowReplies(@Nullable Optional<Boolean> allowReplies) {
    this.allowReplies = allowReplies;
  }

  public NoticeUpdate discussionTopicId(@Nullable Optional<UUID> discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
    return this;
  }

  /**
   * Discussion topic holding the replies.
   * @return discussionTopicId
   */
  
  @JsonProperty("discussionTopicId")
  public @Nullable Optional<UUID> getDiscussionTopicId() {
    return discussionTopicId;
  }

  @JsonProperty("discussionTopicId")
  public void setDiscussionTopicId(@Nullable Optional<UUID> discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
  }

  public NoticeUpdate isClosed(@Nullable Optional<Boolean> isClosed) {
    this.isClosed = isClosed;
    return this;
  }

  /**
   * Replies closed.
   * @return isClosed
   */
  
  @JsonProperty("isClosed")
  public @Nullable Optional<Boolean> getIsClosed() {
    return isClosed;
  }

  @JsonProperty("isClosed")
  public void setIsClosed(@Nullable Optional<Boolean> isClosed) {
    this.isClosed = isClosed;
  }

  public NoticeUpdate isDeactivated(@Nullable Optional<Boolean> isDeactivated) {
    this.isDeactivated = isDeactivated;
    return this;
  }

  /**
   * Withdrawn by the author.
   * @return isDeactivated
   */
  
  @JsonProperty("isDeactivated")
  public @Nullable Optional<Boolean> getIsDeactivated() {
    return isDeactivated;
  }

  @JsonProperty("isDeactivated")
  public void setIsDeactivated(@Nullable Optional<Boolean> isDeactivated) {
    this.isDeactivated = isDeactivated;
  }

  public NoticeUpdate authorComment(@Nullable Optional<String> authorComment) {
    this.authorComment = authorComment;
    return this;
  }

  /**
   * Author's note on the latest change.
   * @return authorComment
   */
  
  @JsonProperty("authorComment")
  public @Nullable Optional<String> getAuthorComment() {
    return authorComment;
  }

  @JsonProperty("authorComment")
  public void setAuthorComment(@Nullable Optional<String> authorComment) {
    this.authorComment = authorComment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NoticeUpdate noticeUpdate = (NoticeUpdate) o;
    return Objects.equals(this.institutionId, noticeUpdate.institutionId) &&
        Objects.equals(this.academicYearId, noticeUpdate.academicYearId) &&
        Objects.equals(this.audience, noticeUpdate.audience) &&
        Objects.equals(this.classSectionId, noticeUpdate.classSectionId) &&
        Objects.equals(this.backgroundId, noticeUpdate.backgroundId) &&
        Objects.equals(this.title, noticeUpdate.title) &&
        Objects.equals(this.body, noticeUpdate.body) &&
        Objects.equals(this.attachmentFileId, noticeUpdate.attachmentFileId) &&
        Objects.equals(this.publishAt, noticeUpdate.publishAt) &&
        Objects.equals(this.expireAt, noticeUpdate.expireAt) &&
        Objects.equals(this.cohortId, noticeUpdate.cohortId) &&
        Objects.equals(this.showInCalendar, noticeUpdate.showInCalendar) &&
        Objects.equals(this.allowReplies, noticeUpdate.allowReplies) &&
        Objects.equals(this.discussionTopicId, noticeUpdate.discussionTopicId) &&
        Objects.equals(this.isClosed, noticeUpdate.isClosed) &&
        Objects.equals(this.isDeactivated, noticeUpdate.isDeactivated) &&
        Objects.equals(this.authorComment, noticeUpdate.authorComment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, audience, classSectionId, backgroundId, title, body, attachmentFileId, publishAt, expireAt, cohortId, showInCalendar, allowReplies, discussionTopicId, isClosed, isDeactivated, authorComment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NoticeUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    audience: ").append(toIndentedString(audience)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    backgroundId: ").append(toIndentedString(backgroundId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    publishAt: ").append(toIndentedString(publishAt)).append("\n");
    sb.append("    expireAt: ").append(toIndentedString(expireAt)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    showInCalendar: ").append(toIndentedString(showInCalendar)).append("\n");
    sb.append("    allowReplies: ").append(toIndentedString(allowReplies)).append("\n");
    sb.append("    discussionTopicId: ").append(toIndentedString(discussionTopicId)).append("\n");
    sb.append("    isClosed: ").append(toIndentedString(isClosed)).append("\n");
    sb.append("    isDeactivated: ").append(toIndentedString(isDeactivated)).append("\n");
    sb.append("    authorComment: ").append(toIndentedString(authorComment)).append("\n");
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

