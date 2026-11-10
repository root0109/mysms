// Owned API contract of assessment (JAVA-CODING-RULES §7); first materialized from api/openapi/assessment.yaml on 2026-11-10.
package com.mysms.assessment.api.model;

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
 * Fields of Exam that can be updated (partial update; see updateMask).
 */

public class ExamUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String totalMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String passMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean resultsEntered;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String notes;

  public ExamUpdate examSessionId(@Nullable UUID examSessionId) {
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

  public ExamUpdate subjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  @Valid 
  @JsonProperty("subjectId")
  public @Nullable UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
  }

  public ExamUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section sitting the paper (K-12).
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public ExamUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Paper code.
   * @return code
   */
  @Size(max = 30) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public ExamUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Paper name.
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

  public ExamUpdate startAt(@Nullable OffsetDateTime startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Start.
   * @return startAt
   */
  @Valid 
  @JsonProperty("startAt")
  public @Nullable OffsetDateTime getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(@Nullable OffsetDateTime startAt) {
    this.startAt = startAt;
  }

  public ExamUpdate endAt(@Nullable OffsetDateTime endAt) {
    this.endAt = endAt;
    return this;
  }

  /**
   * End.
   * @return endAt
   */
  @Valid 
  @JsonProperty("endAt")
  public @Nullable OffsetDateTime getEndAt() {
    return endAt;
  }

  @JsonProperty("endAt")
  public void setEndAt(@Nullable OffsetDateTime endAt) {
    this.endAt = endAt;
  }

  public ExamUpdate totalMarks(@Nullable String totalMarks) {
    this.totalMarks = totalMarks;
    return this;
  }

  /**
   * Maximum marks.
   * @return totalMarks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("totalMarks")
  public @Nullable String getTotalMarks() {
    return totalMarks;
  }

  @JsonProperty("totalMarks")
  public void setTotalMarks(@Nullable String totalMarks) {
    this.totalMarks = totalMarks;
  }

  public ExamUpdate passMarks(@Nullable String passMarks) {
    this.passMarks = passMarks;
    return this;
  }

  /**
   * Minimum marks to pass.
   * @return passMarks
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("passMarks")
  public @Nullable String getPassMarks() {
    return passMarks;
  }

  @JsonProperty("passMarks")
  public void setPassMarks(@Nullable String passMarks) {
    this.passMarks = passMarks;
  }

  public ExamUpdate resultsEntered(@Nullable Boolean resultsEntered) {
    this.resultsEntered = resultsEntered;
    return this;
  }

  /**
   * All marks have been entered.
   * @return resultsEntered
   */
  
  @JsonProperty("resultsEntered")
  public @Nullable Boolean getResultsEntered() {
    return resultsEntered;
  }

  @JsonProperty("resultsEntered")
  public void setResultsEntered(@Nullable Boolean resultsEntered) {
    this.resultsEntered = resultsEntered;
  }

  public ExamUpdate notes(@Nullable String notes) {
    this.notes = notes;
    return this;
  }

  /**
   * Notes.
   * @return notes
   */
  
  @JsonProperty("notes")
  public @Nullable String getNotes() {
    return notes;
  }

  @JsonProperty("notes")
  public void setNotes(@Nullable String notes) {
    this.notes = notes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExamUpdate examUpdate = (ExamUpdate) o;
    return Objects.equals(this.examSessionId, examUpdate.examSessionId) &&
        Objects.equals(this.subjectId, examUpdate.subjectId) &&
        Objects.equals(this.classSectionId, examUpdate.classSectionId) &&
        Objects.equals(this.code, examUpdate.code) &&
        Objects.equals(this.name, examUpdate.name) &&
        Objects.equals(this.startAt, examUpdate.startAt) &&
        Objects.equals(this.endAt, examUpdate.endAt) &&
        Objects.equals(this.totalMarks, examUpdate.totalMarks) &&
        Objects.equals(this.passMarks, examUpdate.passMarks) &&
        Objects.equals(this.resultsEntered, examUpdate.resultsEntered) &&
        Objects.equals(this.notes, examUpdate.notes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(examSessionId, subjectId, classSectionId, code, name, startAt, endAt, totalMarks, passMarks, resultsEntered, notes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExamUpdate {\n");
    sb.append("    examSessionId: ").append(toIndentedString(examSessionId)).append("\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    startAt: ").append(toIndentedString(startAt)).append("\n");
    sb.append("    endAt: ").append(toIndentedString(endAt)).append("\n");
    sb.append("    totalMarks: ").append(toIndentedString(totalMarks)).append("\n");
    sb.append("    passMarks: ").append(toIndentedString(passMarks)).append("\n");
    sb.append("    resultsEntered: ").append(toIndentedString(resultsEntered)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
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

