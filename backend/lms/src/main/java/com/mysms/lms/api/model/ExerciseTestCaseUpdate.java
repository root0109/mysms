// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of ExerciseTestCase that can be updated (partial update; see updateMask).
 */

public class ExerciseTestCaseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String input;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String expectedOutput;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isHidden;

  public ExerciseTestCaseUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public ExerciseTestCaseUpdate input(@Nullable String input) {
    this.input = input;
    return this;
  }

  /**
   * Input.
   * @return input
   */
  
  @JsonProperty("input")
  public @Nullable String getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(@Nullable String input) {
    this.input = input;
  }

  public ExerciseTestCaseUpdate expectedOutput(@Nullable String expectedOutput) {
    this.expectedOutput = expectedOutput;
    return this;
  }

  /**
   * Expected output.
   * @return expectedOutput
   */
  
  @JsonProperty("expectedOutput")
  public @Nullable String getExpectedOutput() {
    return expectedOutput;
  }

  @JsonProperty("expectedOutput")
  public void setExpectedOutput(@Nullable String expectedOutput) {
    this.expectedOutput = expectedOutput;
  }

  public ExerciseTestCaseUpdate isHidden(@Nullable Boolean isHidden) {
    this.isHidden = isHidden;
    return this;
  }

  /**
   * Hidden from students.
   * @return isHidden
   */
  
  @JsonProperty("isHidden")
  public @Nullable Boolean getIsHidden() {
    return isHidden;
  }

  @JsonProperty("isHidden")
  public void setIsHidden(@Nullable Boolean isHidden) {
    this.isHidden = isHidden;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExerciseTestCaseUpdate exerciseTestCaseUpdate = (ExerciseTestCaseUpdate) o;
    return Objects.equals(this.sortOrder, exerciseTestCaseUpdate.sortOrder) &&
        Objects.equals(this.input, exerciseTestCaseUpdate.input) &&
        Objects.equals(this.expectedOutput, exerciseTestCaseUpdate.expectedOutput) &&
        Objects.equals(this.isHidden, exerciseTestCaseUpdate.isHidden);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortOrder, input, expectedOutput, isHidden);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExerciseTestCaseUpdate {\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    expectedOutput: ").append(toIndentedString(expectedOutput)).append("\n");
    sb.append("    isHidden: ").append(toIndentedString(isHidden)).append("\n");
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

