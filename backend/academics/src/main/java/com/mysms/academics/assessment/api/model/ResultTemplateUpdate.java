// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

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
 * Fields of ResultTemplate that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ResultTemplateUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> gradingSchemeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> resultDate;

  public ResultTemplateUpdate examSessionId(@Nullable Optional<UUID> examSessionId) {
    this.examSessionId = examSessionId;
    return this;
  }

  /**
   * Exam series.
   * @return examSessionId
   */
  
  @JsonProperty("examSessionId")
  public @Nullable Optional<UUID> getExamSessionId() {
    return examSessionId;
  }

  @JsonProperty("examSessionId")
  public void setExamSessionId(@Nullable Optional<UUID> examSessionId) {
    this.examSessionId = examSessionId;
  }

  public ResultTemplateUpdate gradingSchemeId(@Nullable Optional<UUID> gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
    return this;
  }

  /**
   * Grading scheme applied (its bands are used unless specific bands are chosen in result_template_band).
   * @return gradingSchemeId
   */
  
  @JsonProperty("gradingSchemeId")
  public @Nullable Optional<UUID> getGradingSchemeId() {
    return gradingSchemeId;
  }

  @JsonProperty("gradingSchemeId")
  public void setGradingSchemeId(@Nullable Optional<UUID> gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
  }

  public ResultTemplateUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public ResultTemplateUpdate resultDate(@Nullable Optional<LocalDate> resultDate) {
    this.resultDate = resultDate;
    return this;
  }

  /**
   * Result date.
   * @return resultDate
   */
  
  @JsonProperty("resultDate")
  public @Nullable Optional<LocalDate> getResultDate() {
    return resultDate;
  }

  @JsonProperty("resultDate")
  public void setResultDate(@Nullable Optional<LocalDate> resultDate) {
    this.resultDate = resultDate;
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
        Objects.equals(this.resultDate, resultTemplateUpdate.resultDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(examSessionId, gradingSchemeId, name, resultDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultTemplateUpdate {\n");
    sb.append("    examSessionId: ").append(toIndentedString(examSessionId)).append("\n");
    sb.append("    gradingSchemeId: ").append(toIndentedString(gradingSchemeId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    resultDate: ").append(toIndentedString(resultDate)).append("\n");
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

