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
 * Fields of CourseSubject that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CourseSubjectUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> termNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> credits;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> subjectCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> maxMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> subjectId;

  public CourseSubjectUpdate termNumber(@Nullable Optional<Integer> termNumber) {
    this.termNumber = termNumber;
    return this;
  }

  /**
   * Semester/term the subject is taught in.
   * @return termNumber
   */
  
  @JsonProperty("termNumber")
  public @Nullable Optional<Integer> getTermNumber() {
    return termNumber;
  }

  @JsonProperty("termNumber")
  public void setTermNumber(@Nullable Optional<Integer> termNumber) {
    this.termNumber = termNumber;
  }

  public CourseSubjectUpdate credits(@Nullable Optional<String> credits) {
    this.credits = credits;
    return this;
  }

  /**
   * Credit value.
   * @return credits
   */
  
  @JsonProperty("credits")
  public @Nullable Optional<String> getCredits() {
    return credits;
  }

  @JsonProperty("credits")
  public void setCredits(@Nullable Optional<String> credits) {
    this.credits = credits;
  }

  public CourseSubjectUpdate subjectCategory(@Nullable Optional<String> subjectCategory) {
    this.subjectCategory = subjectCategory;
    return this;
  }

  /**
   * Category in the plan.
   * @return subjectCategory
   */
  
  @JsonProperty("subjectCategory")
  public @Nullable Optional<String> getSubjectCategory() {
    return subjectCategory;
  }

  @JsonProperty("subjectCategory")
  public void setSubjectCategory(@Nullable Optional<String> subjectCategory) {
    this.subjectCategory = subjectCategory;
  }

  public CourseSubjectUpdate maxMarks(@Nullable Optional<String> maxMarks) {
    this.maxMarks = maxMarks;
    return this;
  }

  /**
   * Maximum marks.
   * @return maxMarks
   */
  
  @JsonProperty("maxMarks")
  public @Nullable Optional<String> getMaxMarks() {
    return maxMarks;
  }

  @JsonProperty("maxMarks")
  public void setMaxMarks(@Nullable Optional<String> maxMarks) {
    this.maxMarks = maxMarks;
  }

  public CourseSubjectUpdate subjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  
  @JsonProperty("subjectId")
  public @Nullable Optional<UUID> getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable Optional<UUID> subjectId) {
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

