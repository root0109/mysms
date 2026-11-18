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
 * Fields of Notice that can be updated (partial update; see updateMask).
 */

public class NoticeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

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
  private @Nullable AudienceEnum audience;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID backgroundId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime expireAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showInCalendar;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowReplies;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID discussionTopicId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isClosed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isDeactivated;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String authorComment;

  public NoticeUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide; audiences in comms.notice_audience).
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

  public NoticeUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year (NULL = not tied to a year, e.g. cohort or tenant notices).
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

  public NoticeUpdate audience(@Nullable AudienceEnum audience) {
    this.audience = audience;
    return this;
  }

  /**
   * Audience.
   * @return audience
   */
  
  @JsonProperty("audience")
  public @Nullable AudienceEnum getAudience() {
    return audience;
  }

  @JsonProperty("audience")
  public void setAudience(@Nullable AudienceEnum audience) {
    this.audience = audience;
  }

  public NoticeUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class (audience = class).
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public NoticeUpdate backgroundId(@Nullable UUID backgroundId) {
    this.backgroundId = backgroundId;
    return this;
  }

  /**
   * Background image.
   * @return backgroundId
   */
  @Valid 
  @JsonProperty("backgroundId")
  public @Nullable UUID getBackgroundId() {
    return backgroundId;
  }

  @JsonProperty("backgroundId")
  public void setBackgroundId(@Nullable UUID backgroundId) {
    this.backgroundId = backgroundId;
  }

  public NoticeUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public NoticeUpdate body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Body.
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

  public NoticeUpdate attachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  @Valid 
  @JsonProperty("attachmentFileId")
  public @Nullable UUID getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public NoticeUpdate publishAt(@Nullable OffsetDateTime publishAt) {
    this.publishAt = publishAt;
    return this;
  }

  /**
   * Publish time.
   * @return publishAt
   */
  @Valid 
  @JsonProperty("publishAt")
  public @Nullable OffsetDateTime getPublishAt() {
    return publishAt;
  }

  @JsonProperty("publishAt")
  public void setPublishAt(@Nullable OffsetDateTime publishAt) {
    this.publishAt = publishAt;
  }

  public NoticeUpdate expireAt(@Nullable OffsetDateTime expireAt) {
    this.expireAt = expireAt;
    return this;
  }

  /**
   * Expiry time.
   * @return expireAt
   */
  @Valid 
  @JsonProperty("expireAt")
  public @Nullable OffsetDateTime getExpireAt() {
    return expireAt;
  }

  @JsonProperty("expireAt")
  public void setExpireAt(@Nullable OffsetDateTime expireAt) {
    this.expireAt = expireAt;
  }

  public NoticeUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort the notice is posted to (audience = cohort).
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

  public NoticeUpdate showInCalendar(@Nullable Boolean showInCalendar) {
    this.showInCalendar = showInCalendar;
    return this;
  }

  /**
   * Also shown in the calendar.
   * @return showInCalendar
   */
  
  @JsonProperty("showInCalendar")
  public @Nullable Boolean getShowInCalendar() {
    return showInCalendar;
  }

  @JsonProperty("showInCalendar")
  public void setShowInCalendar(@Nullable Boolean showInCalendar) {
    this.showInCalendar = showInCalendar;
  }

  public NoticeUpdate allowReplies(@Nullable Boolean allowReplies) {
    this.allowReplies = allowReplies;
    return this;
  }

  /**
   * Readers may reply (through the linked discussion topic).
   * @return allowReplies
   */
  
  @JsonProperty("allowReplies")
  public @Nullable Boolean getAllowReplies() {
    return allowReplies;
  }

  @JsonProperty("allowReplies")
  public void setAllowReplies(@Nullable Boolean allowReplies) {
    this.allowReplies = allowReplies;
  }

  public NoticeUpdate discussionTopicId(@Nullable UUID discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
    return this;
  }

  /**
   * Discussion topic holding the replies.
   * @return discussionTopicId
   */
  @Valid 
  @JsonProperty("discussionTopicId")
  public @Nullable UUID getDiscussionTopicId() {
    return discussionTopicId;
  }

  @JsonProperty("discussionTopicId")
  public void setDiscussionTopicId(@Nullable UUID discussionTopicId) {
    this.discussionTopicId = discussionTopicId;
  }

  public NoticeUpdate isClosed(@Nullable Boolean isClosed) {
    this.isClosed = isClosed;
    return this;
  }

  /**
   * Replies closed.
   * @return isClosed
   */
  
  @JsonProperty("isClosed")
  public @Nullable Boolean getIsClosed() {
    return isClosed;
  }

  @JsonProperty("isClosed")
  public void setIsClosed(@Nullable Boolean isClosed) {
    this.isClosed = isClosed;
  }

  public NoticeUpdate isDeactivated(@Nullable Boolean isDeactivated) {
    this.isDeactivated = isDeactivated;
    return this;
  }

  /**
   * Withdrawn by the author.
   * @return isDeactivated
   */
  
  @JsonProperty("isDeactivated")
  public @Nullable Boolean getIsDeactivated() {
    return isDeactivated;
  }

  @JsonProperty("isDeactivated")
  public void setIsDeactivated(@Nullable Boolean isDeactivated) {
    this.isDeactivated = isDeactivated;
  }

  public NoticeUpdate authorComment(@Nullable String authorComment) {
    this.authorComment = authorComment;
    return this;
  }

  /**
   * Author's note on the latest change.
   * @return authorComment
   */
  
  @JsonProperty("authorComment")
  public @Nullable String getAuthorComment() {
    return authorComment;
  }

  @JsonProperty("authorComment")
  public void setAuthorComment(@Nullable String authorComment) {
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

