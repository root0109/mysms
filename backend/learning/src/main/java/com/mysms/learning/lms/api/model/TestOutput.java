// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * TestOutput
 */

public class TestOutput {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID testCaseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String actualOutput;

  public TestOutput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TestOutput(UUID testCaseId, String actualOutput) {
    this.testCaseId = testCaseId;
    this.actualOutput = actualOutput;
  }

  public TestOutput testCaseId(UUID testCaseId) {
    this.testCaseId = testCaseId;
    return this;
  }

  /**
   * Test case.
   * @return testCaseId
   */
  @NotNull @Valid 
  @JsonProperty("testCaseId")
  public UUID getTestCaseId() {
    return testCaseId;
  }

  @JsonProperty("testCaseId")
  public void setTestCaseId(UUID testCaseId) {
    this.testCaseId = testCaseId;
  }

  public TestOutput actualOutput(String actualOutput) {
    this.actualOutput = actualOutput;
    return this;
  }

  /**
   * Output produced by the sandbox.
   * @return actualOutput
   */
  @NotNull 
  @JsonProperty("actualOutput")
  public String getActualOutput() {
    return actualOutput;
  }

  @JsonProperty("actualOutput")
  public void setActualOutput(String actualOutput) {
    this.actualOutput = actualOutput;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestOutput testOutput = (TestOutput) o;
    return Objects.equals(this.testCaseId, testOutput.testCaseId) &&
        Objects.equals(this.actualOutput, testOutput.actualOutput);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testCaseId, actualOutput);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TestOutput {\n");
    sb.append("    testCaseId: ").append(toIndentedString(testCaseId)).append("\n");
    sb.append("    actualOutput: ").append(toIndentedString(actualOutput)).append("\n");
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

