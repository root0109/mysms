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
 * Fields of LearningProgram that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LearningProgramUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPublished;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> enforceCourseOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> courseCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> memberCount;

  public LearningProgramUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Program title.
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

  public LearningProgramUpdate isPublished(@Nullable Optional<Boolean> isPublished) {
    this.isPublished = isPublished;
    return this;
  }

  /**
   * Visible.
   * @return isPublished
   */
  
  @JsonProperty("isPublished")
  public @Nullable Optional<Boolean> getIsPublished() {
    return isPublished;
  }

  @JsonProperty("isPublished")
  public void setIsPublished(@Nullable Optional<Boolean> isPublished) {
    this.isPublished = isPublished;
  }

  public LearningProgramUpdate enforceCourseOrder(@Nullable Optional<Boolean> enforceCourseOrder) {
    this.enforceCourseOrder = enforceCourseOrder;
    return this;
  }

  /**
   * Courses must be completed in order.
   * @return enforceCourseOrder
   */
  
  @JsonProperty("enforceCourseOrder")
  public @Nullable Optional<Boolean> getEnforceCourseOrder() {
    return enforceCourseOrder;
  }

  @JsonProperty("enforceCourseOrder")
  public void setEnforceCourseOrder(@Nullable Optional<Boolean> enforceCourseOrder) {
    this.enforceCourseOrder = enforceCourseOrder;
  }

  public LearningProgramUpdate courseCount(@Nullable Optional<Integer> courseCount) {
    this.courseCount = courseCount;
    return this;
  }

  /**
   * Cached number of courses.
   * @return courseCount
   */
  
  @JsonProperty("courseCount")
  public @Nullable Optional<Integer> getCourseCount() {
    return courseCount;
  }

  @JsonProperty("courseCount")
  public void setCourseCount(@Nullable Optional<Integer> courseCount) {
    this.courseCount = courseCount;
  }

  public LearningProgramUpdate memberCount(@Nullable Optional<Integer> memberCount) {
    this.memberCount = memberCount;
    return this;
  }

  /**
   * Cached number of members.
   * @return memberCount
   */
  
  @JsonProperty("memberCount")
  public @Nullable Optional<Integer> getMemberCount() {
    return memberCount;
  }

  @JsonProperty("memberCount")
  public void setMemberCount(@Nullable Optional<Integer> memberCount) {
    this.memberCount = memberCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LearningProgramUpdate learningProgramUpdate = (LearningProgramUpdate) o;
    return Objects.equals(this.title, learningProgramUpdate.title) &&
        Objects.equals(this.isPublished, learningProgramUpdate.isPublished) &&
        Objects.equals(this.enforceCourseOrder, learningProgramUpdate.enforceCourseOrder) &&
        Objects.equals(this.courseCount, learningProgramUpdate.courseCount) &&
        Objects.equals(this.memberCount, learningProgramUpdate.memberCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, isPublished, enforceCourseOrder, courseCount, memberCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LearningProgramUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    isPublished: ").append(toIndentedString(isPublished)).append("\n");
    sb.append("    enforceCourseOrder: ").append(toIndentedString(enforceCourseOrder)).append("\n");
    sb.append("    courseCount: ").append(toIndentedString(courseCount)).append("\n");
    sb.append("    memberCount: ").append(toIndentedString(memberCount)).append("\n");
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

