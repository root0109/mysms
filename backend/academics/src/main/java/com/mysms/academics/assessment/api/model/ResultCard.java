// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.academics.assessment.api.model.ExamResult;
import com.mysms.academics.assessment.api.model.MarksheetLine;
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
 * ResultCard
 */

public class ResultCard {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MarksheetLine marksheetLine;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid ExamResult> results = new ArrayList<>();

  public ResultCard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ResultCard(UUID examSessionId) {
    this.examSessionId = examSessionId;
  }

  public ResultCard examSessionId(UUID examSessionId) {
    this.examSessionId = examSessionId;
    return this;
  }

  /**
   * Exam series.
   * @return examSessionId
   */
  @NotNull @Valid 
  @JsonProperty("examSessionId")
  public UUID getExamSessionId() {
    return examSessionId;
  }

  @JsonProperty("examSessionId")
  public void setExamSessionId(UUID examSessionId) {
    this.examSessionId = examSessionId;
  }

  public ResultCard marksheetLine(@Nullable MarksheetLine marksheetLine) {
    this.marksheetLine = marksheetLine;
    return this;
  }

  /**
   * Get marksheetLine
   * @return marksheetLine
   */
  @Valid 
  @JsonProperty("marksheetLine")
  public @Nullable MarksheetLine getMarksheetLine() {
    return marksheetLine;
  }

  @JsonProperty("marksheetLine")
  public void setMarksheetLine(@Nullable MarksheetLine marksheetLine) {
    this.marksheetLine = marksheetLine;
  }

  public ResultCard results(List<@Valid ExamResult> results) {
    this.results = results;
    return this;
  }

  public ResultCard addResultsItem(ExamResult resultsItem) {
    if (this.results == null) {
      this.results = new ArrayList<>();
    }
    this.results.add(resultsItem);
    return this;
  }

  /**
   * Per-paper results.
   * @return results
   */
  
  @JsonProperty("results")
  public List<@Valid ExamResult> getResults() {
    return results;
  }

  @JsonProperty("results")
  public void setResults(List<@Valid ExamResult> results) {
    this.results = results;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultCard resultCard = (ResultCard) o;
    return Objects.equals(this.examSessionId, resultCard.examSessionId) &&
        Objects.equals(this.marksheetLine, resultCard.marksheetLine) &&
        Objects.equals(this.results, resultCard.results);
  }

  @Override
  public int hashCode() {
    return Objects.hash(examSessionId, marksheetLine, results);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultCard {\n");
    sb.append("    examSessionId: ").append(toIndentedString(examSessionId)).append("\n");
    sb.append("    marksheetLine: ").append(toIndentedString(marksheetLine)).append("\n");
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
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

