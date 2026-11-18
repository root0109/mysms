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
 * Fields of ExamSession that can be updated (partial update; see updateMask).
 */

public class ExamSessionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicTermId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID examTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID batchId;

  /**
   * Marks or grade evaluation.
   */
  public enum EvaluationTypeEnum {
    MARKS("marks"),
    
    GRADE("grade");

    private final String value;

    EvaluationTypeEnum(String value) {
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
    public static EvaluationTypeEnum fromValue(String value) {
      for (EvaluationTypeEnum b : EvaluationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EvaluationTypeEnum evaluationType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String venue;

  public ExamSessionUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public ExamSessionUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public ExamSessionUpdate academicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
    return this;
  }

  /**
   * Term.
   * @return academicTermId
   */
  @Valid 
  @JsonProperty("academicTermId")
  public @Nullable UUID getAcademicTermId() {
    return academicTermId;
  }

  @JsonProperty("academicTermId")
  public void setAcademicTermId(@Nullable UUID academicTermId) {
    this.academicTermId = academicTermId;
  }

  public ExamSessionUpdate examTypeId(@Nullable UUID examTypeId) {
    this.examTypeId = examTypeId;
    return this;
  }

  /**
   * Exam type.
   * @return examTypeId
   */
  @Valid 
  @JsonProperty("examTypeId")
  public @Nullable UUID getExamTypeId() {
    return examTypeId;
  }

  @JsonProperty("examTypeId")
  public void setExamTypeId(@Nullable UUID examTypeId) {
    this.examTypeId = examTypeId;
  }

  public ExamSessionUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Series code.
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

  public ExamSessionUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Series name.
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

  public ExamSessionUpdate gradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
    return this;
  }

  /**
   * Grade (K-12).
   * @return gradeLevelId
   */
  @Valid 
  @JsonProperty("gradeLevelId")
  public @Nullable UUID getGradeLevelId() {
    return gradeLevelId;
  }

  @JsonProperty("gradeLevelId")
  public void setGradeLevelId(@Nullable UUID gradeLevelId) {
    this.gradeLevelId = gradeLevelId;
  }

  public ExamSessionUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course (higher education).
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public ExamSessionUpdate batchId(@Nullable UUID batchId) {
    this.batchId = batchId;
    return this;
  }

  /**
   * Batch (higher education).
   * @return batchId
   */
  @Valid 
  @JsonProperty("batchId")
  public @Nullable UUID getBatchId() {
    return batchId;
  }

  @JsonProperty("batchId")
  public void setBatchId(@Nullable UUID batchId) {
    this.batchId = batchId;
  }

  public ExamSessionUpdate evaluationType(@Nullable EvaluationTypeEnum evaluationType) {
    this.evaluationType = evaluationType;
    return this;
  }

  /**
   * Marks or grade evaluation.
   * @return evaluationType
   */
  
  @JsonProperty("evaluationType")
  public @Nullable EvaluationTypeEnum getEvaluationType() {
    return evaluationType;
  }

  @JsonProperty("evaluationType")
  public void setEvaluationType(@Nullable EvaluationTypeEnum evaluationType) {
    this.evaluationType = evaluationType;
  }

  public ExamSessionUpdate startDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * First exam date.
   * @return startDate
   */
  @Valid 
  @JsonProperty("startDate")
  public @Nullable LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
  }

  public ExamSessionUpdate endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Last exam date.
   * @return endDate
   */
  @Valid 
  @JsonProperty("endDate")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public ExamSessionUpdate venue(@Nullable String venue) {
    this.venue = venue;
    return this;
  }

  /**
   * Venue (when external).
   * @return venue
   */
  @Size(max = 255) 
  @JsonProperty("venue")
  public @Nullable String getVenue() {
    return venue;
  }

  @JsonProperty("venue")
  public void setVenue(@Nullable String venue) {
    this.venue = venue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExamSessionUpdate examSessionUpdate = (ExamSessionUpdate) o;
    return Objects.equals(this.institutionId, examSessionUpdate.institutionId) &&
        Objects.equals(this.academicYearId, examSessionUpdate.academicYearId) &&
        Objects.equals(this.academicTermId, examSessionUpdate.academicTermId) &&
        Objects.equals(this.examTypeId, examSessionUpdate.examTypeId) &&
        Objects.equals(this.code, examSessionUpdate.code) &&
        Objects.equals(this.name, examSessionUpdate.name) &&
        Objects.equals(this.gradeLevelId, examSessionUpdate.gradeLevelId) &&
        Objects.equals(this.courseId, examSessionUpdate.courseId) &&
        Objects.equals(this.batchId, examSessionUpdate.batchId) &&
        Objects.equals(this.evaluationType, examSessionUpdate.evaluationType) &&
        Objects.equals(this.startDate, examSessionUpdate.startDate) &&
        Objects.equals(this.endDate, examSessionUpdate.endDate) &&
        Objects.equals(this.venue, examSessionUpdate.venue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, academicTermId, examTypeId, code, name, gradeLevelId, courseId, batchId, evaluationType, startDate, endDate, venue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExamSessionUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    academicTermId: ").append(toIndentedString(academicTermId)).append("\n");
    sb.append("    examTypeId: ").append(toIndentedString(examTypeId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    gradeLevelId: ").append(toIndentedString(gradeLevelId)).append("\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    batchId: ").append(toIndentedString(batchId)).append("\n");
    sb.append("    evaluationType: ").append(toIndentedString(evaluationType)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    venue: ").append(toIndentedString(venue)).append("\n");
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

