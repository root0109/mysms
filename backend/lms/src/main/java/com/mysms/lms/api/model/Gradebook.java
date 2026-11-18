// Owned API contract of lms.
package com.mysms.lms.api.model;

import com.mysms.core.paging.NavigationInfo;
import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.GradebookColumn;
import com.mysms.lms.api.model.GradebookRow;
import com.mysms.lms.api.model.GradingCategory;
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
 * Gradebook
 */

public class Gradebook {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid GradebookColumn> columns = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid GradingCategory> categories = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid GradebookRow> rows = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String gradeDisplay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable NavigationInfo paging;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Gradebook() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Gradebook(UUID cohortId, List<@Valid GradebookColumn> columns, List<@Valid GradebookRow> rows, String gradeDisplay) {
    this.cohortId = cohortId;
    this.columns = columns;
    this.rows = rows;
    this.gradeDisplay = gradeDisplay;
  }

  public Gradebook cohortId(UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @NotNull @Valid 
  @JsonProperty("cohortId")
  public UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(UUID cohortId) {
    this.cohortId = cohortId;
  }

  public Gradebook columns(List<@Valid GradebookColumn> columns) {
    this.columns = columns;
    return this;
  }

  public Gradebook addColumnsItem(GradebookColumn columnsItem) {
    if (this.columns == null) {
      this.columns = new ArrayList<>();
    }
    this.columns.add(columnsItem);
    return this;
  }

  /**
   * Graded items.
   * @return columns
   */
  @NotNull 
  @JsonProperty("columns")
  public List<@Valid GradebookColumn> getColumns() {
    return columns;
  }

  @JsonProperty("columns")
  public void setColumns(List<@Valid GradebookColumn> columns) {
    this.columns = columns;
  }

  public Gradebook categories(List<@Valid GradingCategory> categories) {
    this.categories = categories;
    return this;
  }

  public Gradebook addCategoriesItem(GradingCategory categoriesItem) {
    if (this.categories == null) {
      this.categories = new ArrayList<>();
    }
    this.categories.add(categoriesItem);
    return this;
  }

  /**
   * Grading categories.
   * @return categories
   */
  
  @JsonProperty("categories")
  public List<@Valid GradingCategory> getCategories() {
    return categories;
  }

  @JsonProperty("categories")
  public void setCategories(List<@Valid GradingCategory> categories) {
    this.categories = categories;
  }

  public Gradebook rows(List<@Valid GradebookRow> rows) {
    this.rows = rows;
    return this;
  }

  public Gradebook addRowsItem(GradebookRow rowsItem) {
    if (this.rows == null) {
      this.rows = new ArrayList<>();
    }
    this.rows.add(rowsItem);
    return this;
  }

  /**
   * Students of this page (one row when a student views their own grades).
   * @return rows
   */
  @NotNull 
  @JsonProperty("rows")
  public List<@Valid GradebookRow> getRows() {
    return rows;
  }

  @JsonProperty("rows")
  public void setRows(List<@Valid GradebookRow> rows) {
    this.rows = rows;
  }

  public Gradebook gradeDisplay(String gradeDisplay) {
    this.gradeDisplay = gradeDisplay;
    return this;
  }

  /**
   * points or percentage.
   * @return gradeDisplay
   */
  @NotNull 
  @JsonProperty("gradeDisplay")
  public String getGradeDisplay() {
    return gradeDisplay;
  }

  @JsonProperty("gradeDisplay")
  public void setGradeDisplay(String gradeDisplay) {
    this.gradeDisplay = gradeDisplay;
  }

  public Gradebook paging(@Nullable NavigationInfo paging) {
    this.paging = paging;
    return this;
  }

  /**
   * Navigation over the pages of students (the rows); next is absent on the last page.
   * @return paging
   */
  
  @JsonProperty("paging")
  public @Nullable NavigationInfo getPaging() {
    return paging;
  }

  @JsonProperty("paging")
  public void setPaging(@Nullable NavigationInfo paging) {
    this.paging = paging;
  }

  public Gradebook etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Version of the whole gradebook; send in If-None-Match to get 304 when nothing changed.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Gradebook gradebook = (Gradebook) o;
    return Objects.equals(this.cohortId, gradebook.cohortId) &&
        Objects.equals(this.columns, gradebook.columns) &&
        Objects.equals(this.categories, gradebook.categories) &&
        Objects.equals(this.rows, gradebook.rows) &&
        Objects.equals(this.gradeDisplay, gradebook.gradeDisplay) &&
        Objects.equals(this.paging, gradebook.paging) &&
        Objects.equals(this.etag, gradebook.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, columns, categories, rows, gradeDisplay, paging, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Gradebook {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    gradeDisplay: ").append(toIndentedString(gradeDisplay)).append("\n");
    sb.append("    paging: ").append(toIndentedString(paging)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

