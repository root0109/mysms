// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Forum that can be updated (partial update; see updateMask).
 */

public class ForumUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime earlyAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime opensAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime closesAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lateAccessUntil;

  public ForumUpdate title(@Nullable String title) {
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

  public ForumUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
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

  public ForumUpdate earlyAccessFrom(@Nullable OffsetDateTime earlyAccessFrom) {
    this.earlyAccessFrom = earlyAccessFrom;
    return this;
  }

  /**
   * Teachers' early access.
   * @return earlyAccessFrom
   */
  @Valid 
  @JsonProperty("earlyAccessFrom")
  public @Nullable OffsetDateTime getEarlyAccessFrom() {
    return earlyAccessFrom;
  }

  @JsonProperty("earlyAccessFrom")
  public void setEarlyAccessFrom(@Nullable OffsetDateTime earlyAccessFrom) {
    this.earlyAccessFrom = earlyAccessFrom;
  }

  public ForumUpdate opensAt(@Nullable OffsetDateTime opensAt) {
    this.opensAt = opensAt;
    return this;
  }

  /**
   * Opens for students.
   * @return opensAt
   */
  @Valid 
  @JsonProperty("opensAt")
  public @Nullable OffsetDateTime getOpensAt() {
    return opensAt;
  }

  @JsonProperty("opensAt")
  public void setOpensAt(@Nullable OffsetDateTime opensAt) {
    this.opensAt = opensAt;
  }

  public ForumUpdate closesAt(@Nullable OffsetDateTime closesAt) {
    this.closesAt = closesAt;
    return this;
  }

  /**
   * Closes for students.
   * @return closesAt
   */
  @Valid 
  @JsonProperty("closesAt")
  public @Nullable OffsetDateTime getClosesAt() {
    return closesAt;
  }

  @JsonProperty("closesAt")
  public void setClosesAt(@Nullable OffsetDateTime closesAt) {
    this.closesAt = closesAt;
  }

  public ForumUpdate lateAccessUntil(@Nullable OffsetDateTime lateAccessUntil) {
    this.lateAccessUntil = lateAccessUntil;
    return this;
  }

  /**
   * Read-only access until.
   * @return lateAccessUntil
   */
  @Valid 
  @JsonProperty("lateAccessUntil")
  public @Nullable OffsetDateTime getLateAccessUntil() {
    return lateAccessUntil;
  }

  @JsonProperty("lateAccessUntil")
  public void setLateAccessUntil(@Nullable OffsetDateTime lateAccessUntil) {
    this.lateAccessUntil = lateAccessUntil;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForumUpdate forumUpdate = (ForumUpdate) o;
    return Objects.equals(this.title, forumUpdate.title) &&
        Objects.equals(this.description, forumUpdate.description) &&
        Objects.equals(this.earlyAccessFrom, forumUpdate.earlyAccessFrom) &&
        Objects.equals(this.opensAt, forumUpdate.opensAt) &&
        Objects.equals(this.closesAt, forumUpdate.closesAt) &&
        Objects.equals(this.lateAccessUntil, forumUpdate.lateAccessUntil);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description, earlyAccessFrom, opensAt, closesAt, lateAccessUntil);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForumUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    earlyAccessFrom: ").append(toIndentedString(earlyAccessFrom)).append("\n");
    sb.append("    opensAt: ").append(toIndentedString(opensAt)).append("\n");
    sb.append("    closesAt: ").append(toIndentedString(closesAt)).append("\n");
    sb.append("    lateAccessUntil: ").append(toIndentedString(lateAccessUntil)).append("\n");
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

