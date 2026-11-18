// Owned API contract of assessment.
package com.mysms.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ResultTemplate that can be updated (partial update; see updateMask).
 */

public class ResultTemplateUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradingSchemeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate resultDate;

  /**
   * State.
   */
  public enum StatusEnum {
    DRAFT("draft"),
    
    RESULT_GENERATED("result_generated");

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

  public ResultTemplateUpdate examSessionId(@Nullable UUID examSessionId) {
    this.examSessionId = examSessionId;
    return this;
  }

  /**
   * Exam series.
   * @return examSessionId
   */
  @Valid 
  @JsonProperty("examSessionId")
  public @Nullable UUID getExamSessionId() {
    return examSessionId;
  }

  @JsonProperty("examSessionId")
  public void setExamSessionId(@Nullable UUID examSessionId) {
    this.examSessionId = examSessionId;
  }

  public ResultTemplateUpdate gradingSchemeId(@Nullable UUID gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
    return this;
  }

  /**
   * Grading scheme applied (its bands are used unless specific bands are chosen in result_template_band).
   * @return gradingSchemeId
   */
  @Valid 
  @JsonProperty("gradingSchemeId")
  public @Nullable UUID getGradingSchemeId() {
    return gradingSchemeId;
  }

  @JsonProperty("gradingSchemeId")
  public void setGradingSchemeId(@Nullable UUID gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
  }

  public ResultTemplateUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ResultTemplateUpdate resultDate(@Nullable LocalDate resultDate) {
    this.resultDate = resultDate;
    return this;
  }

  /**
   * Result date.
   * @return resultDate
   */
  @Valid 
  @JsonProperty("resultDate")
  public @Nullable LocalDate getResultDate() {
    return resultDate;
  }

  @JsonProperty("resultDate")
  public void setResultDate(@Nullable LocalDate resultDate) {
    this.resultDate = resultDate;
  }

  public ResultTemplateUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultTemplateUpdate resultTemplateUpdate = (ResultTemplateUpdate) o;
    return Objects.equals(this.examSessionId, resultTemplateUpdate.examSessionId) &&
        Objects.equals(this.gradingSchemeId, resultTemplateUpdate.gradingSchemeId) &&
        Objects.equals(this.name, resultTemplateUpdate.name) &&
        Objects.equals(this.resultDate, resultTemplateUpdate.resultDate) &&
        Objects.equals(this.status, resultTemplateUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(examSessionId, gradingSchemeId, name, resultDate, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultTemplateUpdate {\n");
    sb.append("    examSessionId: ").append(toIndentedString(examSessionId)).append("\n");
    sb.append("    gradingSchemeId: ").append(toIndentedString(gradingSchemeId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resultDate: ").append(toIndentedString(resultDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

