// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * ProgressBucket
 */

public class ProgressBucket {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String rangeLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer students;

  public ProgressBucket() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProgressBucket(String rangeLabel, Integer students) {
    this.rangeLabel = rangeLabel;
    this.students = students;
  }

  public ProgressBucket rangeLabel(String rangeLabel) {
    this.rangeLabel = rangeLabel;
    return this;
  }

  /**
   * e.g. 0-25%.
   * @return rangeLabel
   */
  @NotNull 
  @JsonProperty("rangeLabel")
  public String getRangeLabel() {
    return rangeLabel;
  }

  @JsonProperty("rangeLabel")
  public void setRangeLabel(String rangeLabel) {
    this.rangeLabel = rangeLabel;
  }

  public ProgressBucket students(Integer students) {
    this.students = students;
    return this;
  }

  /**
   * Students.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgressBucket progressBucket = (ProgressBucket) o;
    return Objects.equals(this.rangeLabel, progressBucket.rangeLabel) &&
        Objects.equals(this.students, progressBucket.students);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rangeLabel, students);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgressBucket {\n");
    sb.append("    rangeLabel: ").append(toIndentedString(rangeLabel)).append("\n");
    sb.append("    students: ").append(toIndentedString(students)).append("\n");
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

