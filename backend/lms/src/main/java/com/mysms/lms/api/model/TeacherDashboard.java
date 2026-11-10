// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.CalendarItem;
import com.mysms.lms.api.model.Cohort;
import com.mysms.lms.api.model.StudentAlert;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * TeacherDashboard
 */

public class TeacherDashboard {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Cohort> cohorts = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer toGrade;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer unreadPosts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CalendarItem> upcomingDue = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CalendarItem> upcomingSessions = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid StudentAlert> alerts = new ArrayList<>();

  public TeacherDashboard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TeacherDashboard(Integer toGrade, Integer unreadPosts) {
    this.toGrade = toGrade;
    this.unreadPosts = unreadPosts;
  }

  public TeacherDashboard cohorts(List<@Valid Cohort> cohorts) {
    this.cohorts = cohorts;
    return this;
  }

  public TeacherDashboard addCohortsItem(Cohort cohortsItem) {
    if (this.cohorts == null) {
      this.cohorts = new ArrayList<>();
    }
    this.cohorts.add(cohortsItem);
    return this;
  }

  /**
   * My cohorts.
   * @return cohorts
   */
  
  @JsonProperty("cohorts")
  public List<@Valid Cohort> getCohorts() {
    return cohorts;
  }

  @JsonProperty("cohorts")
  public void setCohorts(List<@Valid Cohort> cohorts) {
    this.cohorts = cohorts;
  }

  public TeacherDashboard toGrade(Integer toGrade) {
    this.toGrade = toGrade;
    return this;
  }

  /**
   * Submissions waiting for grading.
   * @return toGrade
   */
  @NotNull 
  @JsonProperty("toGrade")
  public Integer getToGrade() {
    return toGrade;
  }

  @JsonProperty("toGrade")
  public void setToGrade(Integer toGrade) {
    this.toGrade = toGrade;
  }

  public TeacherDashboard unreadPosts(Integer unreadPosts) {
    this.unreadPosts = unreadPosts;
    return this;
  }

  /**
   * Unread discussion posts.
   * @return unreadPosts
   */
  @NotNull 
  @JsonProperty("unreadPosts")
  public Integer getUnreadPosts() {
    return unreadPosts;
  }

  @JsonProperty("unreadPosts")
  public void setUnreadPosts(Integer unreadPosts) {
    this.unreadPosts = unreadPosts;
  }

  public TeacherDashboard upcomingDue(List<@Valid CalendarItem> upcomingDue) {
    this.upcomingDue = upcomingDue;
    return this;
  }

  public TeacherDashboard addUpcomingDueItem(CalendarItem upcomingDueItem) {
    if (this.upcomingDue == null) {
      this.upcomingDue = new ArrayList<>();
    }
    this.upcomingDue.add(upcomingDueItem);
    return this;
  }

  /**
   * Items due soon.
   * @return upcomingDue
   */
  
  @JsonProperty("upcomingDue")
  public List<@Valid CalendarItem> getUpcomingDue() {
    return upcomingDue;
  }

  @JsonProperty("upcomingDue")
  public void setUpcomingDue(List<@Valid CalendarItem> upcomingDue) {
    this.upcomingDue = upcomingDue;
  }

  public TeacherDashboard upcomingSessions(List<@Valid CalendarItem> upcomingSessions) {
    this.upcomingSessions = upcomingSessions;
    return this;
  }

  public TeacherDashboard addUpcomingSessionsItem(CalendarItem upcomingSessionsItem) {
    if (this.upcomingSessions == null) {
      this.upcomingSessions = new ArrayList<>();
    }
    this.upcomingSessions.add(upcomingSessionsItem);
    return this;
  }

  /**
   * Upcoming sessions and live classes.
   * @return upcomingSessions
   */
  
  @JsonProperty("upcomingSessions")
  public List<@Valid CalendarItem> getUpcomingSessions() {
    return upcomingSessions;
  }

  @JsonProperty("upcomingSessions")
  public void setUpcomingSessions(List<@Valid CalendarItem> upcomingSessions) {
    this.upcomingSessions = upcomingSessions;
  }

  public TeacherDashboard alerts(List<@Valid StudentAlert> alerts) {
    this.alerts = alerts;
    return this;
  }

  public TeacherDashboard addAlertsItem(StudentAlert alertsItem) {
    if (this.alerts == null) {
      this.alerts = new ArrayList<>();
    }
    this.alerts.add(alertsItem);
    return this;
  }

  /**
   * Recent at-risk alerts.
   * @return alerts
   */
  
  @JsonProperty("alerts")
  public List<@Valid StudentAlert> getAlerts() {
    return alerts;
  }

  @JsonProperty("alerts")
  public void setAlerts(List<@Valid StudentAlert> alerts) {
    this.alerts = alerts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TeacherDashboard teacherDashboard = (TeacherDashboard) o;
    return Objects.equals(this.cohorts, teacherDashboard.cohorts) &&
        Objects.equals(this.toGrade, teacherDashboard.toGrade) &&
        Objects.equals(this.unreadPosts, teacherDashboard.unreadPosts) &&
        Objects.equals(this.upcomingDue, teacherDashboard.upcomingDue) &&
        Objects.equals(this.upcomingSessions, teacherDashboard.upcomingSessions) &&
        Objects.equals(this.alerts, teacherDashboard.alerts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohorts, toGrade, unreadPosts, upcomingDue, upcomingSessions, alerts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TeacherDashboard {\n");
    sb.append("    cohorts: ").append(toIndentedString(cohorts)).append("\n");
    sb.append("    toGrade: ").append(toIndentedString(toGrade)).append("\n");
    sb.append("    unreadPosts: ").append(toIndentedString(unreadPosts)).append("\n");
    sb.append("    upcomingDue: ").append(toIndentedString(upcomingDue)).append("\n");
    sb.append("    upcomingSessions: ").append(toIndentedString(upcomingSessions)).append("\n");
    sb.append("    alerts: ").append(toIndentedString(alerts)).append("\n");
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

