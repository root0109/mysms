// Owned API contract of lms.
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
 * LmsStatistics
 */

public class LmsStatistics {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer publishedCourses;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer students;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer enrollments;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer certificatesIssued;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String completionPercent;

  public LmsStatistics() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LmsStatistics(Integer publishedCourses, Integer students, Integer enrollments, Integer certificatesIssued, String completionPercent) {
    this.publishedCourses = publishedCourses;
    this.students = students;
    this.enrollments = enrollments;
    this.certificatesIssued = certificatesIssued;
    this.completionPercent = completionPercent;
  }

  public LmsStatistics publishedCourses(Integer publishedCourses) {
    this.publishedCourses = publishedCourses;
    return this;
  }

  /**
   * Published courses.
   * @return publishedCourses
   */
  @NotNull 
  @JsonProperty("publishedCourses")
  public Integer getPublishedCourses() {
    return publishedCourses;
  }

  @JsonProperty("publishedCourses")
  public void setPublishedCourses(Integer publishedCourses) {
    this.publishedCourses = publishedCourses;
  }

  public LmsStatistics students(Integer students) {
    this.students = students;
    return this;
  }

  /**
   * Students with an enrolment.
   * @return students
   */
  @NotNull 
  @JsonProperty("students")
  public Integer getStudents() {
    return students;
  }

  @JsonProperty("students")
  public void setStudents(Integer students) {
    this.students = students;
  }

  public LmsStatistics enrollments(Integer enrollments) {
    this.enrollments = enrollments;
    return this;
  }

  /**
   * Enrolments.
   * @return enrollments
   */
  @NotNull 
  @JsonProperty("enrollments")
  public Integer getEnrollments() {
    return enrollments;
  }

  @JsonProperty("enrollments")
  public void setEnrollments(Integer enrollments) {
    this.enrollments = enrollments;
  }

  public LmsStatistics certificatesIssued(Integer certificatesIssued) {
    this.certificatesIssued = certificatesIssued;
    return this;
  }

  /**
   * Published certificates.
   * @return certificatesIssued
   */
  @NotNull 
  @JsonProperty("certificatesIssued")
  public Integer getCertificatesIssued() {
    return certificatesIssued;
  }

  @JsonProperty("certificatesIssued")
  public void setCertificatesIssued(Integer certificatesIssued) {
    this.certificatesIssued = certificatesIssued;
  }

  public LmsStatistics completionPercent(String completionPercent) {
    this.completionPercent = completionPercent;
    return this;
  }

  /**
   * Share of enrolments at 100%.
   * @return completionPercent
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("completionPercent")
  public String getCompletionPercent() {
    return completionPercent;
  }

  @JsonProperty("completionPercent")
  public void setCompletionPercent(String completionPercent) {
    this.completionPercent = completionPercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LmsStatistics lmsStatistics = (LmsStatistics) o;
    return Objects.equals(this.publishedCourses, lmsStatistics.publishedCourses) &&
        Objects.equals(this.students, lmsStatistics.students) &&
        Objects.equals(this.enrollments, lmsStatistics.enrollments) &&
        Objects.equals(this.certificatesIssued, lmsStatistics.certificatesIssued) &&
        Objects.equals(this.completionPercent, lmsStatistics.completionPercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(publishedCourses, students, enrollments, certificatesIssued, completionPercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LmsStatistics {\n");
    sb.append("    publishedCourses: ").append(toIndentedString(publishedCourses)).append("\n");
    sb.append("    students: ").append(toIndentedString(students)).append("\n");
    sb.append("    enrollments: ").append(toIndentedString(enrollments)).append("\n");
    sb.append("    certificatesIssued: ").append(toIndentedString(certificatesIssued)).append("\n");
    sb.append("    completionPercent: ").append(toIndentedString(completionPercent)).append("\n");
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

