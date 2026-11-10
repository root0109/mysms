// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * UnreadCounts
 */

public class UnreadCounts {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer forumPosts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer notifications;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer mailbox;

  private @Nullable Object perCohort = null;

  public UnreadCounts() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UnreadCounts(Integer forumPosts, Integer notifications, Integer mailbox) {
    this.forumPosts = forumPosts;
    this.notifications = notifications;
    this.mailbox = mailbox;
  }

  public UnreadCounts forumPosts(Integer forumPosts) {
    this.forumPosts = forumPosts;
    return this;
  }

  /**
   * Unread discussion posts.
   * @return forumPosts
   */
  @NotNull 
  @JsonProperty("forumPosts")
  public Integer getForumPosts() {
    return forumPosts;
  }

  @JsonProperty("forumPosts")
  public void setForumPosts(Integer forumPosts) {
    this.forumPosts = forumPosts;
  }

  public UnreadCounts notifications(Integer notifications) {
    this.notifications = notifications;
    return this;
  }

  /**
   * Unread notifications.
   * @return notifications
   */
  @NotNull 
  @JsonProperty("notifications")
  public Integer getNotifications() {
    return notifications;
  }

  @JsonProperty("notifications")
  public void setNotifications(Integer notifications) {
    this.notifications = notifications;
  }

  public UnreadCounts mailbox(Integer mailbox) {
    this.mailbox = mailbox;
    return this;
  }

  /**
   * Unread internal mail.
   * @return mailbox
   */
  @NotNull 
  @JsonProperty("mailbox")
  public Integer getMailbox() {
    return mailbox;
  }

  @JsonProperty("mailbox")
  public void setMailbox(Integer mailbox) {
    this.mailbox = mailbox;
  }

  public UnreadCounts perCohort(@Nullable Object perCohort) {
    this.perCohort = perCohort;
    return this;
  }

  /**
   * Unread posts per cohort id.
   * @return perCohort
   */
  
  @JsonProperty("perCohort")
  public @Nullable Object getPerCohort() {
    return perCohort;
  }

  @JsonProperty("perCohort")
  public void setPerCohort(@Nullable Object perCohort) {
    this.perCohort = perCohort;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UnreadCounts unreadCounts = (UnreadCounts) o;
    return Objects.equals(this.forumPosts, unreadCounts.forumPosts) &&
        Objects.equals(this.notifications, unreadCounts.notifications) &&
        Objects.equals(this.mailbox, unreadCounts.mailbox) &&
        Objects.equals(this.perCohort, unreadCounts.perCohort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forumPosts, notifications, mailbox, perCohort);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UnreadCounts {\n");
    sb.append("    forumPosts: ").append(toIndentedString(forumPosts)).append("\n");
    sb.append("    notifications: ").append(toIndentedString(notifications)).append("\n");
    sb.append("    mailbox: ").append(toIndentedString(mailbox)).append("\n");
    sb.append("    perCohort: ").append(toIndentedString(perCohort)).append("\n");
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

