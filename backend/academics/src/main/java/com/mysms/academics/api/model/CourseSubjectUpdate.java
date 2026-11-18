// Owned API contract of academics.
package com.mysms.academics.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CourseSubject that can be updated (partial update; see updateMask).
 */

public class CourseSubjectUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer termNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String credits;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subjectCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectId;

  public CourseSubjectUpdate termNumber(@Nullable Integer termNumber) {
    this.termNumber = termNumber;
    return this;
  }

  /**
   * Semester/term the subject is taught in.
   * @return termNumber
   */
  
  @JsonProperty("termNumber")
  public @Nullable Integer getTermNumber() {
    return termNumber;
  }

  @JsonProperty("termNumber")
  public void setTermNumber(@Nullable Integer termNumber) {
    this.termNumber = termNumber;
  }

  public CourseSubjectUpdate credits(@Nullable String credits) {
    this.credits = credits;
    return this;
  }

  /**
   * Credit value.
   * @return credits
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("credits")
  public @Nullable String getCredits() {
    return credits;
  }

  @JsonProperty("credits")
  public void setCredits(@Nullable String credits) {
    this.credits = credits;
  }

  public CourseSubjectUpdate subjectCategory(@Nullable String subjectCategory) {
    this.subjectCategory = subjectCategory;
    return this;
  }

  /**
   * Category in the plan.
   * @return subjectCategory
   */
  @Size(max = 30) 
  @JsonProperty("subjectCategory")
  public @Nullable String getSubjectCategory() {
    return subjectCategory;
  }

  @JsonProperty("subjectCategory")
  public void setSubjectCategory(@Nullable String subjectCategory) {
    this.subjectCategory = subjectCategory;
  }

  public CourseSubjectUpdate maxMarks(@Nullable String maxMarks) {
    this.maxMarks = maxMarks;
    return this;
  }

  /**
   * Maximum marks.
   * @return maxMarks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("maxMarks")
  public @Nullable String getMaxMarks() {
    return maxMarks;
  }

  @JsonProperty("maxMarks")
  public void setMaxMarks(@Nullable String maxMarks) {
    this.maxMarks = maxMarks;
  }

  public CourseSubjectUpdate subjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  @Valid 
  @JsonProperty("subjectId")
  public @Nullable UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseSubjectUpdate courseSubjectUpdate = (CourseSubjectUpdate) o;
    return Objects.equals(this.termNumber, courseSubjectUpdate.termNumber) &&
        Objects.equals(this.credits, courseSubjectUpdate.credits) &&
        Objects.equals(this.subjectCategory, courseSubjectUpdate.subjectCategory) &&
        Objects.equals(this.maxMarks, courseSubjectUpdate.maxMarks) &&
        Objects.equals(this.subjectId, courseSubjectUpdate.subjectId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(termNumber, credits, subjectCategory, maxMarks, subjectId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseSubjectUpdate {\n");
    sb.append("    termNumber: ").append(toIndentedString(termNumber)).append("\n");
    sb.append("    credits: ").append(toIndentedString(credits)).append("\n");
    sb.append("    subjectCategory: ").append(toIndentedString(subjectCategory)).append("\n");
    sb.append("    maxMarks: ").append(toIndentedString(maxMarks)).append("\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
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

