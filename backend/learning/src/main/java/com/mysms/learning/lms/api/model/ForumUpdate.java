// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of Forum that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ForumUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> earlyAccessFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> opensAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> closesAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> lateAccessUntil;

  public ForumUpdate title(@Nullable Optional<String> title) {
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

  public ForumUpdate description(@Nullable Optional<String> description) {
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

  public ForumUpdate earlyAccessFrom(@Nullable Optional<OffsetDateTime> earlyAccessFrom) {
    this.earlyAccessFrom = earlyAccessFrom;
    return this;
  }

  /**
   * Teachers' early access.
   * @return earlyAccessFrom
   */
  
  @JsonProperty("earlyAccessFrom")
  public @Nullable Optional<OffsetDateTime> getEarlyAccessFrom() {
    return earlyAccessFrom;
  }

  @JsonProperty("earlyAccessFrom")
  public void setEarlyAccessFrom(@Nullable Optional<OffsetDateTime> earlyAccessFrom) {
    this.earlyAccessFrom = earlyAccessFrom;
  }

  public ForumUpdate opensAt(@Nullable Optional<OffsetDateTime> opensAt) {
    this.opensAt = opensAt;
    return this;
  }

  /**
   * Opens for students.
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

  public ForumUpdate closesAt(@Nullable Optional<OffsetDateTime> closesAt) {
    this.closesAt = closesAt;
    return this;
  }

  /**
   * Closes for students.
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

  public ForumUpdate lateAccessUntil(@Nullable Optional<OffsetDateTime> lateAccessUntil) {
    this.lateAccessUntil = lateAccessUntil;
    return this;
  }

  /**
   * Read-only access until.
   * @return lateAccessUntil
   */
  
  @JsonProperty("lateAccessUntil")
  public @Nullable Optional<OffsetDateTime> getLateAccessUntil() {
    return lateAccessUntil;
  }

  @JsonProperty("lateAccessUntil")
  public void setLateAccessUntil(@Nullable Optional<OffsetDateTime> lateAccessUntil) {
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

