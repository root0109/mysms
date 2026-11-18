// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.CohortEnrollment;
import com.mysms.lms.api.model.DailyCount;
import com.mysms.lms.api.model.Enrollment;
import com.mysms.lms.api.model.LiveClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * StudentDashboard
 */

public class StudentDashboard {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid Enrollment> enrollments = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid CohortEnrollment> cohortEnrollments = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID continueLessonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid LiveClass> upcomingLiveClasses = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer streakDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer longestStreakDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid DailyCount> activityHeatmap = new ArrayList<>();

  public StudentDashboard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StudentDashboard(Integer streakDays, Integer longestStreakDays) {
    this.streakDays = streakDays;
    this.longestStreakDays = longestStreakDays;
  }

  public StudentDashboard enrollments(List<@Valid Enrollment> enrollments) {
    this.enrollments = enrollments;
    return this;
  }

  public StudentDashboard addEnrollmentsItem(Enrollment enrollmentsItem) {
    if (this.enrollments == null) {
      this.enrollments = new ArrayList<>();
    }
    this.enrollments.add(enrollmentsItem);
    return this;
  }

  /**
   * My course enrolments.
   * @return enrollments
   */
  
  @JsonProperty("enrollments")
  public List<@Valid Enrollment> getEnrollments() {
    return enrollments;
  }

  @JsonProperty("enrollments")
  public void setEnrollments(List<@Valid Enrollment> enrollments) {
    this.enrollments = enrollments;
  }

  public StudentDashboard cohortEnrollments(List<@Valid CohortEnrollment> cohortEnrollments) {
    this.cohortEnrollments = cohortEnrollments;
    return this;
  }

  public StudentDashboard addCohortEnrollmentsItem(CohortEnrollment cohortEnrollmentsItem) {
    if (this.cohortEnrollments == null) {
      this.cohortEnrollments = new ArrayList<>();
    }
    this.cohortEnrollments.add(cohortEnrollmentsItem);
    return this;
  }

  /**
   * My cohorts.
   * @return cohortEnrollments
   */
  
  @JsonProperty("cohortEnrollments")
  public List<@Valid CohortEnrollment> getCohortEnrollments() {
    return cohortEnrollments;
  }

  @JsonProperty("cohortEnrollments")
  public void setCohortEnrollments(List<@Valid CohortEnrollment> cohortEnrollments) {
    this.cohortEnrollments = cohortEnrollments;
  }

  public StudentDashboard continueLessonId(@Nullable UUID continueLessonId) {
    this.continueLessonId = continueLessonId;
    return this;
  }

  /**
   * Lesson to resume.
   * @return continueLessonId
   */
  @Valid 
  @JsonProperty("continueLessonId")
  public @Nullable UUID getContinueLessonId() {
    return continueLessonId;
  }

  @JsonProperty("continueLessonId")
  public void setContinueLessonId(@Nullable UUID continueLessonId) {
    this.continueLessonId = continueLessonId;
  }

  public StudentDashboard upcomingLiveClasses(List<@Valid LiveClass> upcomingLiveClasses) {
    this.upcomingLiveClasses = upcomingLiveClasses;
    return this;
  }

  public StudentDashboard addUpcomingLiveClassesItem(LiveClass upcomingLiveClassesItem) {
    if (this.upcomingLiveClasses == null) {
      this.upcomingLiveClasses = new ArrayList<>();
    }
    this.upcomingLiveClasses.add(upcomingLiveClassesItem);
    return this;
  }

  /**
   * Upcoming live classes.
   * @return upcomingLiveClasses
   */
  
  @JsonProperty("upcomingLiveClasses")
  public List<@Valid LiveClass> getUpcomingLiveClasses() {
    return upcomingLiveClasses;
  }

  @JsonProperty("upcomingLiveClasses")
  public void setUpcomingLiveClasses(List<@Valid LiveClass> upcomingLiveClasses) {
    this.upcomingLiveClasses = upcomingLiveClasses;
  }

  public StudentDashboard streakDays(Integer streakDays) {
    this.streakDays = streakDays;
    return this;
  }

  /**
   * Current streak.
   * @return streakDays
   */
  @NotNull 
  @JsonProperty("streakDays")
  public Integer getStreakDays() {
    return streakDays;
  }

  @JsonProperty("streakDays")
  public void setStreakDays(Integer streakDays) {
    this.streakDays = streakDays;
  }

  public StudentDashboard longestStreakDays(Integer longestStreakDays) {
    this.longestStreakDays = longestStreakDays;
    return this;
  }

  /**
   * Longest streak.
   * @return longestStreakDays
   */
  @NotNull 
  @JsonProperty("longestStreakDays")
  public Integer getLongestStreakDays() {
    return longestStreakDays;
  }

  @JsonProperty("longestStreakDays")
  public void setLongestStreakDays(Integer longestStreakDays) {
    this.longestStreakDays = longestStreakDays;
  }

  public StudentDashboard activityHeatmap(List<@Valid DailyCount> activityHeatmap) {
    this.activityHeatmap = activityHeatmap;
    return this;
  }

  public StudentDashboard addActivityHeatmapItem(DailyCount activityHeatmapItem) {
    if (this.activityHeatmap == null) {
      this.activityHeatmap = new ArrayList<>();
    }
    this.activityHeatmap.add(activityHeatmapItem);
    return this;
  }

  /**
   * Activity per day.
   * @return activityHeatmap
   */
  
  @JsonProperty("activityHeatmap")
  public List<@Valid DailyCount> getActivityHeatmap() {
    return activityHeatmap;
  }

  @JsonProperty("activityHeatmap")
  public void setActivityHeatmap(List<@Valid DailyCount> activityHeatmap) {
    this.activityHeatmap = activityHeatmap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentDashboard studentDashboard = (StudentDashboard) o;
    return Objects.equals(this.enrollments, studentDashboard.enrollments) &&
        Objects.equals(this.cohortEnrollments, studentDashboard.cohortEnrollments) &&
        Objects.equals(this.continueLessonId, studentDashboard.continueLessonId) &&
        Objects.equals(this.upcomingLiveClasses, studentDashboard.upcomingLiveClasses) &&
        Objects.equals(this.streakDays, studentDashboard.streakDays) &&
        Objects.equals(this.longestStreakDays, studentDashboard.longestStreakDays) &&
        Objects.equals(this.activityHeatmap, studentDashboard.activityHeatmap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enrollments, cohortEnrollments, continueLessonId, upcomingLiveClasses, streakDays, longestStreakDays, activityHeatmap);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentDashboard {\n");
    sb.append("    enrollments: ").append(toIndentedString(enrollments)).append("\n");
    sb.append("    cohortEnrollments: ").append(toIndentedString(cohortEnrollments)).append("\n");
    sb.append("    continueLessonId: ").append(toIndentedString(continueLessonId)).append("\n");
    sb.append("    upcomingLiveClasses: ").append(toIndentedString(upcomingLiveClasses)).append("\n");
    sb.append("    streakDays: ").append(toIndentedString(streakDays)).append("\n");
    sb.append("    longestStreakDays: ").append(toIndentedString(longestStreakDays)).append("\n");
    sb.append("    activityHeatmap: ").append(toIndentedString(activityHeatmap)).append("\n");
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

