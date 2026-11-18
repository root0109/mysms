// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * GradebookColumn
 */

public class GradebookColumn {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String itemKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradingCategoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String points;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String weightPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  public GradebookColumn() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradebookColumn(UUID contentItemId, String title, String itemKind) {
    this.contentItemId = contentItemId;
    this.title = title;
    this.itemKind = itemKind;
  }

  public GradebookColumn contentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Graded item.
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

  public GradebookColumn title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Item title.
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public GradebookColumn itemKind(String itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Item kind.
   * @return itemKind
   */
  @NotNull 
  @JsonProperty("itemKind")
  public String getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(String itemKind) {
    this.itemKind = itemKind;
  }

  public GradebookColumn gradingCategoryId(@Nullable UUID gradingCategoryId) {
    this.gradingCategoryId = gradingCategoryId;
    return this;
  }

  /**
   * Category.
   * @return gradingCategoryId
   */
  @Valid 
  @JsonProperty("gradingCategoryId")
  public @Nullable UUID getGradingCategoryId() {
    return gradingCategoryId;
  }

  @JsonProperty("gradingCategoryId")
  public void setGradingCategoryId(@Nullable UUID gradingCategoryId) {
    this.gradingCategoryId = gradingCategoryId;
  }

  public GradebookColumn points(@Nullable String points) {
    this.points = points;
    return this;
  }

  /**
   * Maximum points.
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

  public GradebookColumn weightPercent(@Nullable String weightPercent) {
    this.weightPercent = weightPercent;
    return this;
  }

  /**
   * Weight in its category.
   * @return weightPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("weightPercent")
  public @Nullable String getWeightPercent() {
    return weightPercent;
  }

  @JsonProperty("weightPercent")
  public void setWeightPercent(@Nullable String weightPercent) {
    this.weightPercent = weightPercent;
  }

  public GradebookColumn dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * Due date.
   * @return dueAt
   */
  @Valid 
  @JsonProperty("dueAt")
  public @Nullable OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradebookColumn gradebookColumn = (GradebookColumn) o;
    return Objects.equals(this.contentItemId, gradebookColumn.contentItemId) &&
        Objects.equals(this.title, gradebookColumn.title) &&
        Objects.equals(this.itemKind, gradebookColumn.itemKind) &&
        Objects.equals(this.gradingCategoryId, gradebookColumn.gradingCategoryId) &&
        Objects.equals(this.points, gradebookColumn.points) &&
        Objects.equals(this.weightPercent, gradebookColumn.weightPercent) &&
        Objects.equals(this.dueAt, gradebookColumn.dueAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, title, itemKind, gradingCategoryId, points, weightPercent, dueAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradebookColumn {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    itemKind: ").append(toIndentedString(itemKind)).append("\n");
    sb.append("    gradingCategoryId: ").append(toIndentedString(gradingCategoryId)).append("\n");
    sb.append("    points: ").append(toIndentedString(points)).append("\n");
    sb.append("    weightPercent: ").append(toIndentedString(weightPercent)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
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

