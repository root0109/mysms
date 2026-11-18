// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * GradebookCell
 */

public class GradebookCell {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID entryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String points;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean isPublished;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isLate;

  public GradebookCell() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradebookCell(UUID contentItemId, String status, Boolean isPublished) {
    this.contentItemId = contentItemId;
    this.status = status;
    this.isPublished = isPublished;
  }

  public GradebookCell contentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item.
   * @return contentItemId
   */
  @NotNull @Valid 
  @JsonProperty("contentItemId")
  public UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public GradebookCell entryId(@Nullable UUID entryId) {
    this.entryId = entryId;
    return this;
  }

  /**
   * Gradebook entry (absent when not graded yet).
   * @return entryId
   */
  @Valid 
  @JsonProperty("entryId")
  public @Nullable UUID getEntryId() {
    return entryId;
  }

  @JsonProperty("entryId")
  public void setEntryId(@Nullable UUID entryId) {
    this.entryId = entryId;
  }

  public GradebookCell points(@Nullable String points) {
    this.points = points;
    return this;
  }

  /**
   * Points.
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

  public GradebookCell status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Grading state.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(String status) {
    this.status = status;
  }

  public GradebookCell isPublished(Boolean isPublished) {
    this.isPublished = isPublished;
    return this;
  }

  /**
   * Visible to the student.
   * @return isPublished
   */
  @NotNull 
  @JsonProperty("isPublished")
  public Boolean getIsPublished() {
    return isPublished;
  }

  @JsonProperty("isPublished")
  public void setIsPublished(Boolean isPublished) {
    this.isPublished = isPublished;
  }

  public GradebookCell isLate(@Nullable Boolean isLate) {
    this.isLate = isLate;
    return this;
  }

  /**
   * Submitted after the due date.
   * @return isLate
   */
  
  @JsonProperty("isLate")
  public @Nullable Boolean getIsLate() {
    return isLate;
  }

  @JsonProperty("isLate")
  public void setIsLate(@Nullable Boolean isLate) {
    this.isLate = isLate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradebookCell gradebookCell = (GradebookCell) o;
    return Objects.equals(this.contentItemId, gradebookCell.contentItemId) &&
        Objects.equals(this.entryId, gradebookCell.entryId) &&
        Objects.equals(this.points, gradebookCell.points) &&
        Objects.equals(this.status, gradebookCell.status) &&
        Objects.equals(this.isPublished, gradebookCell.isPublished) &&
        Objects.equals(this.isLate, gradebookCell.isLate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, entryId, points, status, isPublished, isLate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradebookCell {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    entryId: ").append(toIndentedString(entryId)).append("\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    isPublished: ").append(toIndentedString(isPublished)).append("\n");
    sb.append("    isLate: ").append(toIndentedString(isLate)).append("\n");
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

