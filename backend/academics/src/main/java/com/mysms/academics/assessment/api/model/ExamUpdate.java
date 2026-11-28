// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

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
 * Fields of Exam that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ExamUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> subjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> startAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> endAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> totalMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> passMarks;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> notes;

  public ExamUpdate examSessionId(@Nullable Optional<UUID> examSessionId) {
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

  public ExamUpdate subjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  
  @JsonProperty("subjectId")
  public @Nullable Optional<UUID> getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
  }

  public ExamUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section sitting the paper (K-12).
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public ExamUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Paper code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public ExamUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Paper name.
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

  public ExamUpdate startAt(@Nullable Optional<OffsetDateTime> startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Start.
   * @return startAt
   */
  
  @JsonProperty("startAt")
  public @Nullable Optional<OffsetDateTime> getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(@Nullable Optional<OffsetDateTime> startAt) {
    this.startAt = startAt;
  }

  public ExamUpdate endAt(@Nullable Optional<OffsetDateTime> endAt) {
    this.endAt = endAt;
    return this;
  }

  /**
   * End.
   * @return endAt
   */
  
  @JsonProperty("endAt")
  public @Nullable Optional<OffsetDateTime> getEndAt() {
    return endAt;
  }

  @JsonProperty("endAt")
  public void setEndAt(@Nullable Optional<OffsetDateTime> endAt) {
    this.endAt = endAt;
  }

  public ExamUpdate totalMarks(@Nullable Optional<String> totalMarks) {
    this.totalMarks = totalMarks;
    return this;
  }

  /**
   * Maximum marks.
   * @return totalMarks
   */
  
  @JsonProperty("totalMarks")
  public @Nullable Optional<String> getTotalMarks() {
    return totalMarks;
  }

  @JsonProperty("totalMarks")
  public void setTotalMarks(@Nullable Optional<String> totalMarks) {
    this.totalMarks = totalMarks;
  }

  public ExamUpdate passMarks(@Nullable Optional<String> passMarks) {
    this.passMarks = passMarks;
    return this;
  }

  /**
   * Minimum marks to pass.
   * @return passMarks
   */
  
  @JsonProperty("passMarks")
  public @Nullable Optional<String> getPassMarks() {
    return passMarks;
  }

  @JsonProperty("passMarks")
  public void setPassMarks(@Nullable Optional<String> passMarks) {
    this.passMarks = passMarks;
  }

  public ExamUpdate notes(@Nullable Optional<String> notes) {
    this.notes = notes;
    return this;
  }

  /**
   * Notes.
   * @return notes
   */
  
  @JsonProperty("notes")
  public @Nullable Optional<String> getNotes() {
    return notes;
  }

  @JsonProperty("notes")
  public void setNotes(@Nullable Optional<String> notes) {
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
        Objects.equals(this.notes, examUpdate.notes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(examSessionId, subjectId, classSectionId, code, name, startAt, endAt, totalMarks, passMarks, notes);
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

