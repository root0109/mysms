// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * Fields of ForumPost that can be updated (partial update; see updateMask).
 */

public class ForumPostUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentPostId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID authorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime postedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isDraft;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isTeacherPost;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer substantiveFlag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isHidden;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isHiddenByParent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isFlagged;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showSignature;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean usesInlineAuthoring;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isAbusive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean abuseReported;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer abuseReportCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID abuseConfirmedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime abuseConfirmedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer likeCount;

  public ForumPostUpdate parentPostId(@Nullable UUID parentPostId) {
    this.parentPostId = parentPostId;
    return this;
  }

  /**
   * Post replied to.
   * @return parentPostId
   */
  @Valid 
  @JsonProperty("parentPostId")
  public @Nullable UUID getParentPostId() {
    return parentPostId;
  }

  @JsonProperty("parentPostId")
  public void setParentPostId(@Nullable UUID parentPostId) {
    this.parentPostId = parentPostId;
  }

  public ForumPostUpdate title(@Nullable String title) {
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

  public ForumPostUpdate body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Message.
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

  public ForumPostUpdate authorUserId(@Nullable UUID authorUserId) {
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

  public ForumPostUpdate postedAt(@Nullable OffsetDateTime postedAt) {
    this.postedAt = postedAt;
    return this;
  }

  /**
   * Posted.
   * @return postedAt
   */
  @Valid 
  @JsonProperty("postedAt")
  public @Nullable OffsetDateTime getPostedAt() {
    return postedAt;
  }

  @JsonProperty("postedAt")
  public void setPostedAt(@Nullable OffsetDateTime postedAt) {
    this.postedAt = postedAt;
  }

  public ForumPostUpdate isDraft(@Nullable Boolean isDraft) {
    this.isDraft = isDraft;
    return this;
  }

  /**
   * Draft (not yet posted).
   * @return isDraft
   */
  
  @JsonProperty("isDraft")
  public @Nullable Boolean getIsDraft() {
    return isDraft;
  }

  @JsonProperty("isDraft")
  public void setIsDraft(@Nullable Boolean isDraft) {
    this.isDraft = isDraft;
  }

  public ForumPostUpdate isTeacherPost(@Nullable Boolean isTeacherPost) {
    this.isTeacherPost = isTeacherPost;
    return this;
  }

  /**
   * Written by a teacher.
   * @return isTeacherPost
   */
  
  @JsonProperty("isTeacherPost")
  public @Nullable Boolean getIsTeacherPost() {
    return isTeacherPost;
  }

  @JsonProperty("isTeacherPost")
  public void setIsTeacherPost(@Nullable Boolean isTeacherPost) {
    this.isTeacherPost = isTeacherPost;
  }

  public ForumPostUpdate substantiveFlag(@Nullable Integer substantiveFlag) {
    this.substantiveFlag = substantiveFlag;
    return this;
  }

  /**
   * Teacher marked it substantive (1) / non-substantive (0).
   * @return substantiveFlag
   */
  
  @JsonProperty("substantiveFlag")
  public @Nullable Integer getSubstantiveFlag() {
    return substantiveFlag;
  }

  @JsonProperty("substantiveFlag")
  public void setSubstantiveFlag(@Nullable Integer substantiveFlag) {
    this.substantiveFlag = substantiveFlag;
  }

  public ForumPostUpdate isHidden(@Nullable Boolean isHidden) {
    this.isHidden = isHidden;
    return this;
  }

  /**
   * Hidden by a moderator.
   * @return isHidden
   */
  
  @JsonProperty("isHidden")
  public @Nullable Boolean getIsHidden() {
    return isHidden;
  }

  @JsonProperty("isHidden")
  public void setIsHidden(@Nullable Boolean isHidden) {
    this.isHidden = isHidden;
  }

  public ForumPostUpdate isHiddenByParent(@Nullable Boolean isHiddenByParent) {
    this.isHiddenByParent = isHiddenByParent;
    return this;
  }

  /**
   * Hidden because a parent post was hidden.
   * @return isHiddenByParent
   */
  
  @JsonProperty("isHiddenByParent")
  public @Nullable Boolean getIsHiddenByParent() {
    return isHiddenByParent;
  }

  @JsonProperty("isHiddenByParent")
  public void setIsHiddenByParent(@Nullable Boolean isHiddenByParent) {
    this.isHiddenByParent = isHiddenByParent;
  }

  public ForumPostUpdate isFlagged(@Nullable Boolean isFlagged) {
    this.isFlagged = isFlagged;
    return this;
  }

  /**
   * Flagged for follow-up.
   * @return isFlagged
   */
  
  @JsonProperty("isFlagged")
  public @Nullable Boolean getIsFlagged() {
    return isFlagged;
  }

  @JsonProperty("isFlagged")
  public void setIsFlagged(@Nullable Boolean isFlagged) {
    this.isFlagged = isFlagged;
  }

  public ForumPostUpdate showSignature(@Nullable Boolean showSignature) {
    this.showSignature = showSignature;
    return this;
  }

  /**
   * Append the author's signature.
   * @return showSignature
   */
  
  @JsonProperty("showSignature")
  public @Nullable Boolean getShowSignature() {
    return showSignature;
  }

  @JsonProperty("showSignature")
  public void setShowSignature(@Nullable Boolean showSignature) {
    this.showSignature = showSignature;
  }

  public ForumPostUpdate usesInlineAuthoring(@Nullable Boolean usesInlineAuthoring) {
    this.usesInlineAuthoring = usesInlineAuthoring;
    return this;
  }

  /**
   * Authored with the inline editor.
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

  public ForumPostUpdate isAbusive(@Nullable Boolean isAbusive) {
    this.isAbusive = isAbusive;
    return this;
  }

  /**
   * Confirmed abusive.
   * @return isAbusive
   */
  
  @JsonProperty("isAbusive")
  public @Nullable Boolean getIsAbusive() {
    return isAbusive;
  }

  @JsonProperty("isAbusive")
  public void setIsAbusive(@Nullable Boolean isAbusive) {
    this.isAbusive = isAbusive;
  }

  public ForumPostUpdate abuseReported(@Nullable Boolean abuseReported) {
    this.abuseReported = abuseReported;
    return this;
  }

  /**
   * Reported by users.
   * @return abuseReported
   */
  
  @JsonProperty("abuseReported")
  public @Nullable Boolean getAbuseReported() {
    return abuseReported;
  }

  @JsonProperty("abuseReported")
  public void setAbuseReported(@Nullable Boolean abuseReported) {
    this.abuseReported = abuseReported;
  }

  public ForumPostUpdate abuseReportCount(@Nullable Integer abuseReportCount) {
    this.abuseReportCount = abuseReportCount;
    return this;
  }

  /**
   * Number of reports (cached).
   * @return abuseReportCount
   */
  
  @JsonProperty("abuseReportCount")
  public @Nullable Integer getAbuseReportCount() {
    return abuseReportCount;
  }

  @JsonProperty("abuseReportCount")
  public void setAbuseReportCount(@Nullable Integer abuseReportCount) {
    this.abuseReportCount = abuseReportCount;
  }

  public ForumPostUpdate abuseConfirmedBy(@Nullable UUID abuseConfirmedBy) {
    this.abuseConfirmedBy = abuseConfirmedBy;
    return this;
  }

  /**
   * Moderator who confirmed the report.
   * @return abuseConfirmedBy
   */
  @Valid 
  @JsonProperty("abuseConfirmedBy")
  public @Nullable UUID getAbuseConfirmedBy() {
    return abuseConfirmedBy;
  }

  @JsonProperty("abuseConfirmedBy")
  public void setAbuseConfirmedBy(@Nullable UUID abuseConfirmedBy) {
    this.abuseConfirmedBy = abuseConfirmedBy;
  }

  public ForumPostUpdate abuseConfirmedAt(@Nullable OffsetDateTime abuseConfirmedAt) {
    this.abuseConfirmedAt = abuseConfirmedAt;
    return this;
  }

  /**
   * When confirmed.
   * @return abuseConfirmedAt
   */
  @Valid 
  @JsonProperty("abuseConfirmedAt")
  public @Nullable OffsetDateTime getAbuseConfirmedAt() {
    return abuseConfirmedAt;
  }

  @JsonProperty("abuseConfirmedAt")
  public void setAbuseConfirmedAt(@Nullable OffsetDateTime abuseConfirmedAt) {
    this.abuseConfirmedAt = abuseConfirmedAt;
  }

  public ForumPostUpdate likeCount(@Nullable Integer likeCount) {
    this.likeCount = likeCount;
    return this;
  }

  /**
   * Likes (cached).
   * @return likeCount
   */
  
  @JsonProperty("likeCount")
  public @Nullable Integer getLikeCount() {
    return likeCount;
  }

  @JsonProperty("likeCount")
  public void setLikeCount(@Nullable Integer likeCount) {
    this.likeCount = likeCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForumPostUpdate forumPostUpdate = (ForumPostUpdate) o;
    return Objects.equals(this.parentPostId, forumPostUpdate.parentPostId) &&
        Objects.equals(this.title, forumPostUpdate.title) &&
        Objects.equals(this.body, forumPostUpdate.body) &&
        Objects.equals(this.authorUserId, forumPostUpdate.authorUserId) &&
        Objects.equals(this.postedAt, forumPostUpdate.postedAt) &&
        Objects.equals(this.isDraft, forumPostUpdate.isDraft) &&
        Objects.equals(this.isTeacherPost, forumPostUpdate.isTeacherPost) &&
        Objects.equals(this.substantiveFlag, forumPostUpdate.substantiveFlag) &&
        Objects.equals(this.isHidden, forumPostUpdate.isHidden) &&
        Objects.equals(this.isHiddenByParent, forumPostUpdate.isHiddenByParent) &&
        Objects.equals(this.isFlagged, forumPostUpdate.isFlagged) &&
        Objects.equals(this.showSignature, forumPostUpdate.showSignature) &&
        Objects.equals(this.usesInlineAuthoring, forumPostUpdate.usesInlineAuthoring) &&
        Objects.equals(this.isAbusive, forumPostUpdate.isAbusive) &&
        Objects.equals(this.abuseReported, forumPostUpdate.abuseReported) &&
        Objects.equals(this.abuseReportCount, forumPostUpdate.abuseReportCount) &&
        Objects.equals(this.abuseConfirmedBy, forumPostUpdate.abuseConfirmedBy) &&
        Objects.equals(this.abuseConfirmedAt, forumPostUpdate.abuseConfirmedAt) &&
        Objects.equals(this.likeCount, forumPostUpdate.likeCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentPostId, title, body, authorUserId, postedAt, isDraft, isTeacherPost, substantiveFlag, isHidden, isHiddenByParent, isFlagged, showSignature, usesInlineAuthoring, isAbusive, abuseReported, abuseReportCount, abuseConfirmedBy, abuseConfirmedAt, likeCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumPostUpdate {\n");
    sb.append("    parentPostId: ").append(toIndentedString(parentPostId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    authorUserId: ").append(toIndentedString(authorUserId)).append("\n");
    sb.append("    postedAt: ").append(toIndentedString(postedAt)).append("\n");
    sb.append("    isDraft: ").append(toIndentedString(isDraft)).append("\n");
    sb.append("    isTeacherPost: ").append(toIndentedString(isTeacherPost)).append("\n");
    sb.append("    substantiveFlag: ").append(toIndentedString(substantiveFlag)).append("\n");
    sb.append("    isHidden: ").append(toIndentedString(isHidden)).append("\n");
    sb.append("    isHiddenByParent: ").append(toIndentedString(isHiddenByParent)).append("\n");
    sb.append("    isFlagged: ").append(toIndentedString(isFlagged)).append("\n");
    sb.append("    showSignature: ").append(toIndentedString(showSignature)).append("\n");
    sb.append("    usesInlineAuthoring: ").append(toIndentedString(usesInlineAuthoring)).append("\n");
    sb.append("    isAbusive: ").append(toIndentedString(isAbusive)).append("\n");
    sb.append("    abuseReported: ").append(toIndentedString(abuseReported)).append("\n");
    sb.append("    abuseReportCount: ").append(toIndentedString(abuseReportCount)).append("\n");
    sb.append("    abuseConfirmedBy: ").append(toIndentedString(abuseConfirmedBy)).append("\n");
    sb.append("    abuseConfirmedAt: ").append(toIndentedString(abuseConfirmedAt)).append("\n");
    sb.append("    likeCount: ").append(toIndentedString(likeCount)).append("\n");
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

