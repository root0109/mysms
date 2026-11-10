// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * GradeInput
 */

public class GradeInput {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String points;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String feedback;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String teacherComment;

  /**
   * Grading state (default graded).
   */
  public enum StatusEnum {
    GRADED("graded"),
    
    EXCUSED("excused"),
    
    NOT_SUBMITTED("not_submitted");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> rubricCellIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean publish;

  public GradeInput points(@Nullable String points) {
    this.points = points;
    return this;
  }

  /**
   * Points awarded (omit to leave unchanged).
   * @return points
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("points")
  public @Nullable String getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(@Nullable String points) {
    this.points = points;
  }

  public GradeInput feedback(@Nullable String feedback) {
    this.feedback = feedback;
    return this;
  }

  /**
   * Feedback to the student.
   * @return feedback
   */
  
  @JsonProperty("feedback")
  public @Nullable String getFeedback() {
    return feedback;
  }

  @JsonProperty("feedback")
  public void setFeedback(@Nullable String feedback) {
    this.feedback = feedback;
  }

  public GradeInput teacherComment(@Nullable String teacherComment) {
    this.teacherComment = teacherComment;
    return this;
  }

  /**
   * Private note.
   * @return teacherComment
   */
  
  @JsonProperty("teacherComment")
  public @Nullable String getTeacherComment() {
    return teacherComment;
  }

  @JsonProperty("teacherComment")
  public void setTeacherComment(@Nullable String teacherComment) {
    this.teacherComment = teacherComment;
  }

  public GradeInput status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Grading state (default graded).
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public GradeInput rubricCellIds(List<UUID> rubricCellIds) {
    this.rubricCellIds = rubricCellIds;
    return this;
  }

  public GradeInput addRubricCellIdsItem(UUID rubricCellIdsItem) {
    if (this.rubricCellIds == null) {
      this.rubricCellIds = new ArrayList<>();
    }
    this.rubricCellIds.add(rubricCellIdsItem);
    return this;
  }

  /**
   * Chosen rubric cells (one per criterion).
   * @return rubricCellIds
   */
  
  @JsonProperty("rubricCellIds")
  public List<UUID> getRubricCellIds() {
    return rubricCellIds;
  }

  @JsonProperty("rubricCellIds")
  public void setRubricCellIds(List<UUID> rubricCellIds) {
    this.rubricCellIds = rubricCellIds;
  }

  public GradeInput publish(@Nullable Boolean publish) {
    this.publish = publish;
    return this;
  }

  /**
   * Publish to the student immediately.
   * @return publish
   */
  
  @JsonProperty("publish")
  public @Nullable Boolean getPublish() {
    return publish;
  }

  @JsonProperty("publish")
  public void setPublish(@Nullable Boolean publish) {
    this.publish = publish;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradeInput gradeInput = (GradeInput) o;
    return Objects.equals(this.points, gradeInput.points) &&
        Objects.equals(this.feedback, gradeInput.feedback) &&
        Objects.equals(this.teacherComment, gradeInput.teacherComment) &&
        Objects.equals(this.status, gradeInput.status) &&
        Objects.equals(this.rubricCellIds, gradeInput.rubricCellIds) &&
        Objects.equals(this.publish, gradeInput.publish);
  }

  @Override
  public int hashCode() {
    return Objects.hash(points, feedback, teacherComment, status, rubricCellIds, publish);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradeInput {\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
    sb.append("    feedback: ").append(toIndentedString(feedback)).append("\n");
    sb.append("    teacherComment: ").append(toIndentedString(teacherComment)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    rubricCellIds: ").append(toIndentedString(rubricCellIds)).append("\n");
    sb.append("    publish: ").append(toIndentedString(publish)).append("\n");
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

