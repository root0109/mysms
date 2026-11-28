// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Marksheet that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class MarksheetUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> visibleToGuardians;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> resultTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> generatedOn;

  public MarksheetUpdate visibleToGuardians(@Nullable Optional<Boolean> visibleToGuardians) {
    this.visibleToGuardians = visibleToGuardians;
    return this;
  }

  /**
   * Whether guardians can see it once published.
   * @return visibleToGuardians
   */
  
  @JsonProperty("visibleToGuardians")
  public @Nullable Optional<Boolean> getVisibleToGuardians() {
    return visibleToGuardians;
  }

  @JsonProperty("visibleToGuardians")
  public void setVisibleToGuardians(@Nullable Optional<Boolean> visibleToGuardians) {
    this.visibleToGuardians = visibleToGuardians;
  }

  public MarksheetUpdate examSessionId(@Nullable Optional<UUID> examSessionId) {
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

  public MarksheetUpdate resultTemplateId(@Nullable Optional<UUID> resultTemplateId) {
    this.resultTemplateId = resultTemplateId;
    return this;
  }

  /**
   * Template used.
   * @return resultTemplateId
   */
  
  @JsonProperty("resultTemplateId")
  public @Nullable Optional<UUID> getResultTemplateId() {
    return resultTemplateId;
  }

  @JsonProperty("resultTemplateId")
  public void setResultTemplateId(@Nullable Optional<UUID> resultTemplateId) {
    this.resultTemplateId = resultTemplateId;
  }

  public MarksheetUpdate name(@Nullable Optional<String> name) {
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

  public MarksheetUpdate generatedOn(@Nullable Optional<LocalDate> generatedOn) {
    this.generatedOn = generatedOn;
    return this;
  }

  /**
   * Generation date.
   * @return generatedOn
   */
  
  @JsonProperty("generatedOn")
  public @Nullable Optional<LocalDate> getGeneratedOn() {
    return generatedOn;
  }

  @JsonProperty("generatedOn")
  public void setGeneratedOn(@Nullable Optional<LocalDate> generatedOn) {
    this.generatedOn = generatedOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarksheetUpdate marksheetUpdate = (MarksheetUpdate) o;
    return Objects.equals(this.visibleToGuardians, marksheetUpdate.visibleToGuardians) &&
        Objects.equals(this.examSessionId, marksheetUpdate.examSessionId) &&
        Objects.equals(this.resultTemplateId, marksheetUpdate.resultTemplateId) &&
        Objects.equals(this.name, marksheetUpdate.name) &&
        Objects.equals(this.generatedOn, marksheetUpdate.generatedOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(visibleToGuardians, examSessionId, resultTemplateId, name, generatedOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarksheetUpdate {\n");
    sb.append("    visibleToGuardians: ").append(toIndentedString(visibleToGuardians)).append("\n");
    sb.append("    examSessionId: ").append(toIndentedString(examSessionId)).append("\n");
    sb.append("    resultTemplateId: ").append(toIndentedString(resultTemplateId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    generatedOn: ").append(toIndentedString(generatedOn)).append("\n");
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

