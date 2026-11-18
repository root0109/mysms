// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.CourseProgress;
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
 * CohortStudentProgress
 */

public class CohortStudentProgress {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CourseProgress> courseProgress = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer assessmentsCompleted;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer assessmentsTotal;

  public CohortStudentProgress() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CohortStudentProgress(UUID userId, Integer assessmentsCompleted, Integer assessmentsTotal) {
    this.userId = userId;
    this.assessmentsCompleted = assessmentsCompleted;
    this.assessmentsTotal = assessmentsTotal;
  }

  public CohortStudentProgress userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
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

  public CohortStudentProgress courseProgress(List<@Valid CourseProgress> courseProgress) {
    this.courseProgress = courseProgress;
    return this;
  }

  public CohortStudentProgress addCourseProgressItem(CourseProgress courseProgressItem) {
    if (this.courseProgress == null) {
      this.courseProgress = new ArrayList<>();
    }
    this.courseProgress.add(courseProgressItem);
    return this;
  }

  /**
   * Per course.
   * @return courseProgress
   */
  
  @JsonProperty("courseProgress")
  public List<@Valid CourseProgress> getCourseProgress() {
    return courseProgress;
  }

  @JsonProperty("courseProgress")
  public void setCourseProgress(List<@Valid CourseProgress> courseProgress) {
    this.courseProgress = courseProgress;
  }

  public CohortStudentProgress assessmentsCompleted(Integer assessmentsCompleted) {
    this.assessmentsCompleted = assessmentsCompleted;
    return this;
  }

  /**
   * Completed.
   * @return assessmentsCompleted
   */
  @NotNull 
  @JsonProperty("assessmentsCompleted")
  public Integer getAssessmentsCompleted() {
    return assessmentsCompleted;
  }

  @JsonProperty("assessmentsCompleted")
  public void setAssessmentsCompleted(Integer assessmentsCompleted) {
    this.assessmentsCompleted = assessmentsCompleted;
  }

  public CohortStudentProgress assessmentsTotal(Integer assessmentsTotal) {
    this.assessmentsTotal = assessmentsTotal;
    return this;
  }

  /**
   * Total.
   * @return assessmentsTotal
   */
  @NotNull 
  @JsonProperty("assessmentsTotal")
  public Integer getAssessmentsTotal() {
    return assessmentsTotal;
  }

  @JsonProperty("assessmentsTotal")
  public void setAssessmentsTotal(Integer assessmentsTotal) {
    this.assessmentsTotal = assessmentsTotal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortStudentProgress cohortStudentProgress = (CohortStudentProgress) o;
    return Objects.equals(this.userId, cohortStudentProgress.userId) &&
        Objects.equals(this.courseProgress, cohortStudentProgress.courseProgress) &&
        Objects.equals(this.assessmentsCompleted, cohortStudentProgress.assessmentsCompleted) &&
        Objects.equals(this.assessmentsTotal, cohortStudentProgress.assessmentsTotal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, courseProgress, assessmentsCompleted, assessmentsTotal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortStudentProgress {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    courseProgress: ").append(toIndentedString(courseProgress)).append("\n");
    sb.append("    assessmentsCompleted: ").append(toIndentedString(assessmentsCompleted)).append("\n");
    sb.append("    assessmentsTotal: ").append(toIndentedString(assessmentsTotal)).append("\n");
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

