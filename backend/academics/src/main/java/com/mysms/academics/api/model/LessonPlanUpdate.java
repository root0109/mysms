// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
package com.mysms.academics.api.model;

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
 * Fields of LessonPlan that can be updated (partial update; see updateMask).
 */

public class LessonPlanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID teachingAssignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID curriculumUnitId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String unitNo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String unitName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String plannedDurationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String objective;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String materialsRequired;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String introduction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String procedureText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String conclusion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String assessmentText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String modification;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String notes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  public LessonPlanUpdate teachingAssignmentId(@Nullable UUID teachingAssignmentId) {
    this.teachingAssignmentId = teachingAssignmentId;
    return this;
  }

  /**
   * Teaching assignment it belongs to.
   * @return teachingAssignmentId
   */
  @Valid 
  @JsonProperty("teachingAssignmentId")
  public @Nullable UUID getTeachingAssignmentId() {
    return teachingAssignmentId;
  }

  @JsonProperty("teachingAssignmentId")
  public void setTeachingAssignmentId(@Nullable UUID teachingAssignmentId) {
    this.teachingAssignmentId = teachingAssignmentId;
  }

  public LessonPlanUpdate curriculumUnitId(@Nullable UUID curriculumUnitId) {
    this.curriculumUnitId = curriculumUnitId;
    return this;
  }

  /**
   * Unit covered (when linked to the syllabus).
   * @return curriculumUnitId
   */
  @Valid 
  @JsonProperty("curriculumUnitId")
  public @Nullable UUID getCurriculumUnitId() {
    return curriculumUnitId;
  }

  @JsonProperty("curriculumUnitId")
  public void setCurriculumUnitId(@Nullable UUID curriculumUnitId) {
    this.curriculumUnitId = curriculumUnitId;
  }

  public LessonPlanUpdate unitNo(@Nullable String unitNo) {
    this.unitNo = unitNo;
    return this;
  }

  /**
   * Unit number.
   * @return unitNo
   */
  @Size(max = 20) 
  @JsonProperty("unitNo")
  public @Nullable String getUnitNo() {
    return unitNo;
  }

  @JsonProperty("unitNo")
  public void setUnitNo(@Nullable String unitNo) {
    this.unitNo = unitNo;
  }

  public LessonPlanUpdate unitName(@Nullable String unitName) {
    this.unitName = unitName;
    return this;
  }

  /**
   * Unit name.
   * @return unitName
   */
  @Size(max = 200) 
  @JsonProperty("unitName")
  public @Nullable String getUnitName() {
    return unitName;
  }

  @JsonProperty("unitName")
  public void setUnitName(@Nullable String unitName) {
    this.unitName = unitName;
  }

  public LessonPlanUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Plan title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public LessonPlanUpdate plannedDurationMinutes(@Nullable String plannedDurationMinutes) {
    this.plannedDurationMinutes = plannedDurationMinutes;
    return this;
  }

  /**
   * Planned teaching time.
   * @return plannedDurationMinutes
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?s$") 
  @JsonProperty("plannedDurationMinutes")
  public @Nullable String getPlannedDurationMinutes() {
    return plannedDurationMinutes;
  }

  @JsonProperty("plannedDurationMinutes")
  public void setPlannedDurationMinutes(@Nullable String plannedDurationMinutes) {
    this.plannedDurationMinutes = plannedDurationMinutes;
  }

  public LessonPlanUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Summary.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public LessonPlanUpdate objective(@Nullable String objective) {
    this.objective = objective;
    return this;
  }

  /**
   * Learning objectives.
   * @return objective
   */
  
  @JsonProperty("objective")
  public @Nullable String getObjective() {
    return objective;
  }

  @JsonProperty("objective")
  public void setObjective(@Nullable String objective) {
    this.objective = objective;
  }

  public LessonPlanUpdate materialsRequired(@Nullable String materialsRequired) {
    this.materialsRequired = materialsRequired;
    return this;
  }

  /**
   * Materials.
   * @return materialsRequired
   */
  
  @JsonProperty("materialsRequired")
  public @Nullable String getMaterialsRequired() {
    return materialsRequired;
  }

  @JsonProperty("materialsRequired")
  public void setMaterialsRequired(@Nullable String materialsRequired) {
    this.materialsRequired = materialsRequired;
  }

  public LessonPlanUpdate introduction(@Nullable String introduction) {
    this.introduction = introduction;
    return this;
  }

  /**
   * Introduction.
   * @return introduction
   */
  
  @JsonProperty("introduction")
  public @Nullable String getIntroduction() {
    return introduction;
  }

  @JsonProperty("introduction")
  public void setIntroduction(@Nullable String introduction) {
    this.introduction = introduction;
  }

  public LessonPlanUpdate procedureText(@Nullable String procedureText) {
    this.procedureText = procedureText;
    return this;
  }

  /**
   * Teaching procedure.
   * @return procedureText
   */
  
  @JsonProperty("procedureText")
  public @Nullable String getProcedureText() {
    return procedureText;
  }

  @JsonProperty("procedureText")
  public void setProcedureText(@Nullable String procedureText) {
    this.procedureText = procedureText;
  }

  public LessonPlanUpdate conclusion(@Nullable String conclusion) {
    this.conclusion = conclusion;
    return this;
  }

  /**
   * Conclusion.
   * @return conclusion
   */
  
  @JsonProperty("conclusion")
  public @Nullable String getConclusion() {
    return conclusion;
  }

  @JsonProperty("conclusion")
  public void setConclusion(@Nullable String conclusion) {
    this.conclusion = conclusion;
  }

  public LessonPlanUpdate assessmentText(@Nullable String assessmentText) {
    this.assessmentText = assessmentText;
    return this;
  }

  /**
   * Assessment approach.
   * @return assessmentText
   */
  
  @JsonProperty("assessmentText")
  public @Nullable String getAssessmentText() {
    return assessmentText;
  }

  @JsonProperty("assessmentText")
  public void setAssessmentText(@Nullable String assessmentText) {
    this.assessmentText = assessmentText;
  }

  public LessonPlanUpdate modification(@Nullable String modification) {
    this.modification = modification;
    return this;
  }

  /**
   * Adaptations / differentiation.
   * @return modification
   */
  
  @JsonProperty("modification")
  public @Nullable String getModification() {
    return modification;
  }

  @JsonProperty("modification")
  public void setModification(@Nullable String modification) {
    this.modification = modification;
  }

  public LessonPlanUpdate notes(@Nullable String notes) {
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

  public LessonPlanUpdate startDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Planned start.
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

  public LessonPlanUpdate endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Planned end.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonPlanUpdate lessonPlanUpdate = (LessonPlanUpdate) o;
    return Objects.equals(this.teachingAssignmentId, lessonPlanUpdate.teachingAssignmentId) &&
        Objects.equals(this.curriculumUnitId, lessonPlanUpdate.curriculumUnitId) &&
        Objects.equals(this.unitNo, lessonPlanUpdate.unitNo) &&
        Objects.equals(this.unitName, lessonPlanUpdate.unitName) &&
        Objects.equals(this.title, lessonPlanUpdate.title) &&
        Objects.equals(this.plannedDurationMinutes, lessonPlanUpdate.plannedDurationMinutes) &&
        Objects.equals(this.description, lessonPlanUpdate.description) &&
        Objects.equals(this.objective, lessonPlanUpdate.objective) &&
        Objects.equals(this.materialsRequired, lessonPlanUpdate.materialsRequired) &&
        Objects.equals(this.introduction, lessonPlanUpdate.introduction) &&
        Objects.equals(this.procedureText, lessonPlanUpdate.procedureText) &&
        Objects.equals(this.conclusion, lessonPlanUpdate.conclusion) &&
        Objects.equals(this.assessmentText, lessonPlanUpdate.assessmentText) &&
        Objects.equals(this.modification, lessonPlanUpdate.modification) &&
        Objects.equals(this.notes, lessonPlanUpdate.notes) &&
        Objects.equals(this.startDate, lessonPlanUpdate.startDate) &&
        Objects.equals(this.endDate, lessonPlanUpdate.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(teachingAssignmentId, curriculumUnitId, unitNo, unitName, title, plannedDurationMinutes, description, objective, materialsRequired, introduction, procedureText, conclusion, assessmentText, modification, notes, startDate, endDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonPlanUpdate {\n");
    sb.append("    teachingAssignmentId: ").append(toIndentedString(teachingAssignmentId)).append("\n");
    sb.append("    curriculumUnitId: ").append(toIndentedString(curriculumUnitId)).append("\n");
    sb.append("    unitNo: ").append(toIndentedString(unitNo)).append("\n");
    sb.append("    unitName: ").append(toIndentedString(unitName)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    plannedDurationMinutes: ").append(toIndentedString(plannedDurationMinutes)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    objective: ").append(toIndentedString(objective)).append("\n");
    sb.append("    materialsRequired: ").append(toIndentedString(materialsRequired)).append("\n");
    sb.append("    introduction: ").append(toIndentedString(introduction)).append("\n");
    sb.append("    procedureText: ").append(toIndentedString(procedureText)).append("\n");
    sb.append("    conclusion: ").append(toIndentedString(conclusion)).append("\n");
    sb.append("    assessmentText: ").append(toIndentedString(assessmentText)).append("\n");
    sb.append("    modification: ").append(toIndentedString(modification)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
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

