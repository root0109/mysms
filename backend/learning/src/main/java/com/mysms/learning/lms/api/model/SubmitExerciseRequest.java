// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.learning.lms.api.model.TestOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SubmitExerciseRequest
 */

public class SubmitExerciseRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isFullCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid TestOutput> testOutputs = new ArrayList<>();

  public SubmitExerciseRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SubmitExerciseRequest(String code, List<@Valid TestOutput> testOutputs) {
    this.code = code;
    this.testOutputs = testOutputs;
  }

  public SubmitExerciseRequest code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Code.
   * @return code
   */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(String code) {
    this.code = code;
  }

  public SubmitExerciseRequest isFullCode(@Nullable Boolean isFullCode) {
    this.isFullCode = isFullCode;
    return this;
  }

  /**
   * Full program.
   * @return isFullCode
   */
  
  @JsonProperty("isFullCode")
  public @Nullable Boolean getIsFullCode() {
    return isFullCode;
  }

  @JsonProperty("isFullCode")
  public void setIsFullCode(@Nullable Boolean isFullCode) {
    this.isFullCode = isFullCode;
  }

  public SubmitExerciseRequest testOutputs(List<@Valid TestOutput> testOutputs) {
    this.testOutputs = testOutputs;
    return this;
  }

  public SubmitExerciseRequest addTestOutputsItem(TestOutput testOutputsItem) {
    if (this.testOutputs == null) {
      this.testOutputs = new ArrayList<>();
    }
    this.testOutputs.add(testOutputsItem);
    return this;
  }

  /**
   * Outputs from the sandbox run.
   * @return testOutputs
   */
  @NotNull 
  @JsonProperty("testOutputs")
  public List<@Valid TestOutput> getTestOutputs() {
    return testOutputs;
  }

  @JsonProperty("testOutputs")
  public void setTestOutputs(List<@Valid TestOutput> testOutputs) {
    this.testOutputs = testOutputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubmitExerciseRequest submitExerciseRequest = (SubmitExerciseRequest) o;
    return Objects.equals(this.code, submitExerciseRequest.code) &&
        Objects.equals(this.isFullCode, submitExerciseRequest.isFullCode) &&
        Objects.equals(this.testOutputs, submitExerciseRequest.testOutputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, isFullCode, testOutputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubmitExerciseRequest {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    isFullCode: ").append(toIndentedString(isFullCode)).append("\n");
    sb.append("    testOutputs: ").append(toIndentedString(testOutputs)).append("\n");
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

