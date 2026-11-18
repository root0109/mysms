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
 * Public-safe card of a member — only what the member chose to show on their public profile. Sourced from the people module&#39;s published profile; never the full person record.
 */

public class MemberCard {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID photoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String headline;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String about;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime memberSince;

  public MemberCard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MemberCard(UUID userId, String displayName) {
    this.userId = userId;
    this.displayName = displayName;
  }

  public MemberCard userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User account of the member.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public MemberCard displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Display name as published.
   * @return displayName
   */
  @NotNull 
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public MemberCard photoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
    return this;
  }

  /**
   * Published photo.
   * @return photoFileId
   */
  @Valid 
  @JsonProperty("photoFileId")
  public @Nullable UUID getPhotoFileId() {
    return photoFileId;
  }

  @JsonProperty("photoFileId")
  public void setPhotoFileId(@Nullable UUID photoFileId) {
    this.photoFileId = photoFileId;
  }

  public MemberCard headline(@Nullable String headline) {
    this.headline = headline;
    return this;
  }

  /**
   * Published headline.
   * @return headline
   */
  
  @JsonProperty("headline")
  public @Nullable String getHeadline() {
    return headline;
  }

  @JsonProperty("headline")
  public void setHeadline(@Nullable String headline) {
    this.headline = headline;
  }

  public MemberCard about(@Nullable String about) {
    this.about = about;
    return this;
  }

  /**
   * Published about text.
   * @return about
   */
  
  @JsonProperty("about")
  public @Nullable String getAbout() {
    return about;
  }

  @JsonProperty("about")
  public void setAbout(@Nullable String about) {
    this.about = about;
  }

  public MemberCard memberSince(@Nullable OffsetDateTime memberSince) {
    this.memberSince = memberSince;
    return this;
  }

  /**
   * Member since.
   * @return memberSince
   */
  @Valid 
  @JsonProperty("memberSince")
  public @Nullable OffsetDateTime getMemberSince() {
    return memberSince;
  }

  @JsonProperty("memberSince")
  public void setMemberSince(@Nullable OffsetDateTime memberSince) {
    this.memberSince = memberSince;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MemberCard memberCard = (MemberCard) o;
    return Objects.equals(this.userId, memberCard.userId) &&
        Objects.equals(this.displayName, memberCard.displayName) &&
        Objects.equals(this.photoFileId, memberCard.photoFileId) &&
        Objects.equals(this.headline, memberCard.headline) &&
        Objects.equals(this.about, memberCard.about) &&
        Objects.equals(this.memberSince, memberCard.memberSince);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, displayName, photoFileId, headline, about, memberSince);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MemberCard {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    photoFileId: ").append(toIndentedString(photoFileId)).append("\n");
    sb.append("    headline: ").append(toIndentedString(headline)).append("\n");
    sb.append("    about: ").append(toIndentedString(about)).append("\n");
    sb.append("    memberSince: ").append(toIndentedString(memberSince)).append("\n");
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

