// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.GradebookCell;
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
 * GradebookRow
 */

public class GradebookRow {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String studentName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid GradebookCell> cells = new ArrayList<>();

  private @Nullable Object categoryPercents = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String totalPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String letterGrade;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String finalGrade;

  public GradebookRow() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GradebookRow(UUID userId, String studentName) {
    this.userId = userId;
    this.studentName = studentName;
  }

  public GradebookRow userId(UUID userId) {
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

  public GradebookRow studentName(String studentName) {
    this.studentName = studentName;
    return this;
  }

  /**
   * Student name.
   * @return studentName
   */
  @NotNull 
  @JsonProperty("studentName")
  public String getStudentName() {
    return studentName;
  }

  @JsonProperty("studentName")
  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }

  public GradebookRow cells(List<@Valid GradebookCell> cells) {
    this.cells = cells;
    return this;
  }

  public GradebookRow addCellsItem(GradebookCell cellsItem) {
    if (this.cells == null) {
      this.cells = new ArrayList<>();
    }
    this.cells.add(cellsItem);
    return this;
  }

  /**
   * One cell per column.
   * @return cells
   */
  
  @JsonProperty("cells")
  public List<@Valid GradebookCell> getCells() {
    return cells;
  }

  @JsonProperty("cells")
  public void setCells(List<@Valid GradebookCell> cells) {
    this.cells = cells;
  }

  public GradebookRow categoryPercents(@Nullable Object categoryPercents) {
    this.categoryPercents = categoryPercents;
    return this;
  }

  /**
   * Percentage per grading category.
   * @return categoryPercents
   */
  
  @JsonProperty("categoryPercents")
  public @Nullable Object getCategoryPercents() {
    return categoryPercents;
  }

  @JsonProperty("categoryPercents")
  public void setCategoryPercents(@Nullable Object categoryPercents) {
    this.categoryPercents = categoryPercents;
  }

  public GradebookRow totalPercent(@Nullable String totalPercent) {
    this.totalPercent = totalPercent;
    return this;
  }

  /**
   * Weighted total.
   * @return totalPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalPercent")
  public @Nullable String getTotalPercent() {
    return totalPercent;
  }

  @JsonProperty("totalPercent")
  public void setTotalPercent(@Nullable String totalPercent) {
    this.totalPercent = totalPercent;
  }

  public GradebookRow letterGrade(@Nullable String letterGrade) {
    this.letterGrade = letterGrade;
    return this;
  }

  /**
   * Letter grade from the scale.
   * @return letterGrade
   */
  
  @JsonProperty("letterGrade")
  public @Nullable String getLetterGrade() {
    return letterGrade;
  }

  @JsonProperty("letterGrade")
  public void setLetterGrade(@Nullable String letterGrade) {
    this.letterGrade = letterGrade;
  }

  public GradebookRow finalGrade(@Nullable String finalGrade) {
    this.finalGrade = finalGrade;
    return this;
  }

  /**
   * Final grade awarded (cohort enrolment).
   * @return finalGrade
   */
  
  @JsonProperty("finalGrade")
  public @Nullable String getFinalGrade() {
    return finalGrade;
  }

  @JsonProperty("finalGrade")
  public void setFinalGrade(@Nullable String finalGrade) {
    this.finalGrade = finalGrade;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradebookRow gradebookRow = (GradebookRow) o;
    return Objects.equals(this.userId, gradebookRow.userId) &&
        Objects.equals(this.studentName, gradebookRow.studentName) &&
        Objects.equals(this.cells, gradebookRow.cells) &&
        Objects.equals(this.categoryPercents, gradebookRow.categoryPercents) &&
        Objects.equals(this.totalPercent, gradebookRow.totalPercent) &&
        Objects.equals(this.letterGrade, gradebookRow.letterGrade) &&
        Objects.equals(this.finalGrade, gradebookRow.finalGrade);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, studentName, cells, categoryPercents, totalPercent, letterGrade, finalGrade);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradebookRow {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    studentName: ").append(toIndentedString(studentName)).append("\n");
    sb.append("    cells: ").append(toIndentedString(cells)).append("\n");
    sb.append("    categoryPercents: ").append(toIndentedString(categoryPercents)).append("\n");
    sb.append("    totalPercent: ").append(toIndentedString(totalPercent)).append("\n");
    sb.append("    letterGrade: ").append(toIndentedString(letterGrade)).append("\n");
    sb.append("    finalGrade: ").append(toIndentedString(finalGrade)).append("\n");
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

